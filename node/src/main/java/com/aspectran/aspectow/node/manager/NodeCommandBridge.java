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
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NodeCommandBridge is used by the Console to send commands to a specific node
 * or a group of nodes through Redis Pub/Sub channels.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
public class NodeCommandBridge {

    private static final Logger logger = LoggerFactory.getLogger(NodeCommandBridge.class);

    private static final String COMMAND_CHANNEL_PREFIX = "aspectow:cluster:commands:";

    private final String clusterName;

    private final RedisConnectionPool connectionPool;

    @Autowired
    public NodeCommandBridge(NodeConfig nodeConfig, RedisConnectionPool connectionPool) {
        this.clusterName = nodeConfig.getClusterConfig().getName();
        this.connectionPool = connectionPool;
    }

    /**
     * Sends a command to a specific node.
     * @param nodeId the target node ID
     * @param command the command in APON or plain text format
     */
    public void sendCommand(String nodeId, String command) {
        String channel = getChannelName(nodeId);
        logger.debug("Sending command to node {}: {}", nodeId, command);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.sync().publish(channel, command);
        }
    }

    /**
     * Sends a command to all nodes in the cluster.
     * @param command the command to broadcast
     */
    public void broadcastCommand(String command) {
        String channel = getChannelName("all");
        logger.debug("Broadcasting command to all nodes in cluster {}: {}", clusterName, command);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.sync().publish(channel, command);
        }
    }

    private String getChannelName(String nodeId) {
        return COMMAND_CHANNEL_PREFIX + clusterName + ":" + nodeId;
    }

}
