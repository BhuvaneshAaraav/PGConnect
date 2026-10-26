package com.pgconnect.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleLoginServiceTest {
    @Test
    void createsAnOwnerOnlyFromVerifiedGoogleEmail() {
        AtomicReference<UserAccount> stored = new AtomicReference<>();
        UserAccountRepository users = repository((object, method, args) -> switch (method.getName()) {
            case "findByGoogleSubject", "findByEmailNormalized" -> Optional.empty();
            case "save" -> { UserAccount account = (UserAccount) args[0]; account.setId("owner-id"); stored.set(account); yield account; }
            default -> null;
        });
        GoogleLoginService service = service(users, "valid-id-token", verifiedToken("google-sub", "OWNER@example.com", true));

        UserAccount account = service.resolveAccount("valid-id-token", Role.OWNER);

        assertEquals("owner-id", account.getId());
        assertEquals(Role.OWNER, account.getRole());
        assertEquals("owner@example.com", account.getEmailNormalized());
        assertEquals("google-sub", account.getGoogleSubject());
        assertTrue(account.isEmailVerified());
        assertTrue(account.getPasswordHash().startsWith("encoded:"));
        assertEquals(account, stored.get());
    }

    @Test
    void linksVerifiedEmailWithoutChangingExistingAccountRole() {
        UserAccount resident = new UserAccount("Resident", "resident@example.com", "existing-password-hash", Role.RESIDENT);
        resident.setId("resident-id");
        AtomicReference<UserAccount> stored = new AtomicReference<>();
        UserAccountRepository users = repository((object, method, args) -> switch (method.getName()) {
            case "findByGoogleSubject" -> Optional.empty();
            case "findByEmailNormalized" -> Optional.of(resident);
            case "save" -> { stored.set((UserAccount) args[0]); yield args[0]; }
            default -> null;
        });
        GoogleLoginService service = service(users, "valid-id-token", verifiedToken("google-sub", "resident@example.com", true));

        UserAccount linked = service.resolveAccount("valid-id-token", Role.OWNER);

        assertEquals(Role.RESIDENT, linked.getRole());
        assertEquals("google-sub", linked.getGoogleSubject());
        assertTrue(linked.isEmailVerified());
        assertEquals(linked, stored.get());
    }

    @Test
    void refusesUnverifiedGoogleEmailAndDoesNotCreateAnAccount() {
        AtomicReference<UserAccount> stored = new AtomicReference<>();
        UserAccountRepository users = repository((object, method, args) -> {
            if (method.getName().equals("save")) stored.set((UserAccount) args[0]);
            return method.getName().startsWith("findBy") ? Optional.empty() : null;
        });
        GoogleLoginService service = service(users, "unverified-id-token", verifiedToken("google-sub", "person@example.com", false));

        assertThrows(ResponseStatusException.class, () -> service.resolveAccount("unverified-id-token", Role.RESIDENT));

        assertEquals(null, stored.get());
    }

    @Test
    void reportsMissingClientConfigurationWithoutAttemptingTokenVerification() {
        AtomicBoolean decoderCalled = new AtomicBoolean();
        JwtDecoder decoder = token -> { decoderCalled.set(true); return verifiedToken("sub", "user@example.com", true); };
        PasswordEncoder passwords = new PasswordEncoder() {
            @Override public String encode(CharSequence rawPassword) { return "encoded:" + rawPassword; }
            @Override public boolean matches(CharSequence rawPassword, String encodedPassword) { return encodedPassword.equals("encoded:" + rawPassword); }
        };
        GoogleLoginService service = new GoogleLoginService(repository((object, method, args) -> null), passwords, decoder, "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.resolveAccount("valid-id-token", Role.RESIDENT));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertFalse(decoderCalled.get());
    }

    private static GoogleLoginService service(UserAccountRepository users, String token, Jwt jwt) {
        JwtDecoder decoder = supplied -> {
            if (!token.equals(supplied)) throw new IllegalArgumentException("Unexpected token");
            return jwt;
        };
        PasswordEncoder passwords = new PasswordEncoder() {
            @Override public String encode(CharSequence rawPassword) { return "encoded:" + rawPassword; }
            @Override public boolean matches(CharSequence rawPassword, String encodedPassword) { return encodedPassword.equals("encoded:" + rawPassword); }
        };
        return new GoogleLoginService(users, passwords, decoder, "web-client.apps.googleusercontent.com");
    }

    private static UserAccountRepository repository(InvocationHandler handler) {
        return (UserAccountRepository) Proxy.newProxyInstance(UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class}, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "Google login repository test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(proxy, method, args);
                });
    }

    private static Jwt verifiedToken(String subject, String email, boolean emailVerified) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .subject(subject)
                .audience(List.of("web-client.apps.googleusercontent.com"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("name", "Google User")
                .build();
    }
}
