package com.recruitassist.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class AppPaths {
    public static final String BASE_DIR_PROPERTY = "recruitassist.baseDir";
    public static final String BASE_DIR_ENV = "RECRUITASSIST_BASE_DIR";

    private AppPaths() {
    }

    public static Path resolveBaseDir() {
        String configured = System.getProperty(BASE_DIR_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(BASE_DIR_ENV);
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }

        Path workingDir = Paths.get("").toAbsolutePath().normalize();
        return normalizeWorkingDir(workingDir);
    }

    private static Path normalizeWorkingDir(Path workingDir) {
        Path fileName = workingDir.getFileName();
        Path parent = workingDir.getParent();
        if (fileName != null && parent != null
                && "recruitassist-web".equals(fileName.toString())
                && parent.getFileName() != null
                && "framework".equals(parent.getFileName().toString())) {
            Path workspace = parent.getParent();
            if (workspace != null) {
                return workspace;
            }
        }
        return workingDir;
    }

    public static Path frameworkDir() {
        return resolveBaseDir().resolve("framework").resolve("recruitassist-web");
    }

    public static Path dataDir() {
        return resolveBaseDir().resolve("data");
    }

    public static Path usersDir() {
        return dataDir().resolve("users");
    }

    public static Path jobsDir() {
        return dataDir().resolve("jobs");
    }

    public static Path applicationsDir() {
        return dataDir().resolve("applications");
    }

    public static Path systemDir() {
        return dataDir().resolve("system");
    }

    public static Path cvDir() {
        return dataDir().resolve("cv");
    }

    public static Path logsDir() {
        return resolveBaseDir().resolve("logs");
    }

    public static Path appLogsDir() {
        return logsDir().resolve("app");
    }

    public static Path buildLogsDir() {
        return logsDir().resolve("build");
    }

    public static Path accessLogsDir() {
        return logsDir().resolve("access");
    }

    public static Path configFile() {
        return systemDir().resolve("config.json");
    }

    public static Path idCountersFile() {
        return systemDir().resolve("id-counters.json");
    }

    public static Path auditLogFile() {
        return accessLogsDir().resolve("audit.csv");
    }

    public static List<Path> requiredDirectories() {
        return List.of(
                frameworkDir(),
                usersDir(),
                jobsDir(),
                applicationsDir(),
                systemDir(),
                cvDir(),
                appLogsDir(),
                buildLogsDir(),
                accessLogsDir()
        );
    }

    public static void ensureBaseStructure() throws IOException {
        for (Path path : requiredDirectories()) {
            Files.createDirectories(path);
        }
    }
}
