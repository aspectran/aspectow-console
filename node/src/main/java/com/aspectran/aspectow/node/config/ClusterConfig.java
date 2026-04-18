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

    private static final ParameterKey id;
    private static final ParameterKey mode;
    private static final ParameterKey secret;
    private static final ParameterKey heartbeatInterval;
    private static final ParameterKey endpoint;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        mode = new ParameterKey("mode", ValueType.STRING);
        secret = new ParameterKey("secret", SecretConfig.class);
        heartbeatInterval = new ParameterKey("heartbeatInterval", ValueType.LONG);
        endpoint = new ParameterKey("endpoint", EndpointConfig.class);

        parameterKeys = new ParameterKey[] {
                id,
                mode,
                secret,
                heartbeatInterval,
                endpoint
        };
    }

    public ClusterConfig() {
        super(parameterKeys);
    }

    public String getId() {
        return getString(id);
    }

    public void setId(String id) {
        putValue(ClusterConfig.id, id);
    }

    public String getMode() {
        return getString(mode);
    }

    public void setMode(String mode) {
        putValue(ClusterConfig.mode, mode);
    }

    public boolean isDirectMode() {
        return (!isGatewayMode() && !isAutoscalingMode());
    }

    public boolean isGatewayMode() {
        return "gateway".equals(getString(mode));
    }

    public boolean isAutoscalingMode() {
        return "autoscaling".equals(getString(mode));
    }

    public SecretConfig getSecretConfig() {
        return getParameters(secret);
    }

    public long getHeartbeatInterval() {
        return getLong(heartbeatInterval);
    }

    public long getHeartbeatInterval(long defaultValue) {
        return getLong(heartbeatInterval, defaultValue);
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        putValue(ClusterConfig.heartbeatInterval, heartbeatInterval);
    }

    public EndpointConfig getEndpointConfig() {
        return getParameters(endpoint);
    }

    public EndpointConfig touchEndpointConfig() {
        return touchParameters(endpoint);
    }

    public void setEndpointConfig(EndpointConfig endpointConfig) {
        putValue(endpoint, endpointConfig);
    }

}
