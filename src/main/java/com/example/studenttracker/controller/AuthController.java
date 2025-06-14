package com.example.studenttracker.controller;

import com.example.studenttracker.model.User;
import com.example.studenttracker.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute User user, HttpSession session, Model model) {
        User dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
            session.setAttribute("loggedUser", dbUser);

            switch (dbUser.getRole()) {
                case "admin":
                    return "redirect:/admin";
                case "teacher":
                    return "redirect:/teacher";
                case "student":
                    return "redirect:/student";
                default:
                    model.addAttribute("error", "Tanımsız rol!");
                    return "login";
            }
        } else {
            model.addAttribute("error", "Geçersiz kullanıcı adı veya şifre!");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute User user, Model model) {
        // Kullanıcı adı benzersiz mi?
        if (userRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Bu kullanıcı adı zaten alınmış.");
            return "register";
        }
        
        // Email benzersiz mi?
        if (userRepository.findByEmail(user.getEmail()) != null) {
            model.addAttribute("error", "Bu e-posta zaten kayıtlı.");
            return "register";
        }

        // Email sadece @samsun.edu.tr uzantılı mı?
        if (user.getEmail() == null || !user.getEmail().toLowerCase().endsWith("@samsun.edu.tr")) {
            model.addAttribute("error", "Sadece @samsun.edu.tr uzantılı e-postalar kabul edilir.");
            return "register";
        }

        // Rol sabit öğrenci (student) olarak atanıyor
        user.setRole("student");

        userRepository.save(user);
        return "redirect:/login";
    }
}
