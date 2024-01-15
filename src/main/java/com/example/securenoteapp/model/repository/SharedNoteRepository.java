package com.example.securenoteapp.model.repository;

import com.example.securenoteapp.model.data.Note;
import com.example.securenoteapp.model.data.SharedNote;
import com.example.securenoteapp.model.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedNoteRepository extends JpaRepository<SharedNote, Long> {
    List<SharedNote> findByNote(Note note);

    List<SharedNote> findBySharedWithUser(User user);
}