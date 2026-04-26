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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SchedulerManager manages schedulers across the cluster.
 * It sends control commands to nodes and handles results via Redis relay.
 */
@Component
@Bean(id = "schedulerManager")
public class SchedulerManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

    private final NodeManager nodeManager;

    private final SchedulerBroker broker;

    public SchedulerManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.broker = new SchedulerBroker(nodeManager.getNodeId(), nodeManager.getRedisMessagePublisher());
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing SchedulerManager for node: {}", nodeManager.getNodeId());

        // Register a listener for scheduler relay messages (results) from Redis
        if (nodeManager.getRedisMessageSubscriber() != null) {
            SchedulerMessageBridgeHandler bridgeHandler = new SchedulerMessageBridgeHandler(this);
            nodeManager.getRedisMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public SchedulerBroker getBroker() {
        return broker;
    }

    /**
     * Sends a scheduler management command to the cluster.
     * @param targetNodeId the ID of the node to execute the command (information only, targeting handled in data)
     * @param command the command string
     */
    public void sendCommand(String targetNodeId, String command) {
        if (nodeManager.getRedisMessagePublisher() != null) {
            try {
                // Follow the same pattern as RemoteCommandManager
                nodeManager.getRedisMessagePublisher().publishRelay(NodeMessageProtocol.CATEGORY_SCHEDULER, command);
                logger.debug("Scheduler command relayed to cluster: {}", command);
            } catch (Exception e) {
                logger.error("Failed to relay scheduler command to Redis", e);
            }
        }
    }

    /**
     * Handles an incoming scheduler management result from Redis.
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
