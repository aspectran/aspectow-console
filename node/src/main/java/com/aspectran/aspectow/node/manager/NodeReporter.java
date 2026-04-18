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

import com.aspectran.aspectow.node.config.ClusterConfig;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.SecretConfig;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.SystemUtils;
import com.aspectran.utils.apon.AponWriter;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;
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
public class NodeReporter {

    private static final Logger logger = LoggerFactory.getLogger(NodeReporter.class);

    private static final String NODES_HASH_KEY_PREFIX = "aspectow:cluster:nodes:";

    private static final long DEFAULT_HEARTBEAT_INTERVAL = 5000L;

    private final ClusterConfig clusterConfig;

    private final NodeInfo nodeInfo;

    private final RedisConnectionPool connectionPool;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public NodeReporter(ClusterConfig clusterConfig, NodeInfo nodeInfo, RedisConnectionPool connectionPool) {
        this.clusterConfig = clusterConfig;
        this.nodeInfo = nodeInfo;
        this.connectionPool = connectionPool;
    }

    public void start() throws Exception {
        logger.info("Initializing NodeReporter for cluster: {}, node: {}", 
                clusterConfig.getId(), nodeInfo.getNodeId());
        
        // 1. Register the node in Redis Hash
        registerNode();

        // 2. Start periodic pulse update
        long interval = nodeInfo.getHeartbeatInterval(clusterConfig.getHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL));
        scheduler.scheduleAtFixedRate(this::sendPulse, 0, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        logger.info("Stopping NodeReporter for node: {}", nodeInfo.getNodeId());
        scheduler.shutdown();
        unregisterNode();
    }

    private void registerNode() throws IOException {
        String key = NODES_HASH_KEY_PREFIX + clusterConfig.getId();

        // Generate and set authentication token
        nodeInfo.setToken(generateToken());

        // Convert NodeInfo to APON string for storage
        String aponData = new AponWriter().nullWritable(false).write(nodeInfo).toString();
        
        logger.debug("Registering node {} in Redis hash {}:\n{}", nodeInfo.getNodeId(), key, aponData);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            sync.hset(key, nodeInfo.getNodeId(), aponData);
        } catch (Exception e) {
            logger.error("Failed to register node {} in Redis registry", nodeInfo.getNodeId(), e);
        }
    }

    private String generateToken() {
        SecretConfig secretConfig = clusterConfig.getSecretConfig();
        String password = (secretConfig != null ? secretConfig.getPassword() : null);
        if (password == null) {
            password = PBEncryptionUtils.getPassword();
        }
        if (password == null) {
            throw new IllegalStateException("Encryption password not found for token generation");
        }
        String salt = (secretConfig != null ? secretConfig.getSalt() : PBEncryptionUtils.getSalt());

        VariableParameters payload = new VariableParameters();
        payload.putValue("nodeId", nodeInfo.getNodeId());
        payload.putValue("clusterId", clusterConfig.getId());

        return TimeLimitedPBTokenIssuer.createToken(payload, 30000L, password, salt);
    }

    private void sendPulse() {
        String key = NODES_HASH_KEY_PREFIX + clusterConfig.getId() + ":pulse";
        long timestamp = System.currentTimeMillis();
        
        logger.trace("Sending pulse for node {} to {}: {}", nodeInfo.getNodeId(), key, timestamp);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            sync.hset(key, nodeInfo.getNodeId(), String.valueOf(timestamp));
        } catch (Exception e) {
            logger.error("Failed to send pulse for node {} to Redis registry", nodeInfo.getNodeId(), e);
        }
    }

    private void unregisterNode() {
        String key = NODES_HASH_KEY_PREFIX + clusterConfig.getId();
        logger.debug("Unregistering node {} from Redis hash {}", nodeInfo.getNodeId(), key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            sync.hdel(key, nodeInfo.getNodeId());
        } catch (Exception e) {
            logger.error("Failed to unregister node {} from Redis registry", nodeInfo.getNodeId(), e);
        }
    }

}
