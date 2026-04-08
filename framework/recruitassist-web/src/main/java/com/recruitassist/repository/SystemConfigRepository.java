package com.recruitassist.repository;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.SystemConfig;
import com.recruitassist.util.JsonFileStore;

public class SystemConfigRepository {
    private final JsonFileStore jsonFileStore;

    public SystemConfigRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public SystemConfig load() {
        SystemConfig config = jsonFileStore.read(AppPaths.configFile(), SystemConfig.class);
        return config == null ? new SystemConfig() : config;
    }
}
