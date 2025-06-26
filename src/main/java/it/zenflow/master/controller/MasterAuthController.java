package it.zenflow.master.controller;

import org.springframework.stereotype.Controller;
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
    public String dashboard() {
        return "master/dashboard";
    }
}