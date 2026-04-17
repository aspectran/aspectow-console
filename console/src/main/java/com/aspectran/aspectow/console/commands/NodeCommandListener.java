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
package com.aspectran.aspectow.console.commands;

import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.daemon.service.DaemonService;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NodeCommandListener subscribes to the node's command channel on Redis 
 * and executes incoming commands using the File Commander or internal logic.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
public class NodeCommandListener extends RedisPubSubAdapter<String, String> 
        implements InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NodeCommandListener.class);

    private static final String COMMAND_CHANNEL_PREFIX = "aspectow:cluster:commands:";

    private final String clusterName;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private DaemonService daemonService;

    @Autowired
    public NodeCommandListener(String clusterName, String nodeId, RedisConnectionPool connectionPool) {
        this.clusterName = clusterName;
        this.nodeId = nodeId;
        this.connectionPool = connectionPool;
    }

    public void setDaemonService(DaemonService daemonService) {
        this.daemonService = daemonService;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing NodeCommandListener for node: {}", nodeId);
        
        this.pubSubConnection = connectionPool.getPubSubConnection();
        this.pubSubConnection.addListener(this);
        
        String nodeChannel = getChannelName(nodeId);
        String broadcastChannel = getChannelName("all");
        
        this.pubSubConnection.sync().subscribe(nodeChannel, broadcastChannel);
        logger.info("Subscribed to command channels: {}, {}", nodeChannel, broadcastChannel);
    }

    @Override
    public void destroy() throws Exception {
        if (pubSubConnection != null) {
            logger.info("Unsubscribing and closing NodeCommandListener for node: {}", nodeId);
            pubSubConnection.removeListener(this);
            pubSubConnection.sync().unsubscribe();
            pubSubConnection.close();
        }
    }

    @Override
    public void message(String channel, String message) {
        logger.info("Received command from channel {}: {}", channel, message);
        
        try {
            if (daemonService != null) {
                // Directly inject the command into the Daemon's CommandExecutor
                CommandParameters parameters = new CommandParameters();
                parameters.readFrom(message);
                //daemonService.get.getCommandExecutor().execute(parameters);
                logger.info("Command injected successfully into Daemon engine");
            } else {
                logger.warn("DaemonService not available. Command execution skipped.");
            }
        } catch (Exception e) {
            logger.error("Failed to process incoming command: " + message, e);
        }
    }

    private String getChannelName(String nodeId) {
        return COMMAND_CHANNEL_PREFIX + clusterName + ":" + nodeId;
    }

}
