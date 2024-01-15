package com.example.securenoteapp.model.repository;

import com.example.securenoteapp.model.data.PasswordResetToken;
import com.example.securenoteapp.model.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findByToken(String token);

    PasswordResetToken findByUser(User user);
}
