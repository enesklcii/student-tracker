package com.example.studenttracker.repository;

import com.example.studenttracker.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByTeacherUsername(String teacherUsername);
    List<Submission> findByStudentUsername(String studentUsername);
    
}
