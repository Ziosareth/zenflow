package it.zenflow.controller;

import it.zenflow.model.rbac.User;
import it.zenflow.service.rbac.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserService userService;

    @Autowired
    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public String settings(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Optional<User> userOpt = userService.findByUsername(username);
        userOpt.ifPresent(user -> model.addAttribute("user", user));
        
        return "settings";
    }
    
    @PostMapping("/theme")
    public String updateTheme(@RequestParam("theme") String theme, 
                             RedirectAttributes redirectAttributes) {
        // In a real implementation, this would save the theme preference to the user's profile
        // For now, we'll just use localStorage in JavaScript
        
        redirectAttributes.addFlashAttribute("successMessage", "settings.theme.updated");
        return "redirect:/settings";
    }
}