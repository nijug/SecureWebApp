package com.example.securenoteapp.service;

import com.example.securenoteapp.model.data.PasswordResetToken;
import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.model.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import java.util.Calendar;
import java.util.UUID;

@Service
public class PasswordResetTokenService {


    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public  PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public PasswordResetToken createPasswordResetToken(User user) {
        PasswordResetToken existingToken = passwordResetTokenRepository.findByUser(user);
        if (existingToken != null) {
            passwordResetTokenRepository.delete(existingToken);
        }
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(calculateExpiryDate(PasswordResetToken.getEXPIRATION()));
        return passwordResetTokenRepository.save(token);
    }

    public PasswordResetToken getPasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token);
    }

    public void deletePasswordResetToken(PasswordResetToken token) {
        passwordResetTokenRepository.delete(token);
    }

    private java.sql.Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new java.sql.Date(cal.getTime().getTime());
    }

}
