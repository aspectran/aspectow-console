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

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.commands.manager.FileCommandRelayer;
import com.aspectran.aspectow.console.commands.manager.FileCommanderManager;
import com.aspectran.aspectow.console.commands.relay.RelaySession;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebsocketFileCommandRelayer provides a WebSocket endpoint for real-time
 * file command result delivery.
 */
@Component
@ServerEndpoint(
        value = "/file-commander/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketFileCommandRelayer extends SimplifiedEndpoint implements FileCommandRelayer {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketFileCommandRelayer.class);

    private final FileCommanderManager fileCommanderManager;

    private final NodeManager nodeManager;

    @Autowired
    public WebsocketFileCommandRelayer(FileCommanderManager fileCommanderManager, NodeManager nodeManager) {
        this.fileCommanderManager = fileCommanderManager;
        this.nodeManager = nodeManager;
    }

    @Initialize
    public void register() {
        if (fileCommanderManager.getRelayManager() != null) {
            fileCommanderManager.getRelayManager().addRelayer(this);
            logger.info("WebsocketFileCommandRelayer registered with FileCommanderManager");
        }
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
            return true;
        } catch (InvalidPBTokenException e) {
            logger.warn("WebSocket connection rejected: invalid or expired token");
            return false;
        }
    }

    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message -> {
                setLoggingGroup();
                handleMessage(session, message);
            });
        }
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (message.startsWith("command:execute")) {
            execute(session, message);
        } else if ("command:join".equals(message)) {
            join(session);
        } else if ("command:ping".equals(message)) {
            pong(session);
        }
    }

    private void join(Session session) {
        WebsocketRelaySession relaySession = new WebsocketRelaySession(session);
        relaySession.setNodeId(nodeManager.getNodeId());
        if (addSession(session)) {
            sendText(session, "joined:" + nodeManager.getNodeId());
            logger.debug("ConsoleClient joined: session {}", session.getId());
        }
    }

    private void pong(Session session) {
        sendText(session, "pong");
    }

    private void execute(Session session, String message) {
        String[] parts = message.split("\n");
        String targetNodeId = null;
        String commandData = null;
        for (String part : parts) {
            if (part.startsWith("targetNodeId:")) {
                targetNodeId = part.substring(13).trim();
            } else if (part.startsWith("data:")) {
                commandData = part.substring(5).trim();
            }
        }
        if (commandData != null) {
            if (targetNodeId == null || targetNodeId.isEmpty()) {
                targetNodeId = nodeManager.getNodeId();
            }
            try {
                fileCommanderManager.executeCommand(targetNodeId, commandData);
                logger.debug("Command execution initiated from session {}: target={}, data={}",
                        session.getId(), targetNodeId, commandData);
            } catch (Exception e) {
                logger.error("Failed to execute command from session {}", session.getId(), e);
                sendText(session, "[ERROR] " + e.getMessage());
            }
        }
    }

    @Override
    protected void onSessionRemoved(Session session) {
        logger.debug("WebSocket session removed: {} (Total: {})", session.getId(), countSessions());
    }

    @Override
    public void relay(String data) {
        if (StringUtils.hasText(data)) {
            broadcast(data);
        }
    }

    @Override
    public void relay(@NonNull RelaySession relaySession, String data) {
        if (relaySession instanceof WebsocketRelaySession websocketRelaySession) {
            sendText(websocketRelaySession.getSession(), data);
        }
    }

}
