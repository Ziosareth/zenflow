package it.zenflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Controller
public class LocaleController {

    @Autowired
    private LocaleResolver localeResolver;

    @GetMapping("/change-lang")
    public String changeLanguage(HttpServletRequest request, HttpServletResponse response, String lang) {
        Locale locale = new Locale(lang); // Es: "it", "en", "es"
        localeResolver.setLocale(request, response, locale);
        return "redirect:/"; // Modifica secondo la pagina di destinazione
    }
}