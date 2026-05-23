package com.recruitassist.service;

import com.recruitassist.model.UserProfile;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(5);

    private final UserService userService;
    private final Clock clock;
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public AuthService(UserService userService) {
        this(userService, Clock.systemUTC());
    }

    AuthService(UserService userService, Clock clock) {
        this.userService = userService;
        this.clock = clock;
    }

    public Optional<UserProfile> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim();
        String normalizedPassword = password.trim();
        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            return Optional.empty();
        }

        if (isTemporarilyLocked(normalizedUsername)) {
            return Optional.empty();
        }

        Optional<UserProfile> authenticated = userService.findByUsername(normalizedUsername)
                .filter(user -> PasswordHasher.verify(normalizedPassword, user.getPassword()));
        if (authenticated.isPresent()) {
            loginAttempts.remove(normalizedUsername.toLowerCase());
            return authenticated;
        }

        recordFailure(normalizedUsername);
        return Optional.empty();
    }

    public boolean isTemporarilyLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        LoginAttempt attempt = loginAttempts.get(username.trim().toLowerCase());
        if (attempt == null || attempt.failedCount < MAX_FAILED_ATTEMPTS) {
            return false;
        }
        Instant lockExpiresAt = attempt.lastFailedAt.plus(LOCKOUT_DURATION);
        boolean locked = clock.instant().isBefore(lockExpiresAt);
        if (!locked) {
            loginAttempts.remove(username.trim().toLowerCase());
        }
        return locked;
    }

    void clearLoginAttemptsForTesting(String username) {
        if (username != null) {
            loginAttempts.remove(username.trim().toLowerCase());
        }
    }

    private void recordFailure(String username) {
        String key = username.toLowerCase();
        loginAttempts.compute(key, (unused, current) -> {
            int failedCount = current == null ? 1 : current.failedCount + 1;
            return new LoginAttempt(failedCount, clock.instant());
        });
    }

    private record LoginAttempt(int failedCount, Instant lastFailedAt) {
    }
}
