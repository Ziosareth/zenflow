package it.zenflow.controller;

import it.zenflow.dto.PasswordResetForm;
import it.zenflow.dto.PasswordResetRequestForm;
import it.zenflow.service.rbac.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {
    private final UserService userService;
    private final MessageSource messageSource;
    
    @GetMapping("/forgot")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("passwordResetRequestForm", new PasswordResetRequestForm());
        return "password-forgot";
    }
    
    @PostMapping("/forgot")
    public String processForgotPassword(@Valid @ModelAttribute("passwordResetRequestForm") PasswordResetRequestForm form,
                                       BindingResult result,
                                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "password-forgot";
        }
        
        userService.createPasswordResetToken(form.getEmail(), LocaleContextHolder.getLocale());
        
        String message = messageSource.getMessage("password.reset.email.sent", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        
        return "redirect:/login";
    }
    
    @GetMapping("/reset")
    public String showResetPasswordForm(@RequestParam(required = false) String token, Model model) {
        if (token == null || !userService.validateResetToken(token)) {
            String message = messageSource.getMessage("password.reset.invalid.token", null, LocaleContextHolder.getLocale());
            model.addAttribute("error", message);
            return "password-reset-error";
        }
        
        PasswordResetForm form = new PasswordResetForm();
        form.setToken(token);
        model.addAttribute("passwordResetForm", form);
        
        return "password-reset";
    }
    
    @PostMapping("/reset")
    public String processResetPassword(@Valid @ModelAttribute("passwordResetForm") PasswordResetForm form,
                                      BindingResult result,
                                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "password-reset";
        }
        
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch");
            return "password-reset";
        }
        
        if (!userService.validateResetToken(form.getToken())) {
            String message = messageSource.getMessage("password.reset.invalid.token", null, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("error", message);
            return "redirect:/password/forgot";
        }
        
        userService.resetPassword(form.getToken(), form.getNewPassword());
        
        String message = messageSource.getMessage("password.reset.success", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        
        return "redirect:/login";
    }
}