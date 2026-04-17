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
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NodeManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NodeManagerBuilder.class);

    public static final String MY_NODE_ID_PROPERTY_NAME = "aspectow.cluster.node.id";

    public static final String DEFAULT_CLUSTER_ID = "cluster";

    public static final String DEFAULT_NODE_ID = "node1";

    @NonNull
    public static NodeManager build(ActivityContext context, NodeConfig nodeConfig) throws Exception {
        Assert.notNull(context, "context must not be null");
        Assert.notNull(nodeConfig, "nodeConfig must not be null");

        NodeInfoHolder nodeInfoHolder = new NodeInfoHolder(nodeConfig.getNodeInfoList());

        String nodeId = resolveMyNodeId();
        logger.info("Current Node: {}", nodeId);

        NodeInfo nodeInfo = nodeInfoHolder.getNodeInfo(nodeId);
        if (nodeInfo == null) {
            nodeInfo = new NodeInfo();
            nodeInfo.setName(nodeId);
            nodeConfig.putNodeInfo(nodeInfo);
        }

        ClusterConfig clusterConfig = nodeConfig.getClusterConfig();
        if (clusterConfig == null) {
            clusterConfig = new ClusterConfig();
            nodeConfig.setClusterConfig(clusterConfig);
        }

        String clusterName = clusterConfig.getName();
        if (!StringUtils.hasText(clusterName)) {
            clusterConfig.setName(DEFAULT_CLUSTER_ID);
        }

        if (!context.getBeanRegistry().containsBean(RedisConnectionPool.class)) {
            throw new Exception("RedisConnectionPool bean not found in the context");
        }
        RedisConnectionPool connectionPool = context.getBeanRegistry().getBean(RedisConnectionPool.class);

        NodeRegistry nodeRegistry = new NodeRegistry(clusterName, connectionPool);
        NodeReporter nodeReporter = new NodeReporter(clusterName, nodeInfo, connectionPool);
        RedisMessagePublisher redisMessagePublisher = null;
        RedisMessageSubscriber redisMessageSubscriber = null;

        String clusterMode = clusterConfig.getMode();
        if ("gateway".equals(clusterMode) || "autoscaling".equals(clusterMode)) {
            redisMessagePublisher = new RedisMessagePublisher(clusterName, nodeId, connectionPool);
            redisMessageSubscriber = new RedisMessageSubscriber(clusterName, nodeId, connectionPool);
        }

        NodeManager nodeManager = new NodeManager(nodeId, nodeInfoHolder);
        nodeManager.setActivityContext(context);
        nodeManager.setNodeRegistry(nodeRegistry);
        nodeManager.setNodeReporter(nodeReporter);
        nodeManager.setRedisMessagePublisher(redisMessagePublisher);
        nodeManager.setRedisMessageSubscriber(redisMessageSubscriber);
        return nodeManager;
    }

    private static String resolveMyNodeId() {
        return SystemUtils.getProperty(MY_NODE_ID_PROPERTY_NAME, DEFAULT_NODE_ID);
    }

}
