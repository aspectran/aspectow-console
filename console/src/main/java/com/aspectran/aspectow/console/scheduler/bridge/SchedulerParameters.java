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
package com.aspectran.aspectow.console.scheduler.bridge;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Represents a structured message for scheduler management commands.
 */
public class SchedulerParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey command;
    public static final ParameterKey timeZone;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        command = new ParameterKey("command", ValueType.STRING);
        timeZone = new ParameterKey("timeZone", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                header,
                targetNodeId,
                command,
                timeZone
        };
    }

    public SchedulerParameters() {
        super(parameterKeys);
    }

    public String getHeader() {
        return getString(header);
    }

    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    public String getCommand() {
        return getString(command);
    }

}
