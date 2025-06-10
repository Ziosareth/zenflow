package it.zenflow.controller;

import it.zenflow.model.rbac.User;
import it.zenflow.model.rbac.UserRepository;
import it.zenflow.service.EmailService;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class PasswordResetControllerI18nIntegrationTest {

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
    public void testForgotPasswordPageInEnglish() throws Exception {
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
    public void testForgotPasswordPageInItalian() throws Exception {
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
    public void testResetPasswordPageInEnglish() throws Exception {
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
    public void testResetPasswordPageInItalian() throws Exception {
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
    public void testResetPasswordErrorPageInEnglish() throws Exception {
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
    public void testResetPasswordErrorPageInItalian() throws Exception {
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

        // Get the expected message from the message source
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
        // Get the expected message from the message source
        String expectedMessage = messageSource.getMessage("password.reset.email.sent", null, Locale.ITALIAN);

        mockMvc.perform(post("/password/forgot")
                        .param("email", testUser.getEmail())
                        .locale(Locale.ITALIAN)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", expectedMessage));
    }

    @Test
    public void testPasswordResetSuccessMessageInEnglish() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        // Get the expected message from the message source
        String expectedMessage = messageSource.getMessage("password.reset.success", null, Locale.ENGLISH);

        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "newpassword123")
                        .locale(Locale.ENGLISH)
                        .param("lang", "en")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", expectedMessage));
    }

    @Test
    public void testPasswordResetSuccessMessageInItalian() throws Exception {
        // Get the expected message from the message source
        String expectedMessage = messageSource.getMessage("password.reset.success", null, Locale.ITALIAN);

        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "newpassword123")
                        .locale(Locale.ITALIAN)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", expectedMessage));
    }

    @Test
    public void testPasswordMismatchErrorInEnglish() throws Exception {
        // Set locale to English
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "differentpassword")
                        .locale(Locale.ENGLISH)
                        .param("lang", "en")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "confirmPassword"))
                .andExpect(content().string(containsString("Passwords do not match")));
    }

    @Test
    public void testPasswordMismatchErrorInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        mockMvc.perform(post("/password/reset")
                        .param("token", userWithToken.getResetToken())
                        .param("newPassword", "newpassword123")
                        .param("confirmPassword", "differentpassword")
                        .locale(Locale.ITALIAN)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "confirmPassword"))
                .andExpect(content().string(containsString("Le password non corrispondono")));
    }
}
