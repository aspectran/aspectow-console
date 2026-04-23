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
package com.aspectran.aspectow.console.commands.manager;

import com.aspectran.aspectow.console.commands.relay.redis.RemoteCommandMessageRelayHandler;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.daemon.command.CommandResult;
import com.aspectran.daemon.service.DefaultDaemonService;
import com.aspectran.daemon.service.DefaultDaemonServiceBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RemoteCommandManager manages commands across the cluster.
 * It handles local command execution in direct mode and relays commands
 * via Redis in gateway/autoscaling modes.
 */
@Component
@Bean(id = "remoteCommandManager")
public class RemoteCommandManager implements ActivityContextAware, InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandManager.class);

    private final NodeManager nodeManager;

    private ActivityContext activityContext;

    private RemoteCommandRelayManager relayManager;

    private DefaultDaemonService daemonService;

    public RemoteCommandManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public void setActivityContext(@NonNull ActivityContext activityContext) {
        this.activityContext = activityContext;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteCommandManager for node: {}", nodeManager.getNodeId());

        relayManager = new RemoteCommandRelayManager(nodeManager.getNodeId(), nodeManager.getRedisMessagePublisher());

        // Register a listener for command results from Redis
        if (nodeManager.getRedisMessageSubscriber() != null) {
            RemoteCommandMessageRelayHandler relayHandler = new RemoteCommandMessageRelayHandler(this);
            nodeManager.getRedisMessageSubscriber().addListener(relayHandler);
        }
    }

    private synchronized void setupDaemonService() throws Exception {
        if (daemonService != null) {
            return;
        }

        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service instanceof DefaultDaemonService ds) {
                daemonService = ds;
                break;
            }
        }

        if (daemonService == null) {
            CoreService baseService = null;
            if (activityContext != null) {
                baseService = activityContext.getMasterService().getRootService();
            } else {
                for (CoreService service : CoreServiceHolder.getAllServices()) {
                    baseService = service.getRootService();
                    break;
                }
            }

            if (baseService != null) {
                logger.info("No active DaemonService found; starting a new one based on root service [{}]",
                        baseService.getServiceName());
                daemonService = DefaultDaemonServiceBuilder.build(baseService);
                // The daemonService is added to baseService's sub-services during construction.
                // If the baseService is already active, the new daemonService is considered an
                // orphan and must be started manually.
                if (daemonService.getServiceLifeCycle().isOrphan()) {
                    daemonService.start();
                }
            } else {
                logger.warn("No Core Service found in CoreServiceHolder; cannot start DaemonService. " +
                        "This might be because RemoteCommandManager is initialized too early.");
            }
        } else {
            logger.info("Active DaemonService found: {}", daemonService.getServiceName());
        }
    }

    public RemoteCommandRelayManager getRelayManager() {
        return relayManager;
    }

    /**
     * Sends a command to a specific node.
     * @param targetNodeId the ID of the node to execute the command
     * @param commandData the command payload in APON/JSON format
     */
    public void executeCommand(String targetNodeId, String commandData) throws Exception {
        if (nodeManager.getNodeId().equals(targetNodeId)) {
            // Process locally
            processLocalCommand(commandData);
        } else {
            // Relay via Redis
            if (nodeManager.getRedisMessagePublisher() != null) {
                logger.debug("Relaying command to node {}: {}", targetNodeId, commandData);
                nodeManager.getRedisMessagePublisher().publishRelay(RemoteCommandRelayManager.CATEGORY_COMMANDS, commandData);
            } else {
                throw new IllegalStateException("Redis publisher is not available for relaying commands");
            }
        }
    }

    private void processLocalCommand(String commandData) throws Exception {
        logger.info("Processing local command: {}", commandData);
        if (daemonService == null) {
            setupDaemonService();
        }
        if (daemonService != null) {
            try {
                CommandResult commandResult = daemonService.execute(commandData);
                if (commandResult.isSuccess()) {
                    handleCommandResult(commandResult.getResult());
                } else {
                    handleCommandResult(commandResult.getResult());
                    if (commandResult.getError() != null) {
                        logger.error("Local command execution failed: {}", commandResult.getError());
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to execute local command", e);
                handleCommandResult("[FAILED] Error executing command: " + e.getMessage());
            }
        } else {
            logger.warn("DaemonService is not available for local command processing");
            handleCommandResult("[FAILED] Local DaemonService is not available");
        }
    }

    /**
     * Handles an incoming command result from Redis or local execution.
     * This will be pushed to connected clients via WebSocket or Polling.
     * @param resultData the result payload
     */
    public void handleCommandResult(String resultData) {
        if (logger.isTraceEnabled()) {
            logger.trace("Received command result, relaying to clients: {}", resultData);
        }
        if (relayManager != null) {
            relayManager.relay(resultData);
        }
    }

}
