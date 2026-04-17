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

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NodeManagerActivity provides REST API endpoints for managing cluster nodes.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component("/commands")
public class NodeManagerActivity {

    private final NodeRegistry nodeRegistry;

    private final NodeCommandBridge nodeCommandBridge;

    @Autowired
    public NodeManagerActivity(NodeRegistry nodeRegistry, NodeCommandBridge nodeCommandBridge) {
        this.nodeRegistry = nodeRegistry;
        this.nodeCommandBridge = nodeCommandBridge;
    }

    /**
     * Lists all registered nodes with their current status.
     * @return a list of node information maps
     */
    @Request("/list")
    public List<Map<String, Object>> listNodes() {
        List<NodeInfo> nodeInfoList = nodeRegistry.getNodes();
        Map<String, String> pulses = nodeRegistry.getAllPulses();
        
        List<Map<String, Object>> result = new ArrayList<>(nodeInfoList.size());
        long now = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        for (NodeInfo info : nodeInfoList) {
            Map<String, Object> map = new HashMap<>();
            String nodeId = info.getName();
            map.put("id", nodeId);
            map.put("group", info.getGroup());
            map.put("title", info.getTitle());
            map.put("endpoints", info.getEndpoints());
            
            String pulseStr = pulses.get(nodeId);
            boolean live = false;
            if (pulseStr != null) {
                long lastPulse = Long.parseLong(pulseStr);
                live = (now - lastPulse <= timeout);
                map.put("lastPulse", lastPulse);
            }
            map.put("status", live ? "live" : "dead");
            
            result.add(map);
        }
        return result;
    }

    /**
     * Sends a command to a specific node or all nodes.
     * @return a success message
     */
    @Request("/command")
    public Map<String, String> sendCommand(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        String command = translet.getParameter("command");
        
        if (StringUtils.isEmpty(command)) {
            throw new IllegalArgumentException("Command is required");
        }

        if (StringUtils.hasText(nodeId) && !"all".equalsIgnoreCase(nodeId)) {
            nodeCommandBridge.sendCommand(nodeId, command);
        } else {
            nodeCommandBridge.broadcastCommand(command);
        }

        Map<String, String> result = new HashMap<>();
        result.put("message", "Command sent successfully to: " + (nodeId != null ? nodeId : "all"));
        return result;
    }

}
