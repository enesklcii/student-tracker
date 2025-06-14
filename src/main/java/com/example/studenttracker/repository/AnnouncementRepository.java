package com.example.studenttracker.repository;

import com.example.studenttracker.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByAuthor(String author);
}
