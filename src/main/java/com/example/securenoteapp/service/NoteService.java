package com.example.securenoteapp.service;


import com.example.securenoteapp.model.data.Note;
import com.example.securenoteapp.model.data.SharedNote;
import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.model.repository.NoteRepository;
import com.example.securenoteapp.model.repository.SharedNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final SharedNoteRepository sharedNoteRepository;

    private final NoteEncryptionService noteEncryptionService;
    @Autowired
    public NoteService(NoteRepository noteRepository, SharedNoteRepository sharedNoteRepository) {
        this.noteRepository = noteRepository;
        this.sharedNoteRepository = sharedNoteRepository;
        this.noteEncryptionService = new NoteEncryptionService();
    }

    public void createNote(Note note, String secretPassword) throws Exception {
        if (secretPassword != null && !secretPassword.isEmpty()) {
            String encryptedContent = noteEncryptionService.encrypt(note.getContent(), secretPassword);
            note.setContent(encryptedContent);
            note.setEncrypted(true);
        }
        noteRepository.save(note);
    }

    public Note getNoteById(Long id) {
        return noteRepository.findById(id).orElse(null);
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public void shareNoteWithUser(Note note, User user) {
        if (user == null) {
            throw new IllegalArgumentException("User does not exist.");
        }
        SharedNote sharedNote = new SharedNote();
        sharedNote.setNote(note);
        sharedNote.setSharedWithUser(user);
        sharedNoteRepository.save(sharedNote);
    }

    public List<Note> getUserNotes(User user) {
        return noteRepository.findByUser(user);
    }

    public List<Note> getSharedNotes(User user) {
        List<SharedNote> sharedNotes = sharedNoteRepository.findBySharedWithUser(user);
        return sharedNotes.stream()
                .map(SharedNote::getNote)
                .collect(Collectors.toList());
    }
    public void shareNotePublicly(Note note) {
        note.setSharedPublicly(true);
        noteRepository.save(note);
    }

    public String readNote(Note note, String secretPassword) throws Exception {
        if (note.isEncrypted()) {
            return noteEncryptionService.decrypt(note.getContent(), secretPassword);
        } else {
            return note.getContent();
        }
    }

    public List<Note> getPublicNotes() {
        return noteRepository.findBySharedPublicly(true);
    }

}