package com.example.securenoteapp.model.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Username cannot be null")
    @Size(min = 2, max = 30, message = "Username must be between 2 and 30 characters")
    private String username;

    @NotNull(message = "Password cannot be null")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    private String totpSecret;

    @OneToMany(mappedBy = "user")
    private Set<Note> notes = new HashSet<>();

    @NotNull
    private Integer failedAttempts = 0;
    @NotNull
    private Long lockTime = 0L;

    @NotNull
    @ElementCollection
    private Set<String> recoveryKeys = new HashSet<>();
}