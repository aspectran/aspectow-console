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

import com.aspectran.aspectow.console.commands.relay.redis.FileCommandMessageRelayHandler;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileCommanderManager manages file-related commands across the cluster.
 * It handles local command execution in direct mode and relays commands
 * via Redis in gateway/autoscaling modes.
 */
@Component
@Bean(id = "fileCommanderManager")
public class FileCommanderManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(FileCommanderManager.class);

    private final NodeManager nodeManager;

    private FileCommandRelayManager relayManager;

    public FileCommanderManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing FileCommanderManager for node: {}", nodeManager.getNodeId());

        this.relayManager = new FileCommandRelayManager(nodeManager.getNodeId(), nodeManager.getRedisMessagePublisher());

        // Register a listener for command results from Redis
        if (nodeManager.getRedisMessageSubscriber() != null) {
            FileCommandMessageRelayHandler relayHandler = new FileCommandMessageRelayHandler(this);
            nodeManager.getRedisMessageSubscriber().addListener(relayHandler);
        }
    }

    public FileCommandRelayManager getRelayManager() {
        return relayManager;
    }

    /**
     * Sends a file command to a specific node.
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
                nodeManager.getRedisMessagePublisher().publishRelay(FileCommandRelayManager.CATEGORY_COMMANDS, commandData);
            } else {
                throw new IllegalStateException("Redis publisher is not available for relaying commands");
            }
        }
    }

    private void processLocalCommand(String commandData) {
        logger.info("Processing local file command: {}", commandData);
        // Implementation for local command handling
        // For testing, just simulate a result back
        handleCommandResult(commandData + " (processed locally)");
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
