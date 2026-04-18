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
package com.aspectran.aspectow.console.commands.relay.websocket;

import com.aspectran.aspectow.console.commands.relay.RelaySession;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

/**
 * A {@link RelaySession} implementation that wraps a JSR-356 {@link Session}.
 */
public class WebsocketRelaySession extends WrappedSession implements RelaySession {

    private final String nodeId;

    /**
     * Instantiates a new WebsocketRelaySession.
     * @param nodeId the node ID
     * @param session the underlying WebSocket session
     */
    public WebsocketRelaySession(String nodeId, Session session) {
        super(session);
        this.nodeId = nodeId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}
