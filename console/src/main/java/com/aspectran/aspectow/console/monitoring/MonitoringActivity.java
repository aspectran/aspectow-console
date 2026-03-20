/*
 * Copyright (c) 2008-present The Aspectran Project
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
package com.aspectran.aspectow.console.monitoring;

import com.aspectran.aspectow.appmon.common.auth.AppMonCookieIssuer;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;

import java.util.Map;

/**
 * A controller that provides the monitoring dashboard via iframe.
 */
@Component("/monitoring")
@Bean("monitoringActivity")
public class MonitoringActivity {

    public MonitoringActivity() {
    }

    @Request("/")
    @Dispatch("monitoring/viewer")
    @Action("page")
    public Map<String, Object> viewer(Translet translet) {
        AppMonCookieIssuer appMonCookieIssuer = new AppMonCookieIssuer();
        appMonCookieIssuer.issueCookie(translet, "/appmon", 3600);
        return Map.of(
                "title", "Monitoring - Aspectow Console",
                "headline", "System Monitoring",
                "style", "monitoring-page",
                "appmonUrl", "/appmon/dashboard/"
        );
    }

}
