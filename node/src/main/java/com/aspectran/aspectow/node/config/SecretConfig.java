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
 * SecretConfig defines the settings for secure communication within the cluster,
 * including the shared password, encryption algorithm, and salt.
 *
 * <p>Created: 2026-04-18</p>
 */
public class SecretConfig extends DefaultParameters {

    private static final ParameterKey password;
    private static final ParameterKey algorithm;
    private static final ParameterKey salt;

    private static final ParameterKey[] parameterKeys;

    static {
        password = new ParameterKey("password", ValueType.STRING);
        algorithm = new ParameterKey("algorithm", ValueType.STRING);
        salt = new ParameterKey("salt", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                password,
                algorithm,
                salt
        };
    }

    public SecretConfig() {
        super(parameterKeys);
    }

    public String getPassword() {
        return getString(password);
    }

    public void setPassword(String password) {
        putValue(SecretConfig.password, password);
    }

    public String getAlgorithm() {
        return getString(algorithm);
    }

    public void setAlgorithm(String algorithm) {
        putValue(SecretConfig.algorithm, algorithm);
    }

    public String getSalt() {
        return getString(salt);
    }

    public void setSalt(String salt) {
        putValue(SecretConfig.salt, salt);
    }

}
