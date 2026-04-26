/*
 * Copyright (c) 2026-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.console.scheduler.manager;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.console.scheduler.bridge.redis.SchedulerMessageBridgeHandler;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessageProtocol;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.schedule.ScheduleRuleRegistry;
import com.aspectran.core.context.rule.ScheduleRule;
import com.aspectran.core.context.rule.ScheduledJobRule;
import com.aspectran.core.context.rule.converter.RulesToParameters;
import com.aspectran.core.context.rule.params.ScheduleParameters;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * SchedulerManager manages schedulers across the cluster.
 * It sends control commands to nodes and handles results via Redis relay.
 */
@Component
@Bean(id = "schedulerManager")
public class SchedulerManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

    private static final String COMMAND_LIST = "scheduler:list";
    private static final String COMMAND_ENABLE = "scheduler:enable";
    private static final String COMMAND_DISABLE = "scheduler:disable";

    private final NodeManager nodeManager;

    private final SchedulerBroker broker;

    public SchedulerManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.broker = new SchedulerBroker(nodeManager.getNodeId(), nodeManager.getRedisMessagePublisher());
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing SchedulerManager for node: {}", nodeManager.getNodeId());

        // Register a listener for scheduler relay messages (commands and results) from Redis
        if (nodeManager.getRedisMessageSubscriber() != null) {
            SchedulerMessageBridgeHandler bridgeHandler = new SchedulerMessageBridgeHandler(this);
            nodeManager.getRedisMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public SchedulerBroker getBroker() {
        return broker;
    }

    /**
     * Sends a scheduler management command to the cluster or executes locally.
     * @param targetNodeId the ID of the node to execute the command
     * @param command the command string
     */
    public void sendCommand(String targetNodeId, String command) {
        if (nodeManager.getNodeId().equals(targetNodeId)) {
            // Case 1: Target is local node, execute directly and bridge result to local clients
            logger.debug("Executing local scheduler command: {}", command);
            String result = executeLocalCommand(command);
            if (result != null) {
                handleSchedulerResult(result);
            }
        } else {
            // Case 2: Target is a remote node, relay via Redis
            if (nodeManager.getRedisMessagePublisher() != null) {
                try {
                    String message = "command:" + command + ";" + targetNodeId;
                    nodeManager.getRedisMessagePublisher().publishRelay(NodeMessageProtocol.CATEGORY_SCHEDULER, message);
                    logger.debug("Scheduler command relayed to node {}: {}", targetNodeId, command);
                } catch (Exception e) {
                    logger.error("Failed to relay scheduler command to Redis", e);
                }
            } else {
                logger.warn("Cannot relay command to node {}: Redis publisher not available", targetNodeId);
            }
        }
    }

    /**
     * Processes a scheduler management command received from Redis.
     * If the command is for this node, executes it and publishes the result back to Redis.
     * @param message the full command message string
     */
    public void processCommand(String message) {
        if (StringUtils.isEmpty(message) || !message.startsWith("command:")) {
            return;
        }

        String full = message.substring(8);
        int idx = full.indexOf(';');
        String command;
        String targetNodeId = null;

        if (idx != -1) {
            command = full.substring(0, idx);
            targetNodeId = full.substring(idx + 1);
        } else {
            command = full;
        }

        if (targetNodeId == null || targetNodeId.equals(nodeManager.getNodeId())) {
            // Executing command requested by another node
            String result = executeLocalCommand(command);
            if (result != null && nodeManager.getRedisMessagePublisher() != null) {
                try {
                    // Publish the result so the requesting node can receive it
                    nodeManager.getRedisMessagePublisher().publishRelay(NodeMessageProtocol.CATEGORY_SCHEDULER, result);
                } catch (Exception e) {
                    logger.error("Failed to publish scheduler result to Redis", e);
                }
            }
        }
    }

    private String executeLocalCommand(String command) {
        try {
            if (command.startsWith(COMMAND_LIST)) {
                return getSchedulerListJson();
            } else if (command.startsWith(COMMAND_ENABLE)) {
                return changeActiveState(command.substring(COMMAND_ENABLE.length() + 1), false);
            } else if (command.startsWith(COMMAND_DISABLE)) {
                return changeActiveState(command.substring(COMMAND_DISABLE.length() + 1), true);
            }
        } catch (Exception e) {
            logger.error("Failed to process local scheduler command: {}", command, e);
        }
        return null;
    }

    private String getSchedulerListJson() {
        JsonBuilder jsonBuilder = new JsonBuilder().object();
        jsonBuilder.put("type", "list");
        jsonBuilder.array("services");

        int serviceCount = 0;
        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service.getServiceLifeCycle().isActive()) {
                ScheduleRuleRegistry registry = service.getActivityContext().getScheduleRuleRegistry();
                if (registry != null) {
                    jsonBuilder.object();
                    jsonBuilder.put("serviceName", service.getServiceName());
                    jsonBuilder.put("contextName", service.getActivityContext().getName());
                    jsonBuilder.array("schedules");
                    for (ScheduleRule scheduleRule : registry.getScheduleRules()) {
                        ScheduleParameters params = RulesToParameters.toScheduleParameters(scheduleRule);
                        jsonBuilder.put(params);
                    }
                    jsonBuilder.endArray();
                    jsonBuilder.endObject();
                    serviceCount++;
                }
            }
        }

        jsonBuilder.endArray();
        jsonBuilder.endObject();

        logger.debug("Collected scheduler list from {} active services", serviceCount);
        return jsonBuilder.toString();
    }

    private String changeActiveState(String target, boolean disabled) {
        String[] parts = target.split(":");
        if (parts.length < 3) {
            return null;
        }

        String serviceName = parts[0];
        String type = parts[1];
        String id = parts[2];

        boolean changed = false;
        String resultMessage;

        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service.getServiceName().equals(serviceName) && service.getServiceLifeCycle().isActive()) {
                ScheduleRuleRegistry registry = service.getActivityContext().getScheduleRuleRegistry();
                if (registry != null) {
                    if ("schedule".equals(type)) {
                        ScheduleRule scheduleRule = registry.getScheduleRule(id);
                        if (scheduleRule != null && !scheduleRule.isIsolated()) {
                            scheduleRule.setDisabled(disabled);
                            changed = true;
                        }
                    } else if ("job".equals(type)) {
                        Set<ScheduledJobRule> jobRules = registry.getScheduledJobRules(new String[] { id });
                        if (!jobRules.isEmpty()) {
                            for (ScheduledJobRule jobRule : jobRules) {
                                if (!jobRule.isIsolated()) {
                                    jobRule.setDisabled(disabled);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }

        if (changed) {
            resultMessage = (disabled ? "Disabled" : "Enabled") + " " + type + " '" + id + "' in service '" + serviceName + "'";
            // After change, we might want to return the updated list or just the result
            // Usually, a result message is sent, and the UI might request a list refresh
        } else {
            resultMessage = "Failed to change state for " + type + " '" + id + "' in service '" + serviceName + "' (Not found or isolated)";
        }

        JsonBuilder jsonBuilder = new JsonBuilder().object();
        jsonBuilder.put("type", "result");
        jsonBuilder.put("success", changed);
        jsonBuilder.put("message", resultMessage);
        jsonBuilder.endObject();

        return jsonBuilder.toString();
    }

    /**
     * Handles an incoming scheduler management result (from Redis or local execution).
     * This will be pushed to connected clients via WebSockets or Polling.
     * @param resultData the result payload
     */
    public void handleSchedulerResult(String resultData) {
        if (logger.isTraceEnabled()) {
            logger.trace("Received scheduler result, bridging to clients: {}", resultData);
        }
        if (broker != null) {
            broker.bridge(resultData);
        }
    }

}
