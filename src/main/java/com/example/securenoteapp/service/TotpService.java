package com.example.securenoteapp.service;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    public String generateSecretKey() {
        return Base32.random();
    }

    public String generateTotp(String secretKey) {
        Totp totp = new Totp(secretKey);
        return totp.now();
    }

    public boolean validateTotp(String secretKey, String totp) {
        Totp totpGenerator = new Totp(secretKey);
        return totpGenerator.verify(totp);
    }
}