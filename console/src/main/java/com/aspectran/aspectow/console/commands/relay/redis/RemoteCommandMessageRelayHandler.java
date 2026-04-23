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
package com.aspectran.aspectow.console.commands.relay.redis;

import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.aspectow.console.commands.relay.RemoteCommandRelayManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RemoteCommandMessageRelayHandler listens to Redis relay messages and forwards
 * them to the RemoteCommandManager.
 */
public class RemoteCommandMessageRelayHandler implements RedisMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandMessageRelayHandler.class);

    private final RemoteCommandManager remoteCommandManager;

    public RemoteCommandMessageRelayHandler(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
    }

    @Override
    public String getCategory() {
        return RemoteCommandRelayManager.CATEGORY_COMMANDS;
    }

    @Override
    public void onRelayMessage(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace("Forwarding relay message to RemoteCommandManager: {}", message);
        }
        remoteCommandManager.handleCommandResult(message);
    }

}
