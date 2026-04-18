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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * NodeConfig defines the configuration for an Aspectow node,
 * including its identity, group, and communication settings.
 *
 * <p>Created: 2026-04-16</p>
 */
public class NodeConfig extends DefaultParameters {

    private static final ParameterKey cluster;
    private static final ParameterKey node;

    private static final ParameterKey[] parameterKeys;

    static {
        cluster = new ParameterKey("cluster", ClusterConfig.class);
        node = new ParameterKey("node", NodeInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                cluster,
                node
        };
    }

    public NodeConfig() {
        super(parameterKeys);
    }

    public NodeConfig(Reader reader) throws IOException {
        this();
        readFrom(reader);
    }

    public NodeConfig(File configFile) throws IOException {
        this();
        readFrom(configFile);
    }

    public ClusterConfig getClusterConfig() {
        return getParameters(cluster);
    }

    public void setClusterConfig(ClusterConfig clusterConfig) {
        putValue(cluster, clusterConfig);
    }

    public List<NodeInfo> getNodeInfoList() {
        return getParametersList(node);
    }

    public void putNodeInfo(NodeInfo nodeInfo) {
        putValue(node, nodeInfo);
    }

}
