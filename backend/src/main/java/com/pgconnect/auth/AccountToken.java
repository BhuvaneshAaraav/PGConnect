package com.pgconnect.auth;
import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.index.CompoundIndex;import org.springframework.data.mongodb.core.index.Indexed;import org.springframework.data.mongodb.core.mapping.Document;import java.time.Instant;
@Document("account_tokens") @CompoundIndex(name="account_token_active",def="{'userId':1,'purpose':1,'usedAt':1,'createdAt':-1}")
public class AccountToken{
 @Id private String id;private String userId;private AccountTokenPurpose purpose;@Indexed(unique=true)private String tokenHash;@Indexed(expireAfter="0s")private Instant expiresAt;private Instant createdAt=Instant.now();private Instant usedAt;
 public String getId(){return id;}public String getUserId(){return userId;}public AccountTokenPurpose getPurpose(){return purpose;}public String getTokenHash(){return tokenHash;}public Instant getExpiresAt(){return expiresAt;}public Instant getCreatedAt(){return createdAt;}public Instant getUsedAt(){return usedAt;}
 public void setUserId(String v){userId=v;}public void setPurpose(AccountTokenPurpose v){purpose=v;}public void setTokenHash(String v){tokenHash=v;}public void setExpiresAt(Instant v){expiresAt=v;}public void setUsedAt(Instant v){usedAt=v;}
}
