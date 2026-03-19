package com.aspectran.aspectow.console.home;

import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;

import java.util.Map;

@Component("/")
public class HomeActivity {

    @Request("/")
    @Dispatch("home/home")
    @Action("page")
    public Map<String, String> home() {
        return Map.of(
                "title", "Aspectow Console",
                "headline", "Aspectow Console Management",
                "include", "home",
                "style", "dashboard-page"
                );
    }

}
