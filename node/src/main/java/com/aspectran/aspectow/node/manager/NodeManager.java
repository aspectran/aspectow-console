/*
 * Copyright (c) 2020-present The Aspectran Project
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
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.config.SecretConfig;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;

import java.util.List;

/**
 * The main manager for Aspectow AppMon.
 * This class orchestrates the entire monitoring application, including configuration,
 * exporters, persistence, and lifecycle management.
 * It also provides access to the core components of Aspectran's ActivityContext.
 */
public class NodeManager {

    private final String nodeId;

    private final ClusterConfig clusterConfig;

    private final NodeInfoHolder nodeInfoHolder;

    private NodeRegistry nodeRegistry;

    private NodeReporter nodeReporter;

    private RedisMessagePublisher redisMessagePublisher;

    private RedisMessageSubscriber redisMessageSubscriber;

    public NodeManager(String nodeId, ClusterConfig clusterConfig, NodeInfoHolder nodeInfoHolder) {
        this.nodeId = nodeId;
        this.clusterConfig = clusterConfig;
        this.nodeInfoHolder = nodeInfoHolder;
    }

    /**
     * Gets the ID of the current node.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Gets the cluster configuration.
     * @return the cluster configuration
     */
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    public NodeInfoHolder getNodeInfoHolder() {
        return nodeInfoHolder;
    }

    /**
     * Gets the list of all node information.
     * @return the list of node information
     */
    public List<NodeInfo> getNodeInfoList() {
        return nodeInfoHolder.getNodeInfoList();
    }

    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    public void setNodeRegistry(NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }

    /**
     * Gets the node reporter.
     * @return the node reporter
     */
    public NodeReporter getNodeReporter() {
        return nodeReporter;
    }

    /**
     * Sets the node reporter.
     * @param nodeReporter the node reporter
     */
    public void setNodeReporter(NodeReporter nodeReporter) {
        this.nodeReporter = nodeReporter;
    }

    public RedisMessagePublisher getRedisMessagePublisher() {
        return redisMessagePublisher;
    }

    public void setRedisMessagePublisher(RedisMessagePublisher redisMessagePublisher) {
        this.redisMessagePublisher = redisMessagePublisher;
    }

    public RedisMessageSubscriber getRedisMessageSubscriber() {
        return redisMessageSubscriber;
    }

    public void setRedisMessageSubscriber(RedisMessageSubscriber redisMessageSubscriber) {
        this.redisMessageSubscriber = redisMessageSubscriber;
    }

    /**
     * Creates a time-limited authentication token for this node.
     * @return an encrypted token string
     */
    public String generateToken() {
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
        payload.putValue("nodeId", nodeId);
        payload.putValue("clusterId", clusterConfig.getId());

        // Default 30 seconds expiration
        return TimeLimitedPBTokenIssuer.createToken(payload, 30000L, password, salt);
    }

    /**
     * Validates the given authentication token.
     * @param token the token string to validate
     * @throws InvalidPBTokenException if the token is invalid or expired
     */
    public void validateToken(String token) throws InvalidPBTokenException {
        SecretConfig secretConfig = clusterConfig.getSecretConfig();
        String password = (secretConfig != null ? secretConfig.getPassword() : null);
        if (password == null) {
            password = PBEncryptionUtils.getPassword();
        }
        if (password == null) {
            throw new InvalidPBTokenException(token, "Encryption password not found for token validation");
        }
        String salt = (secretConfig != null ? secretConfig.getSalt() : PBEncryptionUtils.getSalt());

        TimeLimitedPBTokenIssuer.validate(token, password, salt);
    }

}
