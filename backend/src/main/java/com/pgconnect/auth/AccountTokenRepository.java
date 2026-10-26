package com.pgconnect.auth;
import org.springframework.data.mongodb.repository.MongoRepository;import java.time.Instant;import java.util.Optional;
public interface AccountTokenRepository extends MongoRepository<AccountToken,String>{Optional<AccountToken> findByTokenHashAndPurposeAndUsedAtIsNullAndExpiresAtAfter(String tokenHash,AccountTokenPurpose purpose,Instant now);void deleteByUserIdAndPurposeAndUsedAtIsNull(String userId,AccountTokenPurpose purpose);}
