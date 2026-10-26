package com.pgconnect.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
public class GoogleLoginService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final JwtDecoder decoder;
    private final String clientId;
    private final SecureRandom random = new SecureRandom();

    public GoogleLoginService(UserAccountRepository users, PasswordEncoder passwords, JwtDecoder decoder,
                              @Value("${pgconnect.google.client-id:}") String clientId) {
        this.users = users;
        this.passwords = passwords;
        this.decoder = decoder;
        this.clientId = clientId;
    }

    public UserAccount resolveAccount(String credential, Role requestedRole) {
        if (clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google sign-in is not configured. Set GOOGLE_OAUTH_CLIENT_ID on the API and VITE_GOOGLE_CLIENT_ID on the frontend.");
        }
        Jwt claims;
        try {
            claims = decoder.decode(credential);
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidCredential();
        }

        String subject = claims.getSubject();
        String email = normalize(claims.getClaimAsString("email"));
        if (subject == null || subject.isBlank() || email.isBlank()
                || !Boolean.TRUE.equals(claims.getClaimAsBoolean("email_verified"))
                || email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw invalidCredential();
        }

        UserAccount account = users.findByGoogleSubject(subject).orElse(null);
        if (account != null) return requireEnabled(account);

        account = users.findByEmailNormalized(email).orElse(null);
        if (account != null) {
            if (account.getGoogleSubject() != null && !account.getGoogleSubject().equals(subject)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is linked to another Google account");
            }
            account.setGoogleSubject(subject);
            account.setEmailVerified(true);
            try {
                return requireEnabled(users.save(account));
            } catch (DuplicateKeyException race) {
                return users.findByGoogleSubject(subject).map(this::requireEnabled)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "This email is linked to another account"));
            }
        }

        if (requestedRole == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No PGConnect account uses this Google email yet. Choose Create an account first.");
        }
        if (requestedRole == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrator accounts are provisioned separately");
        }

        String displayName = safeName(claims.getClaimAsString("name"), email);
        byte[] secret = new byte[48];
        random.nextBytes(secret);
        UserAccount created = new UserAccount(displayName, email,
                passwords.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(secret)), requestedRole);
        created.setGoogleSubject(subject);
        created.setEmailVerified(true);
        try {
            return users.save(created);
        } catch (DuplicateKeyException race) {
            return users.findByGoogleSubject(subject).map(this::requireEnabled)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists"));
        }
    }

    private UserAccount requireEnabled(UserAccount account) {
        if (!account.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled");
        return account;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeName(String name, String email) {
        String value = name == null ? "" : name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (value.isBlank()) value = email.substring(0, email.indexOf('@'));
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private static ResponseStatusException invalidCredential() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google sign-in could not verify this account");
    }
}
