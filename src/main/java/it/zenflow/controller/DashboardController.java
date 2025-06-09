package it.zenflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller("/dashboard")
public class DashboardController {

    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }

}
