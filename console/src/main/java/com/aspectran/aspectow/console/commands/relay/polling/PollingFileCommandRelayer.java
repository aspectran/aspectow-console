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

import com.aspectran.aspectow.console.commands.manager.FileCommandRelayer;
import com.aspectran.aspectow.console.commands.manager.FileCommanderManager;
import com.aspectran.aspectow.console.commands.relay.RelaySession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PollingFileCommandRelayer manages client sessions for HTTP long-polling
 * and queues command results until they are requested.
 */
@Component
public class PollingFileCommandRelayer implements FileCommandRelayer {

    private static final Logger logger = LoggerFactory.getLogger(PollingFileCommandRelayer.class);

    private final FileCommanderManager fileCommanderManager;

    private final Map<String, PollingRelaySession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public PollingFileCommandRelayer(FileCommanderManager fileCommanderManager) {
        this.fileCommanderManager = fileCommanderManager;
    }

    @Initialize
    public void register() {
        if (fileCommanderManager.getRelayManager() != null) {
            fileCommanderManager.getRelayManager().addRelayer(this);
            logger.info("PollingFileCommandRelayer registered with FileCommanderManager");
        }
    }

    public PollingRelaySession createSession(String nodeId) {
        // Use a unique session ID for the client
        String sessionId = java.util.UUID.randomUUID().toString();
        PollingRelaySession session = new PollingRelaySession(nodeId);
        sessions.put(sessionId, session);
        return session;
    }

    public PollingRelaySession getSession(String sessionId) {
        PollingRelaySession session = sessions.get(sessionId);
        if (session != null) {
            session.access();
        }
        return session;
    }

    public void removeSession(String sessionId) {
        PollingRelaySession session = sessions.remove(sessionId);
        if (session != null) {
            session.expire();
        }
    }

    @Override
    public void relay(String data) {
        for (PollingRelaySession session : sessions.values()) {
            if (session.isValid()) {
                session.push(data);
            }
        }
    }

    @Override
    public void relay(@NonNull RelaySession relaySession, String data) {
        if (relaySession instanceof PollingRelaySession pollingRelaySession) {
            if (pollingRelaySession.isValid()) {
                pollingRelaySession.push(data);
            }
        }
    }

    /**
     * Scavenges expired sessions.
     * @param timeoutMillis the timeout in milliseconds
     */
    public void scavenge(long timeoutMillis) {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastAccessTime() > timeoutMillis);
            if (expired) {
                entry.getValue().expire();
            }
            return expired;
        });
    }

}
