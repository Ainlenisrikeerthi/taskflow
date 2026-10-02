package com.taskflow.backend.service;

import com.taskflow.backend.dto.GoogleLoginRequest;
import com.taskflow.backend.dto.JwtResponse;
import com.taskflow.backend.dto.LoginRequest;
import com.taskflow.backend.dto.RegisterRequest;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.exception.DuplicateResourceException;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.model.PasswordResetToken;
import com.taskflow.backend.model.Role;
import com.taskflow.backend.model.User;
import com.taskflow.backend.repository.PasswordResetTokenRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail().trim().toLowerCase(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(loginRequest.getEmail().trim().toLowerCase());

        User user = userRepository.findByEmail(loginRequest.getEmail().trim().toLowerCase())
                .orElseThrow();

        return new JwtResponse(jwt, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    // -------------------------------------------------------------------------
    // Register  — ALWAYS creates a USER account (ADMIN role is not assignable via API)
    // -------------------------------------------------------------------------

    @Transactional
    public JwtResponse register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered: " + email);
        }

        User user = new User();
        user.setName(registerRequest.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        // Public registration always creates USER accounts.
        // ADMIN role must be set directly in the database by a trusted administrator.
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        logger.info("[AUTH] New USER account registered: {}", email);

        String jwt = jwtUtils.generateToken(savedUser.getEmail());
        return new JwtResponse(jwt, savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name());
    }

    // -------------------------------------------------------------------------
    // Google OAuth
    // -------------------------------------------------------------------------

    @Transactional
    public JwtResponse googleLogin(GoogleLoginRequest googleRequest) {
        String email = googleRequest.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByGoogleId(googleRequest.getGoogleId());

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User existingUser = userOpt.get();
                existingUser.setGoogleId(googleRequest.getGoogleId());
                userRepository.save(existingUser);
            } else {
                User newUser = new User();
                newUser.setName(googleRequest.getName().trim());
                newUser.setEmail(email);
                newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                newUser.setGoogleId(googleRequest.getGoogleId());
                newUser.setRole(Role.USER);
                User savedUser = userRepository.save(newUser);
                userOpt = Optional.of(savedUser);
                logger.info("[AUTH] New USER account created via Google OAuth: {}", email);
            }
        }

        User user = userOpt.get();
        String jwt = jwtUtils.generateToken(user.getEmail());
        return new JwtResponse(jwt, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    protected String saveResetToken(String normalizedEmail, User user) {
        // Delete any existing tokens for this email (one at a time)
        passwordResetTokenRepository.deleteByEmail(normalizedEmail);

        // Generate a secure random token
        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(rawToken);
        resetToken.setEmail(normalizedEmail);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        return rawToken;
    }

    // forgotPassword is NOT @Transactional so that the token commit is not rolled back by email failure
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        logger.info("[AUTH] Password reset requested for: {}", normalizedEmail);

        // A reset email can only be sent for an existing TaskFlow account.
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            logger.warn("[AUTH] Password reset requested for unregistered email: {}", normalizedEmail);
            throw new ResourceNotFoundException("No TaskFlow account was found with that email address.");
        }

        User user = userOpt.get();

        // Save token in its own transaction so it commits before email is sent
        String rawToken = saveResetToken(normalizedEmail, user);

        // Build the reset link pointing to the frontend
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        logger.info("[AUTH] Password reset link generated for: {} (link not logged for security)", normalizedEmail);

        // Send the email — exceptions propagate to the frontend
        // Token is already committed, so failing here does NOT roll it back
        emailService.sendPasswordResetEmail(normalizedEmail, user.getName(), resetLink);
    }

    // -------------------------------------------------------------------------
    // Reset Password — validates the token and updates the password
    // -------------------------------------------------------------------------

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset link. Please request a new one."));

        if (resetToken.isUsed()) {
            throw new BadRequestException("This password reset link has already been used. Please request a new one.");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("This password reset link has expired (valid for 1 hour). Please request a new one.");
        }

        String email = resetToken.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for this reset token."));

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the token
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        logger.info("[AUTH] Password successfully reset for: {}", email);
    }
}
