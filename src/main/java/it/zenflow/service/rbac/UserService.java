package it.zenflow.service.rbac;

import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.User;
import it.zenflow.model.rbac.UserRepository;
import it.zenflow.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User inviteUser(String username, String email, Set<Role> roles, Locale locale) {
        // Generate a random password
        String temporaryPassword = generateRandomPassword();

        // Create and save the user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setEnabled(true);
        user.setRoles(roles);
        user.setPasswordChangeRequired(true);

        User savedUser = userRepository.save(user);

        // Send invitation email
        emailService.sendInvitationEmail(email, username, temporaryPassword, locale);

        return savedUser;
    }

    private String generateRandomPassword() {
        // Generate a secure random password (12 characters)
        return RandomStringUtils.secure().nextAlphanumeric(12);
    }

    @Transactional
    public void createPasswordResetToken(String email, Locale locale) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = generateSecureToken();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), token, locale);
        });
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        return userRepository.findByResetToken(token)
            .filter(user -> user.getResetTokenExpiry().isAfter(LocalDateTime.now()))
            .isPresent();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        userRepository.findByResetToken(token)
            .filter(user -> user.getResetTokenExpiry().isAfter(LocalDateTime.now()))
            .ifPresent(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
            });
    }

    private String generateSecureToken() {
        return UUID.randomUUID().toString();
    }
}
