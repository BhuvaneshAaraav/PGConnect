package com.pgconnect.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("users")
@CompoundIndex(name = "role_subscription", def = "{'role': 1, 'subscriptionStatus': 1}")
public class UserAccount {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String emailNormalized;
    @Indexed(unique = true, partialFilter = "{'googleSubject': {'$type': 'string'}}")
    private String googleSubject;
    private String passwordHash;
    private String phone;
    private boolean emailVerified;
    private Role role;
    private SubscriptionStatus subscriptionStatus;
    private boolean enabled = true;
    private Instant createdAt = Instant.now();

    public UserAccount() {}

    public UserAccount(String name, String emailNormalized, String passwordHash, Role role) {
        this.name = name;
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.role = role;
        this.subscriptionStatus = role == Role.OWNER ? SubscriptionStatus.PENDING : null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getGoogleSubject() { return googleSubject; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public boolean isEmailVerified() { return emailVerified; }
    public Role getRole() { return role; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmailNormalized(String emailNormalized) { this.emailNormalized = emailNormalized; }
    public void setGoogleSubject(String googleSubject) { this.googleSubject = googleSubject; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setRole(Role role) { this.role = role; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
