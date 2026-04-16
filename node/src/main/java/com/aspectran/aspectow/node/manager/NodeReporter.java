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
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.apon.AponWriter;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * NodeReporter is responsible for reporting the node's status to the Redis registry.
 * It periodically updates the pulse to indicate that the node is still alive.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
public class NodeReporter implements InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NodeReporter.class);

    private static final String NODES_HASH_KEY_PREFIX = "aspectow:cluster:nodes:";

    private final NodeConfig nodeConfig;

    private final String clusterName;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private StatefulRedisConnection<String, String> connection;

    @Autowired
    public NodeReporter(NodeConfig nodeConfig, RedisConnectionPool connectionPool) {
        this.nodeConfig = nodeConfig;
        this.connectionPool = connectionPool;
        this.clusterName = nodeConfig.getClusterConfig().getName();
        this.nodeId = nodeConfig.getNodeInfo().getName();
    }

    @Initialize
    @Override
    public void initialize() throws Exception {
        logger.info("Initializing NodeReporter for cluster: {}, node: {}", clusterName, nodeId);
        
        // Establish Redis connection
        this.connection = connectionPool.getConnection();

        // 1. Register the node in Redis Hash
        registerNode();

        // 2. Start periodic pulse update
        long interval = nodeConfig.getNodeInfo().getHeartbeatInterval(5000);
        scheduler.scheduleAtFixedRate(this::sendPulse, 0, interval, TimeUnit.MILLISECONDS);
    }

    @Destroy
    @Override
    public void destroy() throws Exception {
        logger.info("Destroying NodeReporter for node: {}", nodeId);
        scheduler.shutdown();
        unregisterNode();
        if (connection != null) {
            connection.close();
        }
    }

    private void registerNode() throws IOException {
        String key = NODES_HASH_KEY_PREFIX + clusterName;
        NodeInfo nodeInfo = nodeConfig.getNodeInfo();
        
        // Convert NodeInfo to APON string for storage
        String aponData = new AponWriter().nullWritable(false).write(nodeInfo).toString();
        
        logger.debug("Registering node {} in Redis hash {}: \n{}", nodeId, key, aponData);
        RedisCommands<String, String> sync = connection.sync();
        sync.hset(key, nodeId, aponData);
    }

    private void sendPulse() {
        String key = NODES_HASH_KEY_PREFIX + clusterName + ":pulse";
        long timestamp = System.currentTimeMillis();
        
        logger.trace("Sending pulse for node {} to {}: {}", nodeId, key, timestamp);
        RedisCommands<String, String> sync = connection.sync();
        sync.hset(key, nodeId, String.valueOf(timestamp));
    }

    private void unregisterNode() {
        String key = NODES_HASH_KEY_PREFIX + clusterName;
        logger.debug("Unregistering node {} from Redis hash {}", nodeId, key);
        RedisCommands<String, String> sync = connection.sync();
        sync.hdel(key, nodeId);
    }

}
