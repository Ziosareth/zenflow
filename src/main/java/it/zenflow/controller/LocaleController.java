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
    public String changeLanguage(HttpServletRequest request, HttpServletResponse response, String lang, String referer) {
        Locale locale = new Locale(lang); // Es: "it", "en", "es"
        localeResolver.setLocale(request, response, locale);

        // If referer is provided, redirect to it, otherwise redirect to home
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }

        // Get the referer from the request header if not provided as a parameter
        String headerReferer = request.getHeader("Referer");
        if (headerReferer != null && !headerReferer.isEmpty()) {
            // Extract the path from the full URL
            String path = headerReferer.replaceFirst("^(https?://[^/]+)?", "");
            return "redirect:" + path;
        }

        return "redirect:/"; // Default redirect to home
    }
}
