package com.example.securenoteapp.model.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shared_notes")
@Getter
@Setter
public class SharedNote {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Note note;

    @ManyToOne
    private User sharedWithUser;


}