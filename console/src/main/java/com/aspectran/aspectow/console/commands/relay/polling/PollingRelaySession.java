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
package com.aspectran.aspectow.console.commands.relay.polling;

import com.aspectran.aspectow.console.commands.relay.RelaySession;
import com.aspectran.utils.concurrent.AutoLock;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RelaySession} implementation for HTTP polling.
 * It manages a message queue for a specific client session.
 */
public class PollingRelaySession implements RelaySession {

    private final String nodeId;

    private final AutoLock autoLock = new AutoLock();

    private final List<String> messageQueue = new ArrayList<>();

    private volatile long lastAccessTime;

    private boolean expired;

    public PollingRelaySession(String nodeId) {
        this.nodeId = nodeId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean isValid() {
        return !expired;
    }

    /**
     * Pushes a message to the session's individual queue.
     * @param message the message to push
     */
    public void push(String message) {
        try (AutoLock ignored = autoLock.lock()) {
            if (isValid()) {
                messageQueue.add(message);
            }
        }
    }

    /**
     * Pops all messages from the session's individual queue.
     * @return a list of messages, or {@code null} if the queue is empty
     */
    public List<String> popMessages() {
        try (AutoLock ignored = autoLock.lock()) {
            if (messageQueue.isEmpty()) {
                return null;
            }
            List<String> messages = new ArrayList<>(messageQueue);
            messageQueue.clear();
            return messages;
        }
    }

    public void access() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void expire() {
        try (AutoLock ignored = autoLock.lock()) {
            this.expired = true;
            this.messageQueue.clear();
        }
    }

}
