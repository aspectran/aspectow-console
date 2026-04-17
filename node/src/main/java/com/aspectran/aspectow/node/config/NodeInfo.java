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
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.ValueType;

/**
 * NodeInfo defines the properties of a single node in the cluster.
 *
 * <p>Created: 2026-04-16</p>
 */
public class NodeInfo extends DefaultParameters {

    private static final ParameterKey name;
    private static final ParameterKey group;
    private static final ParameterKey title;
    private static final ParameterKey host;
    private static final ParameterKey port;
    private static final ParameterKey heartbeatInterval;
    private static final ParameterKey endpoints;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        group = new ParameterKey("group", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        host = new ParameterKey("host", ValueType.STRING);
        port = new ParameterKey("port", ValueType.INT);
        heartbeatInterval = new ParameterKey("heartbeatInterval", ValueType.LONG);
        endpoints = new ParameterKey("endpoints", ValueType.VARIABLE); // For flexible map-like structure

        parameterKeys = new ParameterKey[] {
                name,
                group,
                title,
                host,
                port,
                heartbeatInterval,
                endpoints
        };
    }

    public NodeInfo() {
        super(parameterKeys);
    }

    public String getName() {
        return getString(name);
    }

    public void setName(String name) {
        putValue(NodeInfo.name, name);
    }

    public String getGroup() {
        return getString(group);
    }

    public String getTitle() {
        return getString(title);
    }

    public long getHeartbeatInterval(long defaultValue) {
        return getLong(heartbeatInterval, defaultValue);
    }

    public Parameters getEndpoints() {
        return getParameters(endpoints);
    }

}
