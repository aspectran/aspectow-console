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

import com.aspectran.aspectow.console.commands.manager.FileCommandRelayManager;
import com.aspectran.aspectow.console.commands.manager.FileCommanderManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileCommandRelayHandler listens to Redis relay messages and forwards
 * them to the FileCommanderManager.
 */
public class FileCommandMessageRelayHandler implements RedisMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(FileCommandMessageRelayHandler.class);

    private final FileCommanderManager fileCommanderManager;

    public FileCommandMessageRelayHandler(FileCommanderManager fileCommanderManager) {
        this.fileCommanderManager = fileCommanderManager;
    }

    @Override
    public String getCategory() {
        return FileCommandRelayManager.CATEGORY_COMMANDS;
    }

    @Override
    public void onRelayMessage(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace("Forwarding relay message to FileCommanderManager: {}", message);
        }
        fileCommanderManager.handleCommandResult(message);
    }

}
