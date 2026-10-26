package com.pgconnect.auth;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByEmailNormalized(String emailNormalized);
    Optional<UserAccount> findByGoogleSubject(String googleSubject);
    List<UserAccount> findByRoleAndSubscriptionStatus(Role role, SubscriptionStatus status);
    long countByRole(Role role);
    long countByRoleAndSubscriptionStatus(Role role, SubscriptionStatus status);
}
