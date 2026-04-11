package com.recruitassist.service;

import com.recruitassist.config.AppPaths;
import com.recruitassist.config.AppServices;
import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegisterServiceTest {

    private static AppServices services;

    @BeforeAll
    static void initServices() throws IOException {
        System.setProperty(AppPaths.BASE_DIR_PROPERTY,
                System.getenv("RECRUITASSIST_BASE_DIR") != null
                        ? System.getenv("RECRUITASSIST_BASE_DIR")
                        : System.getProperty("user.dir") + "/../..");
        AppPaths.ensureBaseStructure();
        services = new AppServices();
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty(AppPaths.BASE_DIR_PROPERTY);
    }

    @Test
    @DisplayName("Register TA should persist to JSON file")
    void registerTaPersists() {
        String username = "test.register.ta." + System.currentTimeMillis();
        ActionResult result = services.userService().registerUser(
                username, "testpass123", "testpass123",
                "TA", "Test TA User", "test@example.com",
                services.idCounterRepository());

        assertTrue(result.isSuccess(), "Registration should succeed: " + result.getMessage());

        // Verify persisted to file
        Optional<UserProfile> loaded = services.userService().findByUsername(username);
        assertTrue(loaded.isPresent(), "Registered user should be found by username");

        UserProfile user = loaded.get();
        assertEquals(username, user.getUsername());
        assertEquals("TA", user.getRole().name());
        assertEquals("Test TA User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertFalse(user.getUserId().isBlank(), "Should have a generated userId");

        // Verify JSON file exists on disk
        Path jsonFile = AppPaths.usersDir().resolve(user.getUserId() + ".json");
        assertTrue(Files.exists(jsonFile), "JSON file should exist: " + jsonFile);

        // Cleanup
        try { Files.deleteIfExists(jsonFile); } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Register MO should persist to JSON file")
    void registerMoPersists() {
        String username = "test.register.mo." + System.currentTimeMillis();
        ActionResult result = services.userService().registerUser(
                username, "testpass123", "testpass123",
                "MO", "Test MO User", "mo@example.com",
                services.idCounterRepository());

        assertTrue(result.isSuccess(), "Registration should succeed: " + result.getMessage());

        Optional<UserProfile> loaded = services.userService().findByUsername(username);
        assertTrue(loaded.isPresent(), "Registered MO should be found");
        assertEquals("MO", loaded.get().getRole().name());

        // Cleanup
        Path jsonFile = AppPaths.usersDir().resolve(loaded.get().getUserId() + ".json");
        try { Files.deleteIfExists(jsonFile); } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Cannot register as ADMIN")
    void cannotRegisterAdmin() {
        String username = "test.admin." + System.currentTimeMillis();
        ActionResult result = services.userService().registerUser(
                username, "testpass123", "testpass123",
                "ADMIN", "Sneaky Admin", "",
                services.idCounterRepository());

        assertFalse(result.isSuccess(), "ADMIN registration should be blocked");
        assertTrue(result.getMessage().contains("Admin accounts cannot"),
                "Should explain that admin registration is not allowed");

        Optional<UserProfile> loaded = services.userService().findByUsername(username);
        assertTrue(loaded.isEmpty(), "ADMIN user should NOT be created");
    }

    @Test
    @DisplayName("Duplicate username should fail")
    void duplicateUsernameFails() {
        ActionResult result = services.userService().registerUser(
                "alice.ta", "testpass123", "testpass123",
                "TA", "Duplicate", "",
                services.idCounterRepository());
        assertFalse(result.isSuccess(), "Duplicate username should fail");
        assertTrue(result.getMessage().contains("already taken"));
    }

    @Test
    @DisplayName("Password mismatch should fail")
    void passwordMismatchFails() {
        ActionResult result = services.userService().registerUser(
                "mismatch.user", "pass123", "pass456",
                "TA", "Test", "",
                services.idCounterRepository());
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("do not match"));
    }

    @Test
    @DisplayName("Short password should fail")
    void shortPasswordFails() {
        ActionResult result = services.userService().registerUser(
                "short.pass", "abc", "abc",
                "TA", "Test", "",
                services.idCounterRepository());
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("6 characters"));
    }

    @Test
    @DisplayName("Can login with newly registered account")
    void canLoginAfterRegister() {
        String username = "test.login.after." + System.currentTimeMillis();
        String password = "mypassword123";

        services.userService().registerUser(
                username, password, password,
                "TA", "Login Test", "",
                services.idCounterRepository());

        Optional<UserProfile> auth = services.authService().authenticate(username, password);
        assertTrue(auth.isPresent(), "Should be able to login with newly registered account");
        assertEquals(username, auth.get().getUsername());

        // Cleanup
        Path jsonFile = AppPaths.usersDir().resolve(auth.get().getUserId() + ".json");
        try { Files.deleteIfExists(jsonFile); } catch (IOException ignored) {}
    }
}
