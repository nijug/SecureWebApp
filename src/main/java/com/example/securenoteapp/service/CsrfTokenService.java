package com.example.securenoteapp.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.math.BigInteger;
import java.security.SecureRandom;

@Service
public class CsrfTokenService {

    private static final SecureRandom random = new SecureRandom();

    public void generateAndStoreToken(HttpSession session) {
        String token = new BigInteger(130, random).toString(32);
        session.setAttribute("csrfToken", token);
        System.out.println("Generated CSRF token: " + token);
    }

    public boolean isTokenValid(HttpSession session, String token) {
        String sessionToken = (String) session.getAttribute("csrfToken");
        System.out.println("Session token: " + sessionToken);
        System.out.println("Token: " + token);
        System.out.println(sessionToken != null && sessionToken.equals(token));
        return sessionToken != null && sessionToken.equals(token);
    }
}