package com.pgconnect.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final AccountSecurityService accountSecurity;
    private final GoogleLoginService googleLogin;
    private final UserDetailsService userDetailsService;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy,
                          AccountSecurityService accountSecurity,
                          GoogleLoginService googleLogin,
                          UserDetailsService userDetailsService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.accountSecurity = accountSecurity;
        this.googleLogin = googleLogin;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse) {
        Role role = request.role() == null ? Role.RESIDENT : request.role();
        if (role == Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrator accounts are provisioned separately");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be no longer than 72 UTF-8 bytes");
        }
        String email = request.email().trim().toLowerCase();
        UserAccount account = new UserAccount(request.name().trim(), email,
                passwordEncoder.encode(request.password()), role);
        try {
            account = users.save(account);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        accountSecurity.sendVerificationForNewAccount(account);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        persistAuthentication(authentication, servletRequest, servletResponse);
        return AuthResponse.from(account);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest,
                              HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        persistAuthentication(authentication, servletRequest, servletResponse);
        UserAccount account = users.findById(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return AuthResponse.from(account);
    }

    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                    HttpServletRequest servletRequest,
                                    HttpServletResponse servletResponse) {
        UserAccount account = googleLogin.resolveAccount(request.credential(), request.role());
        UserDetails principal = userDetailsService.loadUserByUsername(account.getId());
        if (!principal.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled");
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        persistAuthentication(authentication, servletRequest, servletResponse);
        return AuthResponse.from(account);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        UserAccount account = users.findById(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        return AuthResponse.from(account);
    }

    @PatchMapping("/profile")
    public AuthResponse updateProfile(@Valid @RequestBody ProfileRequest request, Authentication authentication) {
        UserAccount account = users.findById(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        account.setName(request.name().trim());
        account.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return AuthResponse.from(users.save(account));
    }

    @PostMapping("/email-verification/request")
    public MessageResponse requestEmailVerification(@Valid @RequestBody AccountSecurityService.EmailRequest request) {
        accountSecurity.requestVerification(request.email());
        return new MessageResponse("If the account needs verification, a secure one-time link has been sent.");
    }

    @PostMapping("/email-verification/confirm")
    public MessageResponse confirmEmail(@Valid @RequestBody AccountSecurityService.TokenRequest request) {
        accountSecurity.confirmEmail(request.token());
        return new MessageResponse("Email verified. You can now use owner publication and resident booking workflows.");
    }

    @PostMapping("/password-reset/request")
    public MessageResponse requestPasswordReset(@Valid @RequestBody AccountSecurityService.EmailRequest request) {
        accountSecurity.requestPasswordReset(request.email());
        return new MessageResponse("If the account exists, a password reset link has been sent.");
    }

    @PostMapping("/password-reset/complete")
    public MessageResponse completePasswordReset(@Valid @RequestBody AccountSecurityService.ResetRequest request) {
        accountSecurity.resetPassword(request);
        return new MessageResponse("Password updated. Log in with your new password.");
    }

    private void persistAuthentication(Authentication authentication, HttpServletRequest request,
                                       HttpServletResponse response) {
        if (request.getSession(false) != null) sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 72) String password,
            Role role) {}

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {}
    public record GoogleLoginRequest(@NotBlank @Size(max = 12000) String credential, Role role) {}
    public record ProfileRequest(@NotBlank @Size(min = 2, max = 80) String name,
                                 @jakarta.validation.constraints.Pattern(regexp = "^$|^[+0-9() -]{7,20}$") String phone) {}
    public record CsrfResponse(String headerName, String token) {}
    public record MessageResponse(String message) {}
    public record AuthResponse(String id, String name, String email, String role, String subscriptionStatus, String phone, boolean emailVerified) {
        static AuthResponse from(UserAccount account) {
            return new AuthResponse(account.getId(), account.getName(), account.getEmailNormalized(),
                    account.getRole().name(), account.getSubscriptionStatus() == null ? null : account.getSubscriptionStatus().name(), account.getPhone(),account.isEmailVerified());
        }
    }
}
