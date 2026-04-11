package com.recruitassist.repository;

import com.google.gson.reflect.TypeToken;
import com.recruitassist.config.AppPaths;
import com.recruitassist.util.JsonFileStore;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class IdCounterRepository {
    private static final Type COUNTER_TYPE = new TypeToken<Map<String, Long>>() { } .getType();

    private final JsonFileStore jsonFileStore;
    private final ReentrantLock lock = new ReentrantLock();

    public IdCounterRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public String nextJobId() {
        return next("job", "J");
    }

    public String nextApplicationId() {
        return next("application", "A");
    }

    public String nextUserId() {
        return next("user", "U");
    }

    private String next(String key, String prefix) {
        lock.lock();
        try {
            Map<String, Long> counters = jsonFileStore.read(AppPaths.idCountersFile(), COUNTER_TYPE);
            if (counters == null) {
                counters = new LinkedHashMap<>();
            }

            long nextValue = counters.getOrDefault(key, defaultSeed(key)) + 1;
            counters.put(key, nextValue);
            jsonFileStore.write(AppPaths.idCountersFile(), counters);
            return prefix + nextValue;
        } finally {
            lock.unlock();
        }
    }

    private long defaultSeed(String key) {
        return switch (key) {
            case "user" -> 1000L;
            case "job" -> 2000L;
            case "application" -> 3000L;
            default -> 0L;
        };
    }
}
