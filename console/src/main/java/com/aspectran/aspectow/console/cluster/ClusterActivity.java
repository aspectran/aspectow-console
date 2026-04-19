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
package com.aspectran.aspectow.console.cluster;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.node.manager.NodeRegistryProtocol.NODES_BASE_PATH;

/**
 * ClusterActivity provides views and data for monitoring and managing cluster nodes.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component(NODES_BASE_PATH)
public class ClusterActivity {

    private final NodeManager nodeManager;

    @Autowired
    public ClusterActivity(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    /**
     * Displays the cluster nodes list page.
     * @return a map of attributes for rendering the view
     */
    @Request("/list")
    @Dispatch("nodes/list")
    @Action("page")
    public Map<String, Object> listNodes() {
        List<Map<String, Object>> nodes = getNodes();
        return Map.of(
                "title", "Cluster Nodes",
                "include", "nodes/list",
                "style", "cluster-page",
                "nodes", nodes
        );
    }

    private List<Map<String, Object>> getNodes() {
        NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();
        if (nodeManager.getClusterConfig().isDirectMode() || nodeRegistry == null) {
            List<NodeInfo> nodeInfoList = nodeManager.getNodeInfoHolder().getNodeInfoList();
            List<Map<String, Object>> result = new ArrayList<>(nodeInfoList.size());
            for (NodeInfo info : nodeInfoList) {
                boolean alive = info.getNodeId().equals(nodeManager.getNodeId());
                result.add(createNodeMap(info, alive));
            }
            return result;
        }

        List<NodeInfo> nodeInfoList = nodeRegistry.getNodes();
        Map<String, String> pulses = nodeRegistry.getAllPulses();
        List<Map<String, Object>> result = new ArrayList<>(nodeInfoList.size());
        long now = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        for (NodeInfo info : nodeInfoList) {
            String nodeId = info.getNodeId();
            boolean alive = false;
            String pulseStr = (pulses != null ? pulses.get(nodeId) : null);
            if (pulseStr != null) {
                try {
                    long lastPulse = Long.parseLong(pulseStr);
                    alive = (now - lastPulse <= timeout);
                } catch (NumberFormatException ignored) {
                }
            }
            result.add(createNodeMap(info, alive));
        }
        return result;
    }

    private Map<String, Object> createNodeMap(NodeInfo info, boolean alive) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", info.getNodeId());
        map.put("group", info.getGroup());
        map.put("title", info.getTitle());
        map.put("host", info.getHost());
        map.put("port", info.getPort());

        String status = info.getStatus();
        if (!alive) {
            status = "dead";
        } else if (status == null) {
            status = "live";
        }
        map.put("status", status);
        return map;
    }

}
