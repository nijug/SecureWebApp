package com.example.securenoteapp.service;


import com.example.securenoteapp.model.data.PasswordResetToken;
import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.model.repository.UserRepository;
import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final  Argon2PasswordEncoder passwordEncoder;

    private final HashService hashService;

    private final TotpService totpService;

    private final PasswordResetTokenService passTokenService;

    private final Validator validator;

    private static final int MAX_ATTEMPTS = 2;  // Maximum number of failed attempts
    private static final long LOCK_TIME = 1 * 60 * 1000; // 1 minute


    @Autowired
    public UserService(UserRepository userRepository, Validator validator, PasswordResetTokenService passTokenService, HashService hashService, TotpService totpService) {
        this.userRepository = userRepository;
        this.passTokenService = passTokenService;
        this.totpService = totpService;
        this.passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 7168, 5);
        this.hashService = hashService;
        this.validator = validator;
    }
    public void register(User user) throws InterruptedException{
        Thread.sleep(500);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new IllegalArgumentException("Username is already taken");
        }

        try {
            validatePassword(user.getPassword());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        String secretKey = totpService.generateSecretKey();
        user.setTotpSecret(secretKey);

        List<String> recoveryKeys = generateRecoveryKeys();
        storeRecoveryKeys(user, recoveryKeys);
        userRepository.save(user);
    }

    public void validatePassword(String password) {
        String pattern = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}";
        if (!password.matches(pattern)) {
            throw new IllegalArgumentException("Invalid password");
        }

        int N = 95;
        int L = password.length();
        double entropy = Math.log(Math.pow(N, L)) / Math.log(2);

        System.out.println("Entropy: " + entropy);
        if (entropy < 60) {
            throw new IllegalArgumentException("Weak password");
        }
    }

    public User login(User userFromRequest, String totpOrRecoveryKey, HttpSession session) throws InterruptedException{
        Thread.sleep(500);

        User userInDB = userRepository.findByUsername(userFromRequest.getUsername());
        if (userInDB == null) {
            throw new IllegalArgumentException("Invalid username/password");
        }
        if (userInDB.getFailedAttempts() >= MAX_ATTEMPTS) {
            if (System.currentTimeMillis() - userInDB.getLockTime() < LOCK_TIME) {
                throw new IllegalArgumentException("Account is locked. Please try again later.");
            } else {
                // todo: failed attempts should be per ip address
                userInDB.setFailedAttempts(0);
            }
        }

        if (!passwordEncoder.matches(userFromRequest.getPassword(), userInDB.getPassword())) {
            userInDB.setFailedAttempts(userInDB.getFailedAttempts() + 1);
            if (userInDB.getFailedAttempts() >= MAX_ATTEMPTS) {
                userInDB.setLockTime(System.currentTimeMillis());
            }
            userRepository.save(userInDB);
            throw new IllegalArgumentException("Invalid username/password");
        }

        if (isRecoveryKeyValid(userInDB, totpOrRecoveryKey)) {
            invalidateRecoveryKey(userInDB, totpOrRecoveryKey);
        } else if (userInDB.getTotpSecret() != null) {
            try {
                if (!totpService.validateTotp(userInDB.getTotpSecret(), totpOrRecoveryKey, session)) {
                    userInDB.setFailedAttempts(userInDB.getFailedAttempts() + 1);
                    if (userInDB.getFailedAttempts() >= MAX_ATTEMPTS) {
                        userInDB.setLockTime(System.currentTimeMillis());
                    }
                    userRepository.save(userInDB);
                    throw new IllegalArgumentException("Invalid TOTP.");
                }
            } catch (NumberFormatException e) {
                userInDB.setFailedAttempts(userInDB.getFailedAttempts() + 1);
                if (userInDB.getFailedAttempts() >= MAX_ATTEMPTS) {
                    userInDB.setLockTime(System.currentTimeMillis());
                }
                userRepository.save(userInDB);
                throw new IllegalArgumentException("Invalid TOTP.");
            }
        }

        userInDB.setFailedAttempts(0);
        userRepository.save(userInDB);

        return userInDB;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public String forgotPassword(String username) throws InterruptedException {
        Thread.sleep(500);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Invalid username.");
        }
        PasswordResetToken token = passTokenService.createPasswordResetToken(user);
        return token.getToken();
    }

    public void resetPassword(String token, String password) throws InterruptedException {
        Thread.sleep(500);
        PasswordResetToken resetToken = passTokenService.getPasswordResetToken(token);
        if (resetToken == null) {
            throw new IllegalArgumentException("Invalid password reset token. Try again.");
        }
        User user = resetToken.getUser();

        try {
            validatePassword(password);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        passTokenService.deletePasswordResetToken(resetToken);
    }

    public List<String> generateRecoveryKeys() {
        List<String> recoveryKeys = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String recoveryKey = String.format("%08d", new Random().nextInt(100000000));
            recoveryKeys.add(recoveryKey);
        }
        return recoveryKeys;
    }

    public void storeRecoveryKeys(User user, List<String> recoveryKeys) {
        List<String> hashedKeys = recoveryKeys.stream()
                .map(hashService::hash)
                .collect(Collectors.toList());

        user.setRecoveryKeys(new HashSet<>(hashedKeys));
        userRepository.save(user);

    }

    public boolean isRecoveryKeyValid(User user, String recoveryKey) {
        return user.getRecoveryKeys().contains(recoveryKey);
    }

    public void invalidateRecoveryKey(User user, String recoveryKey) {
        user.getRecoveryKeys().remove(recoveryKey);
        userRepository.save(user);
    }

}


