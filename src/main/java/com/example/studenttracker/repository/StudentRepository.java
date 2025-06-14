package com.example.studenttracker.repository;

import com.example.studenttracker.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Öğrencinin kendi projeleri için (öğrenci paneli)
    List<Student> findByCreatedByUsername(String username);

    // Admin paneli için filtre sorguları
    List<Student> findByNameContainingIgnoreCase(String name);
    List<Student> findByStatus(String status);
    List<Student> findByNameContainingIgnoreCaseAndStatus(String name, String status);
    List<Student> findByTeacherUsername(String teacherUsername);
}
