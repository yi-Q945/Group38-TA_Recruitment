package com.recruitassist.service;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.UserProfile;
import com.recruitassist.repository.UserRepository;
import com.recruitassist.util.JsonFileStore;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        System.setProperty(AppPaths.BASE_DIR_PROPERTY,
                System.getenv("RECRUITASSIST_BASE_DIR") != null
                        ? System.getenv("RECRUITASSIST_BASE_DIR")
                        : System.getProperty("user.dir") + "/../..");
        JsonFileStore store = new JsonFileStore();
        UserRepository userRepo = new UserRepository(store);
        UserService userService = new UserService(userRepo);
        authService = new AuthService(userService);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(AppPaths.BASE_DIR_PROPERTY);
    }

    @Test
    @DisplayName("Valid TA login should return user")
    void validTaLogin() {
        Optional<UserProfile> result = authService.authenticate("alice.ta", "demo123");
        assertTrue(result.isPresent(), "alice.ta should authenticate successfully");
        assertEquals("TA", result.get().getRole().name());
    }

    @Test
    @DisplayName("Valid MO login should return user")
    void validMoLogin() {
        Optional<UserProfile> result = authService.authenticate("recruiter.01", "demo123");
        assertTrue(result.isPresent(), "recruiter.01 should authenticate successfully");
        assertEquals("MO", result.get().getRole().name());
    }

    @Test
    @DisplayName("Valid Admin login should return user")
    void validAdminLogin() {
        Optional<UserProfile> result = authService.authenticate("admin.sarah", "demo123");
        assertTrue(result.isPresent(), "admin.sarah should authenticate successfully");
        assertEquals("ADMIN", result.get().getRole().name());
    }

    @Test
    @DisplayName("Wrong password should return empty")
    void wrongPassword() {
        Optional<UserProfile> result = authService.authenticate("alice.ta", "wrongpass");
        assertTrue(result.isEmpty(), "Wrong password should fail");
    }

    @Test
    @DisplayName("Non-existent user should return empty")
    void nonExistentUser() {
        Optional<UserProfile> result = authService.authenticate("nobody", "demo123");
        assertTrue(result.isEmpty(), "Non-existent user should fail");
    }

    @Test
    @DisplayName("Null/blank input should return empty")
    void nullBlankInput() {
        assertTrue(authService.authenticate(null, "demo123").isEmpty());
        assertTrue(authService.authenticate("", "demo123").isEmpty());
        assertTrue(authService.authenticate("alice.ta", null).isEmpty());
        assertTrue(authService.authenticate("alice.ta", "").isEmpty());
        assertTrue(authService.authenticate("  ", "  ").isEmpty());
    }
}
