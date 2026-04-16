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
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * RedisMessageSubscriber listens to Redis Pub/Sub channels
 * and notifies registered listeners.
 */
@Component
public class RedisMessageSubscriber extends RedisPubSubAdapter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private static final String CHANNEL_PATTERN = "aspectow:cluster:";

    private final String clusterName;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    private final Set<RedisMessageListener> listeners = new CopyOnWriteArraySet<>();

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    @Autowired
    public RedisMessageSubscriber(NodeConfig nodeConfig, RedisConnectionPool connectionPool) {
        this.clusterName = nodeConfig.getClusterConfig().getName();
        this.nodeId = nodeConfig.getNodeInfo().getName();
        this.connectionPool = connectionPool;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Initialize
    public void initialize() {
        this.pubSubConnection = connectionPool.getPubSubConnection();
        this.pubSubConnection.addListener(this);

        // Subscribe to all log channels for this cluster using pattern
        String pattern = CHANNEL_PATTERN + clusterName + ":*";
        this.pubSubConnection.sync().psubscribe(pattern);
        logger.info("RedisMessageSubscriber initialized and subscribed to pattern: {}", pattern);
    }

    @Destroy
    public void destroy() {
        if (pubSubConnection != null) {
            pubSubConnection.removeListener(this);
            pubSubConnection.sync().punsubscribe();
            pubSubConnection.close();
        }
    }

    public void addListener(RedisMessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RedisMessageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        // Extract nodeId from the channel: aspectow:cluster:logs:{clusterName}:{nodeId}
        int lastColonIndex = channel.lastIndexOf(':');
        if (lastColonIndex == -1) {
            return;
        }

        String senderNodeId = channel.substring(lastColonIndex + 1);

        // Skip messages from self to avoid infinite loops
        if (senderNodeId.equals(this.nodeId)) {
            return;
        }

        for (RedisMessageListener listener : listeners) {
            listener.onMessage(senderNodeId, message);
        }
    }

}
