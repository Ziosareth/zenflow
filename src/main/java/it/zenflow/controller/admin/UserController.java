package it.zenflow.controller.admin;

import it.zenflow.model.rbac.User;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHLEAD')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping("")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        return userService.findById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    return "admin/user-detail";
                })
                .orElse("redirect:/admin/users");
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        return userService.findById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    return "admin/user-edit";
                })
                .orElse("redirect:/admin/users");
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        return userService.findById(id)
                .map(existingUser -> {
                    // Update only allowed fields (not password)
                    existingUser.setUsername(user.getUsername());
                    existingUser.setEmail(user.getEmail());
                    existingUser.setEnabled(user.isEnabled());
                    existingUser.setRoles(user.getRoles());

                    userService.save(existingUser);
                    String message = messageSource.getMessage("admin.users.updated", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);
                    return "redirect:/admin/users/" + id;
                })
                .orElse("redirect:/admin/users");
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return userService.findById(id)
                .map(user -> {
                    user.setEnabled(!user.isEnabled());
                    userService.save(user);
                    String messageKey = user.isEnabled() ? "admin.users.enabled" : "admin.users.disabled";
                    String message = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);
                    return "redirect:/admin/users/" + id;
                })
                .orElse("redirect:/admin/users");
    }
}
