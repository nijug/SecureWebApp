package com.example.securenoteapp.model.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private User user;

    private String content;
    private boolean isEncrypted;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean sharedPublicly = false;


    @OneToMany(mappedBy = "note")
    private Set<SharedNote> sharedNotes = new HashSet<>();


}