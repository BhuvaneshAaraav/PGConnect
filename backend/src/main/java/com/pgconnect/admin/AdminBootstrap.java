package com.pgconnect.admin;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserAccountRepository users, PasswordEncoder encoder,
                          @Value("${PGCONNECT_ADMIN_EMAIL:}") String email,
                          @Value("${PGCONNECT_ADMIN_PASSWORD:}") String password) {
        this.users = users; this.encoder = encoder; this.email = email; this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new IllegalStateException("Set both PGCONNECT_ADMIN_EMAIL and a PGCONNECT_ADMIN_PASSWORD of at least 16 UTF-8 bytes");
        }
        String normalized = email.trim().toLowerCase();
        if (users.findByEmailNormalized(normalized).isPresent()) {
            log.info("Configured administrator already exists; bootstrap will not overwrite its account");
            return;
        }
        UserAccount admin = new UserAccount("Platform administrator", normalized, encoder.encode(password), Role.ADMIN);
        admin.setEnabled(true);
        users.save(admin);
        log.info("Provisioned the first platform administrator from environment configuration");
    }
}
