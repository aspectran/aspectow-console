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

import com.aspectran.aspectow.console.commands.manager.FileCommanderManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.node.manager.NodeRegistryProtocol.NODES_BASE_PATH;

/**
 * RemoteCommandsActivity provides REST API endpoints for managing cluster nodes
 * and executing remote file commands.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component(NODES_BASE_PATH + "/${nodeId}/commands")
public class RemoteCommandsActivity {

    private final NodeManager nodeManager;

    private final FileCommanderManager fileCommanderManager;

    @Autowired
    public RemoteCommandsActivity(NodeManager nodeManager, FileCommanderManager fileCommanderManager) {
        this.nodeManager = nodeManager;
        this.fileCommanderManager = fileCommanderManager;
    }

    /**
     * Lists all registered nodes with their current status.
     * @return a list of node information maps
     */
    @Request("/list")
    public List<Map<String, Object>> listCommands() {
        NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();
        if (nodeRegistry == null) {
            // Direct mode might not have a registry, return self info
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(createNodeMap(nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId()), true));
            return result;
        }

        List<NodeInfo> nodeInfoList = nodeRegistry.getNodes();
        Map<String, String> pulses = nodeRegistry.getAllPulses();

        List<Map<String, Object>> result = new ArrayList<>(nodeInfoList.size());
        long now = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        for (NodeInfo info : nodeInfoList) {
            String nodeId = info.getNodeId();
            String pulseStr = pulses.get(nodeId);
            boolean live = false;
            if (pulseStr != null) {
                try {
                    long lastPulse = Long.parseLong(pulseStr);
                    live = (now - lastPulse <= timeout);
                } catch (NumberFormatException ignored) {
                }
            }
            result.add(createNodeMap(info, live));
        }
        return result;
    }

    private Map<String, Object> createNodeMap(NodeInfo info, boolean live) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", info.getNodeId());
        map.put("group", info.getGroup());
        map.put("title", info.getTitle());
        map.put("host", info.getHost());
        map.put("port", info.getPort());
        map.put("endpoint", info.getEndpointConfig());
        map.put("status", live ? "live" : "dead");
        return map;
    }

    /**
     * Creates a command to be sent to a specific node or all nodes.
     * @return a success message
     */
    @RequestToPost("/execute")
    public Map<String, String> executeCommand(@NonNull Translet translet) throws Exception {
        String targetNodeId = translet.getParameter("nodeId");
        String command = translet.getParameter("command");

        if (StringUtils.isEmpty(command)) {
            throw new IllegalArgumentException("Command is required");
        }
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = nodeManager.getNodeId();
        }

        fileCommanderManager.executeCommand(targetNodeId, command);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Command initiated successfully for node: " + targetNodeId);
        return result;
    }

}
