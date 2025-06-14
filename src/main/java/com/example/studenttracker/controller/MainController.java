package com.example.studenttracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";  // Thymeleaf template adı
    }

    // Diğer mappingler buraya gelebilir
}
