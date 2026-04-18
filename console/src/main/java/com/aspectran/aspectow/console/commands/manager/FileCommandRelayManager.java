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
package com.aspectran.aspectow.console.commands.manager;

import com.aspectran.aspectow.console.commands.relay.RelaySession;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * FileCommandRelayManager handles the distribution of command results
 * to connected clients (via WebSockets or Polling).
 */
public class FileCommandRelayManager {

    private static final Logger logger = LoggerFactory.getLogger(FileCommandRelayManager.class);

    private final String nodeId;

    private final RedisMessagePublisher messagePublisher;

    private final Set<FileCommandRelayer> relayers = new CopyOnWriteArraySet<>();

    public FileCommandRelayManager(String nodeId, RedisMessagePublisher messagePublisher) {
        this.nodeId = nodeId;
        this.messagePublisher = messagePublisher;
    }

    public void addRelayer(FileCommandRelayer relayer) {
        relayers.add(relayer);
    }

    public void removeRelayer(FileCommandRelayer relayer) {
        relayers.remove(relayer);
    }

    /**
     * Relays a command result to all connected clients.
     * @param resultData the result payload to send
     */
    public void relay(String resultData) {
        for (FileCommandRelayer relayer : relayers) {
            try {
                relayer.relay(resultData);
            } catch (Exception e) {
                logger.warn("Failed to relay command result via {}: {}", relayer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Relays a command result to a specific session.
     * @param session the target relay session
     * @param resultData the result payload to send
     */
    public void relay(RelaySession session, String resultData) {
        for (FileCommandRelayer relayer : relayers) {
            try {
                relayer.relay(session, resultData);
            } catch (Exception e) {
                logger.warn("Failed to relay command result via {}: {}", relayer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public RedisMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    public String getNodeId() {
        return nodeId;
    }

}
