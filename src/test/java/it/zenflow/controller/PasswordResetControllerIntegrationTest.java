package it.zenflow.controller;

import it.zenflow.model.rbac.User;
import it.zenflow.model.rbac.UserRepository;
import it.zenflow.service.EmailService;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class PasswordResetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private EmailService emailService;

    private User testUser;
    private User userWithToken;

    @BeforeEach
    public void setup() {
        // Clear existing data
        userRepository.deleteAll();

        // Reset mock to avoid any previous interactions
        reset(emailService);

        // Mock email service to avoid sending actual emails
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), any(Locale.class));

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create user with reset token
        userWithToken = new User();
        userWithToken.setUsername("tokenuser");
        userWithToken.setEmail("token@example.com");
        userWithToken.setPassword("password");
        userWithToken.setEnabled(true);
        userWithToken.setResetToken("valid-token");
        userWithToken.setResetTokenExpiry(LocalDateTime.now().plusHours(24)); // Valid for 24 hours
        userWithToken = userRepository.save(userWithToken);
    }

    @Test
    public void testShowForgotPasswordForm() throws Exception {
        mockMvc.perform(get("/password/forgot"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-forgot"))
                .andExpect(model().attributeExists("passwordResetRequestForm"));
    }

    @Test
    public void testProcessForgotPasswordWithValidEmail() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        mockMvc.perform(post("/password/forgot")
                        .param("email", testUser.getEmail())
                        .locale(Locale.ENGLISH)
                        .param("lang", "en")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("message"));

        // Verify email service was called
        verify(emailService, times(1)).sendPasswordResetEmail(eq(testUser.getEmail()), anyString(), eq(Locale.ENGLISH));
    }

    @Test
    public void testProcessForgotPasswordWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/password/forgot")
                        .param("email", "nonexistent@example.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("message"));

        // Verify email service was not called for non-existent email
        verify(emailService, times(0)).sendPasswordResetEmail(eq("nonexistent@example.com"), anyString(), any(Locale.class));
    }

    @Test
    public void testShowResetPasswordFormWithValidToken() throws Exception {
        mockMvc.perform(get("/password/reset")
                        .param("token", userWithToken.getResetToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(model().attributeExists("passwordResetForm"));
    }

    @Test
    public void testShowResetPasswordFormWithInvalidToken() throws Exception {
        mockMvc.perform(get("/password/reset")
                        .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-error"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    public void testShowResetPasswordFormWithExpiredToken() throws Exception {
        // Create user with expired token
        User expiredUser = new User();
        expiredUser.setUsername("expired");
        expiredUser.setEmail("expired@example.com");
        expiredUser.setPassword("password");
        expiredUser.setEnabled(true);
        expiredUser.setResetToken("expired-token");
        expiredUser.setResetTokenExpiry(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        expiredUser = userRepository.save(expiredUser);

        mockMvc.perform(get("/password/reset")
                        .param("token", expiredUser.getResetToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-error"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    public void testProcessResetPasswordWithValidToken() throws Exception {
        String originalPassword = userWithToken.getPassword();

        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "newpassword123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("message"));

        // Reload user from database
        User updatedUser = userRepository.findById(userWithToken.getId()).orElseThrow();

        // Verify password was changed
        assert !updatedUser.getPassword().equals(originalPassword);

        // Verify token was cleared
        assert updatedUser.getResetToken() == null;
        assert updatedUser.getResetTokenExpiry() == null;
    }

    @Test
    public void testProcessResetPasswordWithPasswordMismatch() throws Exception {
        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "differentpassword")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "confirmPassword"));

        // Verify user was not updated
        User unchangedUser = userRepository.findById(userWithToken.getId()).orElseThrow();
        assert unchangedUser.getResetToken() != null;
    }

    @Test
    public void testProcessResetPasswordWithShortPassword() throws Exception {
        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "short")
                        .param("confirmPassword", "short")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "newPassword"));

        // Verify user was not updated
        User unchangedUser = userRepository.findById(userWithToken.getId()).orElseThrow();
        assert unchangedUser.getResetToken() != null;
    }

    @Test
    public void testProcessResetPasswordWithInvalidToken() throws Exception {
        mockMvc.perform(post("/password/reset")
                        .param("token", "invalid-token")
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "newpassword123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/forgot"))
                .andExpect(flash().attributeExists("error"));
    }

    // Internationalization tests

    @Test
    public void testForgotPasswordFormInEnglish() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        mockMvc.perform(get("/password/forgot")
                        .locale(Locale.ENGLISH)
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-forgot"))
                .andExpect(content().string(containsString("Forgot Password")))
                .andExpect(content().string(containsString("Enter your email address")))
                .andExpect(content().string(containsString("Send Reset Link")))
                .andExpect(content().string(containsString("Back to Login")));
    }

    @Test
    public void testForgotPasswordFormInItalian() throws Exception {
        mockMvc.perform(get("/password/forgot")
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(view().name("password-forgot"))
                .andExpect(content().string(containsString("Password Dimenticata")))
                .andExpect(content().string(containsString("Inserisci il tuo indirizzo email")))
                .andExpect(content().string(containsString("Invia Link di Reset")))
                .andExpect(content().string(containsString("Torna al Login")));
    }

    @Test
    public void testResetPasswordFormInEnglish() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        mockMvc.perform(get("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .locale(Locale.ENGLISH)
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(content().string(containsString("Reset Password")))
                .andExpect(content().string(containsString("New Password")))
                .andExpect(content().string(containsString("Confirm Password")));
    }

    @Test
    public void testResetPasswordFormInItalian() throws Exception {
        mockMvc.perform(get("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(content().string(containsString("Reimposta Password")))
                .andExpect(content().string(containsString("Nuova Password")))
                .andExpect(content().string(containsString("Conferma Password")));
    }

    @Test
    public void testResetPasswordErrorInEnglish() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        mockMvc.perform(get("/password/reset")
                        .param("token", "invalid-token")
                        .locale(Locale.ENGLISH)
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-error"))
                .andExpect(content().string(containsString("Reset Password Error")))
                .andExpect(content().string(containsString("Invalid or expired password reset link")))
                .andExpect(content().string(containsString("Request New Reset Link")));
    }

    @Test
    public void testResetPasswordErrorInItalian() throws Exception {
        mockMvc.perform(get("/password/reset")
                        .param("token", "invalid-token")
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-error"))
                .andExpect(content().string(containsString("Errore Reimpostazione Password")))
                .andExpect(content().string(containsString("Link di reimpostazione password non valido o scaduto")))
                .andExpect(content().string(containsString("Richiedi Nuovo Link di Reset")));
    }

    @Test
    public void testFlashMessagesInEnglish() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        // Set up the mock to return the expected message
        String expectedMessage = messageSource.getMessage("password.reset.email.sent", null, Locale.ENGLISH);

        mockMvc.perform(post("/password/forgot")
                        .param("email", testUser.getEmail())
                        .locale(Locale.ENGLISH)
                        .param("lang", "en")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", expectedMessage));
    }

    @Test
    public void testFlashMessagesInItalian() throws Exception {
        // Set up the mock to return the expected message
        String expectedMessage = messageSource.getMessage("password.reset.email.sent", null, Locale.ITALIAN);

        mockMvc.perform(post("/password/forgot")
                        .param("email", testUser.getEmail())
                        .locale(Locale.ITALIAN)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", expectedMessage));
    }
}
