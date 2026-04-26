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
package com.aspectran.aspectow.console.scheduler;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;

import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * SchedulerActivity provides views and data for managing schedulers across cluster nodes.
 *
 * <p>Created: 2026-04-26</p>
 */
@Component(NODES_BASE_PATH + "/scheduler")
public class SchedulerActivity {

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public SchedulerActivity(NodeManager nodeManager, NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the scheduler management page.
     * @return a map of attributes for rendering the view
     */
    @Request("/")
    @Dispatch("nodes/scheduler")
    @Action("page")
    public Map<String, Object> scheduler() {
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(false);
        NodeInfo nodeInfo = nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId());
        return Map.of(
                "title", "Scheduler Management",
                "style", "cluster-page",
                "nodes", nodes,
                "node", nodeConsoleHelper.createNodeMap(nodeInfo, true, true),
                "token", AppMonTokenIssuer.issueToken(30)
        );
    }

}
