package it.zenflow.controller.admin;

import it.zenflow.model.rbac.User;
import it.zenflow.service.rbac.RoleService;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('READ_USER')")
@RequiredArgsConstructor
@Slf4j
public class UserInviteController {
    
    private final UserService userService;
    private final RoleService roleService;
    private final MessageSource messageSource;
    
    @GetMapping("/invite")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public String inviteUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.findAll());
        return "admin/user-invite";
    }
    
    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public String inviteUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        log.info("Inviting new user: {}", user.getUsername());
        
        userService.inviteUser(
            user.getUsername(), 
            user.getEmail(), 
            user.getRoles(), 
            LocaleContextHolder.getLocale()
        );
        
        String message = messageSource.getMessage("admin.users.invite.success", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/admin/users";
    }
}