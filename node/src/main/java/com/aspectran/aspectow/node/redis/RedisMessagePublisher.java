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

import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisMessagePublisher sends messages to a Redis Pub/Sub channel 
 * for remote monitoring or command delivery.
 */
public class RedisMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private static final String CHANNEL_PREFIX = "aspectow:cluster:";

    private final String clusterName;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    public RedisMessagePublisher(String clusterName, String nodeId, RedisConnectionPool connectionPool) {
        this.clusterName = clusterName;
        this.nodeId = nodeId;
        this.connectionPool = connectionPool;
    }

    /**
     * Publishes a message to the cluster log channel.
     * @param message the message to publish
     */
    public void publish(String message) throws Exception {
        String channel = CHANNEL_PREFIX + clusterName + ":" + nodeId;
        publish(channel, message);
    }

    /**
     * Publishes a message to a specific channel.
     * @param channel the channel to publish to
     * @param message the message to publish
     */
    public void publish(String channel, String message) throws Exception {
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.async().publish(channel, message);
        }
    }

}
