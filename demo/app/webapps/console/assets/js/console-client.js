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

/**
 * ConsoleClient provides a common WebSocket interface for Console activities.
 * Handles connection management, heartbeats, and re-entry logic.
 *
 * @author Juho Jeong
 */
class ConsoleClient {

    constructor(node, options = {}) {
        this.node = node;
        this.options = Object.assign({
            heartbeatInterval: 5000,
            maxRetries: 10,
            retryInterval: 5000,
            token: null,
            onOpen: null,
            onMessage: null,
            onClose: null,
            onError: null,
            onEstablished: null,
            onJoined: null,
            onFailed: null,
            viewer: {
                printMessage: (msg) => console.log(this.node.id, msg),
                printErrorMessage: (msg) => console.error(this.node.id, msg),
                processMessage: (msg) => console.log(this.node.id, "received:", msg)
            }
        }, options);

        if (this.options.token && this.node.endpoint) {
            this.node.endpoint.token = this.options.token;
        }

        this.socket = null;
        this.heartbeatTimer = null;
        this.retryCount = 0;
        this.established = false;
        this.manualClose = false;
        this.pendingMessages = [];
        this.activityPath = null;
    }

    /**
     * Connects to the WebSocket server using the provided node endpoint.
     * @param {string} activityPath - The activity-specific WebSocket path (e.g., 'nodes', 'commands', 'scheduler')
     */
    start(activityPath) {
        this.activityPath = activityPath;
        this.manualClose = false;
        this.openSocket();
    }

    /**
     * Closes the WebSocket connection manually.
     */
    stop() {
        this.manualClose = true;
        this.closeSocket(false);
    }

    /**
     * Opens a new WebSocket connection.
     * @private
     */
    openSocket() {
        this.closeSocket(false);

        let path = this.node.endpoint.path;
        if (this.activityPath) {
            const p = (path.endsWith('/') ? path : path + '/');
            const a = (this.activityPath.startsWith('/') ? this.activityPath.substring(1) : this.activityPath);
            path = p + a;
        }
        const url = new URL(path + "/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log(this.node.id, "connecting to websocket:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = (event) => {
            console.log(this.node.id, "websocket connected");
            this.retryCount = 0;
            this.pendingMessages.push("Socket connection successful");

            const joinOptions = [
                "command:join",
                "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
            ];
            this.socket.send(joinOptions.join(";"));
            this.heartbeatPing();

            if (this.options.onOpen) {
                this.options.onOpen(event);
            }
        };

        this.socket.onmessage = (event) => {
            if (typeof event.data === "string") {
                const msg = event.data;
                if (this.established) {
                    if (msg.startsWith("pong:")) {
                        this.node.endpoint.token = msg.substring(5);
                        this.heartbeatPing();
                    } else {
                        if (this.options.onMessage) {
                            this.options.onMessage(msg);
                        } else {
                            this.options.viewer.processMessage(msg);
                        }
                    }
                } else if (msg.startsWith("joined:")) {
                    console.log(this.node.id, "joined", this.activityPath);
                    const payload = (msg.length > 7 ? JSON.parse(msg.substring(7)) : null);
                    this.establish(payload);
                }
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.options.onClose) {
                this.options.onClose(event);
            }
            if (!this.manualClose && event.code !== 1000) {
                this.rejoin();
            }
        };

        this.socket.onerror = (event) => {
            console.error(this.node.id, "websocket error:", event);
            this.options.viewer.printErrorMessage("Could not connect to the WebSocket server.");
            if (this.options.onError) {
                this.options.onError(event);
            }
            if (this.options.onFailed) {
                this.options.onFailed(this.node);
            }
        };
    }

    /**
     * Closes the socket and clears timers.
     * @param {boolean} afterClosing - whether this is called after the socket is already closed
     * @private
     */
    closeSocket(afterClosing) {
        if (this.socket) {
            this.established = false;
            if (!afterClosing) {
                this.socket.close();
            }
            this.socket = null;
        }
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    /**
     * Completes the connection process after receiving the 'joined' message.
     * @param {Object} payload - payload from the server
     * @private
     */
    establish(payload) {
        if (this.options.onJoined) {
            this.options.onJoined(this.node, payload);
        }
        while (this.pendingMessages.length) {
            this.options.viewer.printMessage(this.pendingMessages.shift());
        }
        if (this.options.onEstablished) {
            this.options.onEstablished(this.node);
        }
        this.established = true;
        this.socket.send("command:established");
    }

    /**
     * Retries the connection with exponential backoff.
     * @private
     */
    rejoin() {
        if (this.retryCount < this.options.maxRetries) {
            this.retryCount++;
            const jitter = Math.floor(Math.random() * 1000);
            const interval = (this.options.retryInterval * this.retryCount) + jitter;
            const status = "(" + this.retryCount + "/" + this.options.maxRetries + ", interval=" + interval + "ms)";

            this.options.viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(() => {
                this.openSocket();
            }, interval);
        } else {
            console.log(this.node.id, "abort reconnect attempt");
            this.options.viewer.printErrorMessage("Max connection attempts exceeded.");
            if (this.options.onFailed) {
                this.options.onFailed(this.node);
            }
        }
    }

    /**
     * Sends a command to the server.
     * @param {string} command - the command name
     * @param {string[]} [args] - optional arguments
     */
    sendCommand(command, args = []) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            const options = ["command:" + command, ...args];
            this.socket.send(options.join(";"));
        }
    }

    /**
     * Sends a heartbeat ping to the server.
     * @private
     */
    heartbeatPing() {
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send("command:ping");
            }
        }, this.options.heartbeatInterval);
    }

}
