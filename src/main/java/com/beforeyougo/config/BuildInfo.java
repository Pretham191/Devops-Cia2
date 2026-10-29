package com.beforeyougo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Exposes the build tag (env APP_VERSION) to every template as ${@buildInfo.version}. */
@Component("buildInfo")
public class BuildInfo {

    private final String version;

    public BuildInfo(@Value("${app.version:local-dev}") String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
