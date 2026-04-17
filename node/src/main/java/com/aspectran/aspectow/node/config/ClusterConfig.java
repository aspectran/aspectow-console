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
import com.aspectran.utils.apon.ValueType;

/**
 * ClusterConfig defines cluster-wide settings, including the cluster name
 * and PBE (Password-Based Encryption) configuration for secure communication.
 *
 * <p>Created: 2026-04-16</p>
 */
public class ClusterConfig extends DefaultParameters {

    private static final ParameterKey name;
    private static final ParameterKey mode;
    private static final ParameterKey pbe;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        mode = new ParameterKey("mode", ValueType.STRING);
        pbe = new ParameterKey("pbe", PbeConfig.class);

        parameterKeys = new ParameterKey[] {
                name,
                mode,
                pbe
        };
    }

    public ClusterConfig() {
        super(parameterKeys);
    }

    public String getName() {
        return getString(name);
    }

    public void setName(String name) {
        putValue(ClusterConfig.name, name);
    }

    public String getMode() {
        return getString(mode);
    }

    public void setMode(String mode) {
        putValue(ClusterConfig.mode, mode);
    }

    public boolean isDirectMode() {
        return "direct".equals(getString(mode));
    }

    public boolean isGatewayMode() {
        return "gateway".equals(getString(mode));
    }

    public boolean isAutoscalingMode() {
        return "autoscaling".equals(getString(mode));
    }

    public PbeConfig getPbeConfig() {
        return getParameters(pbe);
    }

}
