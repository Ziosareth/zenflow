package it.zenflow.master.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/master")
public class MasterAuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "master/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentSection", "dashboard");
        return "master/dashboard";
    }
}
