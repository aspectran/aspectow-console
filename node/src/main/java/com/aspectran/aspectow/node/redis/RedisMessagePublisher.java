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
package com.aspectran.aspectow.node.redis;

import com.aspectran.aspectow.node.manager.NodeRegistryProtocol;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisMessagePublisher provides methods to publish management control messages
 * and transparent application data to Redis Pub/Sub channels.
 */
public class RedisMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private final String clusterId;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    public RedisMessagePublisher(String clusterId, String nodeId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.connectionPool = connectionPool;
    }

    /**
     * Publishes a management control message for this node.
     * This method waits for the publication to complete.
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishControl(String message) throws Exception {
        String channel = NodeRegistryProtocol.getControlChannel(clusterId, nodeId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.sync().publish(channel, message);
        }
    }

    /**
     * Publishes a transparent application message to be relayed from this node.
     * This method sends the message asynchronously and does not wait for completion.
     * @param category the category of the relay message
     * @param message the message to publish
     * @throws Exception if an error occurs while obtaining a connection
     */
    public void publishRelay(String category, String message) throws Exception {
        String channel = NodeRegistryProtocol.getRelayChannel(clusterId, nodeId, category);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.async().publish(channel, message);
        }
    }

    /**
     * Publishes a message to a specific channel synchronously.
     * @param channel the channel to publish to
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publish(String channel, String message) throws Exception {
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.sync().publish(channel, message);
        }
    }

}
