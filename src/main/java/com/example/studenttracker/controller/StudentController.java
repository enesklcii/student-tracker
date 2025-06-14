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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private SubmissionRepository submissionRepository; // ✅ Ödev gönderimi için eklendi

    @PostMapping("/submit")
    public String submitForm(@ModelAttribute Student student, @RequestParam String teacherUsername, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (userObj == null) return "redirect:/login";

        String username = ((User) userObj).getUsername();
        student.setCreatedByUsername(username);
        student.setTeacherUsername(teacherUsername);

        studentRepository.save(student);

        User user = (User) userObj;
        return user.getRole().equals("admin") ? "redirect:/admin" : "redirect:/student";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable("id") Long id, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (userObj == null) return "redirect:/login";

        User user = (User) userObj;
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz ID: " + id));

        if (!user.getRole().equals("admin") && !user.getUsername().equals(student.getTeacherUsername())) {
            return "redirect:/access-denied";
        }

        studentRepository.deleteById(id);
        return user.getRole().equals("admin") ? "redirect:/admin" : "redirect:/teacher";
    }

    @GetMapping("/edit/{id}")
    public String editStudent(@PathVariable("id") Long id, Model model, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (userObj == null) return "redirect:/login";

        User user = (User) userObj;
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz ID: " + id));

        if (!user.getRole().equals("admin") && !user.getUsername().equals(student.getTeacherUsername())) {
            return "redirect:/access-denied";
        }

        model.addAttribute("student", student);
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updateStudent(@PathVariable("id") Long id,
                                @ModelAttribute Student student,
                                HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (userObj == null) return "redirect:/login";

        User user = (User) userObj;

        // Veritabanından mevcut öğrenci verisini al
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz ID: " + id));

        // Sadece admin veya ilgili öğretmen güncelleyebilir
        if (!user.getRole().equals("admin") && !user.getUsername().equals(existingStudent.getTeacherUsername())) {
            return "redirect:/access-denied";
        }

        // 🔧 Mevcut önemli alanlar korunmalı:
        student.setId(id);
        student.setCreatedByUsername(existingStudent.getCreatedByUsername()); // 👈 Öğrenciyi ekleyen kullanıcı
        student.setTeacherUsername(existingStudent.getTeacherUsername());     // 👈 Danışman öğretmen

        // Kaydet
        studentRepository.save(student);

        // Yönlendirme
        return user.getRole().equals("admin") ? "redirect:/admin" : "redirect:/teacher";
    }


    @GetMapping("/student")
    public String studentHome(HttpSession session, Model model) {
        Object userObj = session.getAttribute("loggedUser");
        if (userObj == null) return "redirect:/login";

        String username = ((User) userObj).getUsername();
        model.addAttribute("student", new Student());

        List<Student> studentProjects = studentRepository.findByCreatedByUsername(username);
        model.addAttribute("students", studentProjects);

        List<User> teachers = userRepository.findByRole("teacher");
        model.addAttribute("teachers", teachers);

        // Danışman öğretmenin duyurularını ekle
        if (!studentProjects.isEmpty()) {
        	 List<String> teacherUsernames = studentProjects.stream()
        		        .map(Student::getTeacherUsername)
        		        .filter(t -> t != null && !t.isEmpty()) // ✅ ismi t yaptık
        		        .distinct()
        		        .toList();

        		    List<Announcement> allAnnouncements = announcementRepository.findAll().stream()
        		        .filter(a -> teacherUsernames.contains(a.getAuthor()))
        		        .toList();

        		    model.addAttribute("announcements", allAnnouncements);
        		} else {
        		    model.addAttribute("announcements", List.of());
        		}

        // ✅ if-else dışında olmalı
        	return "student-home";
    }


    @PostMapping("/approve/{id}")
    public String approveProject(@PathVariable("id") Long id, HttpSession session) {
        Object userObj = session.getAttribute("loggedUser");
        if (userObj == null) return "redirect:/login";

        User user = (User) userObj;
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz ID: " + id));

        if (!user.getRole().equals("admin") && !user.getUsername().equals(student.getTeacherUsername())) {
            return "redirect:/access-denied";
        }

        student.setStatus("Onaylandı");
        studentRepository.save(student);

        return user.getRole().equals("admin") ? "redirect:/admin" : "redirect:/teacher";
    }

    @PostMapping("/student/upload")
    public String uploadHomework(@RequestParam("file") MultipartFile file,
                                 @RequestParam("teacherUsername") String teacherUsername,
                                 HttpSession session, Model model) {

        Object userObj = session.getAttribute("loggedUser");
        if (!(userObj instanceof User student)) return "redirect:/login";

        if (file.isEmpty()) {
            model.addAttribute("uploadError", "Dosya seçilmedi!");
            return "redirect:/student";
        }

        try {
            // 1️⃣ Upload dizinini platformdan bağımsız oluştur
            String uploadPath = new File("src/main/resources/static/uploads").getAbsolutePath();
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs(); // dizin yoksa oluştur
            }

            // 2️⃣ Dosya adını oluştur (çakışmaları önlemek için timestamp ekliyoruz)
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File destinationFile = new File(uploadDir, fileName);

            // 3️⃣ Dosyayı kaydet
            file.transferTo(destinationFile);

            // 4️⃣ Veritabanına kayıt
            Submission sub = new Submission();
            sub.setFileName(fileName);
            sub.setStudentUsername(student.getUsername());
            sub.setTeacherUsername(teacherUsername);
            sub.setSubmittedAt(LocalDateTime.now());

            submissionRepository.save(sub);

            return "redirect:/student";

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("uploadError", "Dosya yükleme sırasında hata oluştu.");
            return "redirect:/student?error";
        }
    }

}
