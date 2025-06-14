package com.example.studenttracker.controller;

import com.example.studenttracker.model.Announcement;
import com.example.studenttracker.model.Student;
import com.example.studenttracker.model.Submission;
import com.example.studenttracker.model.User;
import com.example.studenttracker.repository.AnnouncementRepository;
import com.example.studenttracker.repository.StudentRepository;
import com.example.studenttracker.repository.SubmissionRepository;
import com.example.studenttracker.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TeacherController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    // 👨‍🏫 Ana panel
    @GetMapping("/teacher")
    public String teacherPanel(HttpSession session, Model model) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User user) || !"teacher".equals(user.getRole())) {
            return "redirect:/login";
        }

        // 📄 Tüm projeler (her proje ayrı satır)
        List<Student> allProjects = studentRepository.findByTeacherUsername(user.getUsername());
        model.addAttribute("students", allProjects);

        // 👨‍🎓 Aynı öğrenci sadece bir kez görünsün
        List<Student> uniqueStudents = allProjects.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                s -> s.getName() + "_" + s.getEmail(),
                                s -> s,
                                (s1, s2) -> s1
                        ),
                        map -> map.values().stream().toList()
                ));
        model.addAttribute("uniqueStudents", uniqueStudents);

        // 📎 Gönderilen ödevler
        List<Submission> submissions = submissionRepository.findByTeacherUsername(user.getUsername());
        model.addAttribute("submissions", submissions);

        // 📢 Duyurular
        List<Announcement> announcements = announcementRepository.findByAuthor(user.getUsername());
        model.addAttribute("announcements", announcements);

        return "teacher-home";
    }

    // 🔄 Rol isteği gönderme
    @PostMapping("/teacher/request-role")
    public String requestRole(@RequestParam String requestedRole, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User user)) {
            return "redirect:/login";
        }

        user.setRoleRequest(requestedRole);
        userRepository.save(user);
        return "redirect:/teacher?success";
    }

    // 📢 Yeni duyuru yayınla
    @PostMapping("/teacher/announcements")
    public String postAnnouncement(@RequestParam String title,
                                   @RequestParam String content,
                                   HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User teacher) || !"teacher".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setAuthor(teacher.getUsername());
        announcement.setCreatedAt(LocalDateTime.now());

        announcementRepository.save(announcement);
        return "redirect:/teacher/announcements";
    }

    // 👨‍🎓 Öğrenciler sayfası (ayrı template)
    @GetMapping("/teacher/students")
    public String viewStudents(HttpSession session, Model model) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User user) || !"teacher".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Student> students = studentRepository.findByTeacherUsername(user.getUsername());

        List<Student> uniqueStudents = students.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                s -> s.getName() + "_" + s.getEmail(),
                                s -> s,
                                (s1, s2) -> s1
                        ),
                        map -> map.values().stream().toList()
                ));

        model.addAttribute("students", uniqueStudents);
        return "teacher-students";
    }

    // 📢 Duyurular sayfası (ayrı template)
    @GetMapping("/teacher/announcements")
    public String showAnnouncements(HttpSession session, Model model) {
        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User user) || !"teacher".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Announcement> announcements = announcementRepository.findByAuthor(user.getUsername());
        model.addAttribute("announcements", announcements);
        return "teacher-announcements";
    }
}
