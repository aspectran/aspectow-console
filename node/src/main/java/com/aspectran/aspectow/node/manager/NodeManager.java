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

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.core.activity.InstantAction;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * The main manager for Aspectow AppMon.
 * This class orchestrates the entire monitoring application, including configuration,
 * exporters, persistence, and lifecycle management.
 * It also provides access to the core components of Aspectran's ActivityContext.
 *
 * <p>Created: 4/3/2024</p>
 */
public class NodeManager extends InstantActivitySupport {

    private final String nodeId;

    private final NodeInfoHolder nodeInfoHolder;

    private NodeRegistry nodeRegistry;

    private NodeReporter nodeReporter;

    private RedisMessagePublisher redisMessagePublisher;

    private RedisMessageSubscriber redisMessageSubscriber;

    public NodeManager(String nodeId, NodeInfoHolder nodeInfoHolder) {
        this.nodeId = nodeId;
        this.nodeInfoHolder = nodeInfoHolder;
    }

    @Override
    @NonNull
    public ActivityContext getActivityContext() {
        return super.getActivityContext();
    }

    @Override
    @NonNull
    public ApplicationAdapter getApplicationAdapter() {
        return super.getApplicationAdapter();
    }

    /**
     * Gets the name of the current domain.
     * @return the current domain name
     */
    public String getNodeId() {
        return nodeId;
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
     * Sets the node reporter.
     * @param nodeReporter the node reporter
     */
    public void setNodeReporter(NodeReporter nodeReporter) {
        this.nodeReporter = nodeReporter;
    }

    @Override
    public <V> V instantActivity(InstantAction<V> instantAction) {
        return super.instantActivity(instantAction);
    }

    /**
     * Gets a bean from the ActivityContext's bean registry by its ID.
     * @param id the ID of the bean
     * @param <V> the type of the bean
     * @return the bean instance
     */
    public <V> V getBean(@NonNull String id) {
        return getActivityContext().getBeanRegistry().getBean(id);
    }

    /**
     * Gets a bean from the ActivityContext's bean registry by its type.
     * @param type the type of the bean
     * @param <V> the type of the bean
     * @return the bean instance
     */
    public <V> V getBean(Class<V> type) {
        return getActivityContext().getBeanRegistry().getBean(type);
    }

    /**
     * Checks if a bean of the given type exists in the ActivityContext's bean registry.
     * @param type the type of the bean
     * @return {@code true} if the bean exists, {@code false} otherwise
     */
    public boolean containsBean(Class<?> type) {
        return getActivityContext().getBeanRegistry().containsBean(type);
    }

}
