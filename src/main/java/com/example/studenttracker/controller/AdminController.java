package com.example.studenttracker.controller;

import com.example.studenttracker.model.User;
import com.example.studenttracker.model.Student;
import com.example.studenttracker.repository.StudentRepository;
import com.example.studenttracker.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    // --- Öğrenci listesi ve filtreleme ---
    @GetMapping("/admin")
    public String adminHome(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model) {

        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        List<Student> students;
        if (name != null && !name.isEmpty() && status != null && !status.isEmpty()) {
            students = studentRepository.findByNameContainingIgnoreCaseAndStatus(name, status);
        } else if (name != null && !name.isEmpty()) {
            students = studentRepository.findByNameContainingIgnoreCase(name);
        } else if (status != null && !status.isEmpty()) {
            students = studentRepository.findByStatus(status);
        } else {
            students = studentRepository.findAll();
        }

        List<User> roleRequests = userRepository.findByRoleRequest("admin");

        model.addAttribute("students", students);
        model.addAttribute("roleRequests", roleRequests);
        model.addAttribute("selectedName", name);
        model.addAttribute("selectedStatus", status);

        return "admin-home";
    }

    // --- Role onaylama ---
    @PostMapping("/approve-role/{id}")
    public String approveRoleRequest(@PathVariable Long id, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        User requestedUser = userRepository.findById(id).orElse(null);
        if (requestedUser != null && "admin".equals(requestedUser.getRoleRequest())) {
            requestedUser.setRole("admin");
            requestedUser.setRoleRequest(null);
            userRepository.save(requestedUser);
        }

        return "redirect:/admin";
    }

    // --- Yeni Kullanıcı Ekle (GET) ---
    @GetMapping("/admin/add-user")
    public String showAddUserForm(Model model, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("user", new User());
        return "add-user";
    }

    // --- Yeni Kullanıcı Ekle (POST) ---
    @PostMapping("/admin/add-user")
    public String handleAddUser(@ModelAttribute User user, Model model, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Bu kullanıcı adı zaten kayıtlı.");
            return "add-user";
        }

        if (!"teacher".equals(user.getRole()) && !"admin".equals(user.getRole())) {
            model.addAttribute("error", "Yalnızca 'teacher' veya 'admin' rolü verilebilir.");
            return "add-user";
        }

        userRepository.save(user);
        return "redirect:/admin/users";
    }

    // --- Kullanıcı Listesi ---
    @GetMapping("/admin/users")
    public String listUsers(Model model, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users-list";
    }

    // --- Kullanıcı Güncelleme Formu (GET) ---
    @GetMapping("/admin/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz ID: " + id));
        model.addAttribute("user", user);
        return "admin/user-form";
    }

    // --- Kullanıcı Güncelleme (POST) ---
    @PostMapping("/admin/users/edit/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, Model model, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        user.setId(id);

        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null && !existingUser.getId().equals(id)) {
            model.addAttribute("error", "Bu kullanıcı adı zaten kayıtlı.");
            return "admin/user-form";
        }

        if (!"teacher".equals(user.getRole()) && !"admin".equals(user.getRole()) && !"student".equals(user.getRole())) {
            model.addAttribute("error", "Geçersiz rol seçimi.");
            return "admin/user-form";
        }

        userRepository.save(user);
        return "redirect:/admin/users";
    }

    // --- Kullanıcı Sil ---
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User loggedUser) || !"admin".equals(loggedUser.getRole())) {
            return "redirect:/login";
        }

        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }
}
