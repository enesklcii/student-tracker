package com.example.studenttracker.repository;

import com.example.studenttracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);

    List<User> findByRole(String role);
    List<User> findByRoleRequest(String roleRequest);


    // Aktivasyon için
    User findByActivationCode(String activationCode);

    // Şifre sıfırlama için
    User findByResetPasswordToken(String resetPasswordToken);
}
