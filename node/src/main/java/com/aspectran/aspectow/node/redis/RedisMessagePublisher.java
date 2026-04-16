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

import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisMessagePublisher sends messages to a Redis Pub/Sub channel 
 * for remote monitoring or command delivery.
 */
@Component
public class RedisMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private static final String LOG_CHANNEL_PREFIX = "aspectow:cluster:logs:";

    private final String clusterName;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    private StatefulRedisConnection<String, String> connection;

    @Autowired
    public RedisMessagePublisher(NodeConfig nodeConfig, RedisConnectionPool connectionPool) {
        this.clusterName = nodeConfig.getClusterConfig().getName();
        this.nodeId = nodeConfig.getNodeInfo().getName();
        this.connectionPool = connectionPool;
    }

    @Initialize
    public void initialize() {
        this.connection = connectionPool.getConnection();
        logger.info("RedisMessagePublisher initialized for node: {}", nodeId);
    }

    @Destroy
    public void destroy() {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Publishes a message to the cluster log channel.
     * @param message the message to publish
     */
    public void publishLog(String message) {
        String channel = LOG_CHANNEL_PREFIX + clusterName + ":" + nodeId;
        if (connection != null) {
            connection.async().publish(channel, message);
        }
    }

    /**
     * Publishes a message to a specific channel.
     * @param channel the channel to publish to
     * @param message the message to publish
     */
    public void publish(String channel, String message) {
        if (connection != null) {
            connection.async().publish(channel, message);
        }
    }

}
