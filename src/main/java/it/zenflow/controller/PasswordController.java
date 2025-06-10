package it.zenflow.controller;

import it.zenflow.dto.PasswordChangeForm;
import it.zenflow.service.rbac.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final UserService userService;
    private final MessageSource messageSource;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @GetMapping("/change")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        return "password-change";
    }
    
    @PostMapping("/change")
    public String processPasswordChange(@Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm form,
                                       BindingResult result,
                                       RedirectAttributes redirectAttributes,
                                       Authentication authentication) {
        if (result.hasErrors()) {
            return "password-change";
        }
        
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
            return "password-change";
        }
        
        String username = authentication.getName();
        log.debug("Changing password for user: {}", username);
        
        userService.findByUsername(username).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(form.getNewPassword()));
            user.setPasswordChangeRequired(false);
            userService.save(user);
            log.info("Password changed successfully for user: {}", username);
        });
        
        String message = messageSource.getMessage("password.change.success", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        
        return "redirect:/dashboard";
    }
}