package com.example.securenoteapp.model.repository;

import com.example.securenoteapp.model.data.Note;
import com.example.securenoteapp.model.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUser(User user);

    List<Note> findBySharedPublicly(boolean sharedPublicly);
}