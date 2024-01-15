package com.example.securenoteapp.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class NoteEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; // GCM recommends a 12-byte IV
    private static final int TAG_BIT_LENGTH = 128; // GCM recommends a tag length of 128 bits
    private static final int SALT_SIZE = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    private SecretKeySpec getKeyFromPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    public String encrypt(String content, String secretPassword) throws Exception {
        // Generate a random salt
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);

        SecretKeySpec secretKey = getKeyFromPassword(secretPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);

        byte[] iv = new byte[IV_SIZE];
        secureRandom.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] encryptedContent = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));


        byte[] saltIvAndEncryptedContent = new byte[SALT_SIZE + IV_SIZE + encryptedContent.length];
        System.arraycopy(salt, 0, saltIvAndEncryptedContent, 0, SALT_SIZE);
        System.arraycopy(iv, 0, saltIvAndEncryptedContent, SALT_SIZE, IV_SIZE);
        System.arraycopy(encryptedContent, 0, saltIvAndEncryptedContent, SALT_SIZE + IV_SIZE, encryptedContent.length);

        return Base64.getEncoder().encodeToString(saltIvAndEncryptedContent);
    }

    public String decrypt(String saltIvAndEncryptedContentBase64, String secretPassword) throws Exception {
        byte[] saltIvAndEncryptedContent = Base64.getDecoder().decode(saltIvAndEncryptedContentBase64);

        byte[] salt = new byte[SALT_SIZE];
        System.arraycopy(saltIvAndEncryptedContent, 0, salt, 0, SALT_SIZE);

        SecretKeySpec secretKey = getKeyFromPassword(secretPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);


        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(saltIvAndEncryptedContent, SALT_SIZE, iv, 0, IV_SIZE);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);


        int encryptedContentSize = saltIvAndEncryptedContent.length - SALT_SIZE - IV_SIZE;
        byte[] encryptedContent = new byte[encryptedContentSize];
        System.arraycopy(saltIvAndEncryptedContent, SALT_SIZE + IV_SIZE, encryptedContent, 0, encryptedContentSize);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedContent = cipher.doFinal(encryptedContent);

        return new String(decryptedContent, StandardCharsets.UTF_8);
    }
}

// szyfrujemy za pomocą algorytmu AES/GCM( Galois/CounterMode) /NoPadding 256 bitowym kluczem,
// który generujemy z sekretnego hasła podanego przy tworzeniu notatki i solowej soli
