package com.recruitassist.repository;

import com.recruitassist.config.AppPaths;
import com.recruitassist.util.JsonFileStore;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuditRepository {
    private final JsonFileStore jsonFileStore;

    public AuditRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public void append(String actor, String action, String target, String result, String details) {
        String line = Stream.of(
                        Instant.now().toString(),
                        actor,
                        action,
                        target,
                        result,
                        details)
                .map(this::csvField)
                .collect(Collectors.joining(",")) + System.lineSeparator();

        jsonFileStore.appendLine(AppPaths.auditLogFile(), line);
    }

    private String csvField(String value) {
        String safeValue = value == null ? "" : value.replace("\"", "\"\"");
        return '"' + safeValue + '"';
    }
}
