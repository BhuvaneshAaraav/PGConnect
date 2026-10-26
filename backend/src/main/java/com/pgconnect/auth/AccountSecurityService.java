package com.pgconnect.auth;
import jakarta.validation.constraints.Email;import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.http.HttpStatus;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.web.server.ResponseStatusException;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.security.SecureRandom;import java.time.Instant;import java.util.Base64;import java.util.HexFormat;
@Service
public class AccountSecurityService{
 private static final Logger log=LoggerFactory.getLogger(AccountSecurityService.class);
 private final UserAccountRepository users;private final AccountTokenRepository tokens;private final EmailDelivery email;private final PasswordEncoder passwords;private final SecureRandom random=new SecureRandom();
 public AccountSecurityService(UserAccountRepository users,AccountTokenRepository tokens,EmailDelivery email,PasswordEncoder passwords){this.users=users;this.tokens=tokens;this.email=email;this.passwords=passwords;}
 public void sendVerificationForNewAccount(UserAccount account){if(email.configured()&&!account.isEmailVerified())try{issue(account,AccountTokenPurpose.VERIFY_EMAIL);}catch(RuntimeException ignored){}}
 public void requestVerification(String rawEmail){requireConfigured();UserAccount account=users.findByEmailNormalized(normalize(rawEmail)).orElse(null);if(account==null||account.isEmailVerified())return;try{issue(account,AccountTokenPurpose.VERIFY_EMAIL);}catch(RuntimeException e){log.warn("Could not deliver an email verification link for an account");}}
 public void confirmEmail(String rawToken){AccountToken token=valid(rawToken,AccountTokenPurpose.VERIFY_EMAIL);UserAccount account=users.findById(token.getUserId()).orElseThrow(()->invalid());account.setEmailVerified(true);users.save(account);consume(token);}
 public void requestPasswordReset(String rawEmail){requireConfigured();UserAccount account=users.findByEmailNormalized(normalize(rawEmail)).orElse(null);if(account==null)return;try{issue(account,AccountTokenPurpose.RESET_PASSWORD);}catch(RuntimeException e){log.warn("Could not deliver a password reset link for an account");}}
 public void resetPassword(ResetRequest request){if(request.password().getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be no longer than 72 UTF-8 bytes");AccountToken token=valid(request.token(),AccountTokenPurpose.RESET_PASSWORD);UserAccount account=users.findById(token.getUserId()).orElseThrow(()->invalid());account.setPasswordHash(passwords.encode(request.password()));users.save(account);consume(token);}
 private void issue(UserAccount account,AccountTokenPurpose purpose){if(!email.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email delivery is not configured. Contact the platform administrator.");tokens.deleteByUserIdAndPurposeAndUsedAtIsNull(account.getId(),purpose);byte[] bytes=new byte[32];random.nextBytes(bytes);String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  AccountToken token=new AccountToken();token.setUserId(account.getId());token.setPurpose(purpose);token.setTokenHash(hash(raw));token.setExpiresAt(Instant.now().plusSeconds(purpose==AccountTokenPurpose.VERIFY_EMAIL?86400:1800));tokens.save(token);
  try{email.sendAccountLink(account.getEmailNormalized(),account.getName(),purpose.name(),raw);}catch(RuntimeException e){tokens.delete(token);throw e;}
 }
 private void requireConfigured(){if(!email.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email delivery is not configured. Contact the platform administrator.");}
 private AccountToken valid(String raw,AccountTokenPurpose purpose){if(raw==null||raw.isBlank())throw invalid();return tokens.findByTokenHashAndPurposeAndUsedAtIsNullAndExpiresAtAfter(hash(raw),purpose,Instant.now()).orElseThrow(AccountSecurityService::invalid);}
 private void consume(AccountToken token){token.setUsedAt(Instant.now());tokens.save(token);}
 private static String normalize(String email){return email==null?"":email.trim().toLowerCase();}
 private static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"This account link is invalid or expired. Request a new link and try again.");}
 public record EmailRequest(@NotBlank @Email @Size(max=254)String email){}
 public record TokenRequest(@NotBlank @Size(max=120)String token){}
 public record ResetRequest(@NotBlank @Size(max=120)String token,@NotBlank @Size(min=10,max=72)String password){}
}
