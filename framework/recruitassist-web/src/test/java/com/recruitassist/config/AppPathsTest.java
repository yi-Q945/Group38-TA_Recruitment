package com.recruitassist.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppPathsTest {
    @AfterEach
    void tearDown() {
        System.clearProperty(AppPaths.BASE_DIR_PROPERTY);
    }

    @Test
    void shouldUseConfiguredBaseDirectory() {
        System.setProperty(AppPaths.BASE_DIR_PROPERTY, "/tmp/recruitassist-test");

        Path baseDir = AppPaths.resolveBaseDir();

        assertEquals(Path.of("/tmp/recruitassist-test"), baseDir);
        assertEquals(Path.of("/tmp/recruitassist-test/framework/recruitassist-web"), AppPaths.frameworkDir());
        assertEquals(Path.of("/tmp/recruitassist-test/data/users"), AppPaths.usersDir());
        assertEquals(Path.of("/tmp/recruitassist-test/logs/access/audit.csv"), AppPaths.auditLogFile());
    }
}
