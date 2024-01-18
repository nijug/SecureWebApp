package com.example.securenoteapp.controller;


import com.example.securenoteapp.MarkdownProcessor;
import com.example.securenoteapp.model.data.Note;
import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.service.CsrfTokenService;
import com.example.securenoteapp.service.NoteService;
import com.example.securenoteapp.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;
    private final UserService userService;

    private final CsrfTokenService csrfTokenService;

    @Autowired
    public NoteController(NoteService noteService, UserService userService, CsrfTokenService csrfTokenService) {
        this.noteService = noteService;
        this.userService = userService;
        this.csrfTokenService = csrfTokenService;
    }

    @GetMapping
    public ResponseEntity<List<Note>> getAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        return ResponseEntity.ok(notes);
    }

    @PostMapping("/add")
    public String createNote(Model model,
                             HttpSession session,
                             @RequestParam String content,
                             @RequestParam(required = false) String secretPassword,
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) Boolean sharePublicly,
                             @RequestParam String csrfToken) throws InterruptedException {
        Thread.sleep(500);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            model.addAttribute("error", "You must be logged in to add a note!");
            return "addNote";
        }

        if (!csrfTokenService.isTokenValid(session, csrfToken)) {
            throw new IllegalArgumentException("CSRF token does not match.");
        }


        if (secretPassword != null && !secretPassword.isEmpty() && (username != null && !username.isEmpty() || (sharePublicly != null && sharePublicly))) {
            model.addAttribute("error", "Encrypted notes cannot be shared.");
            return "addNote";
        }

        if (username != null && !username.isEmpty() && sharePublicly != null) {
            model.addAttribute("error", "Note cannot be shared both with a user and publicly.");
            return "addNote";
        }

        Note note = new Note();
        note.setUser(user);
        MarkdownProcessor markdownProcessor = new MarkdownProcessor();
        String htmlContent = markdownProcessor.parseMarkdown(content);
        note.setContent(htmlContent);

        try {
            noteService.createNote(note, secretPassword);

            if (username != null && !username.isEmpty()) {
                User shareWithUser = userService.getUserByUsername(username);
                if (shareWithUser != null) {
                    noteService.shareNoteWithUser(note, shareWithUser);
                } else {
                    model.addAttribute("error", "Failed to share note with user.");
                    return "addNote";
                }
            } else if (sharePublicly != null && sharePublicly) {
                noteService.shareNotePublicly(note);
            }

            model.addAttribute("message", "Note added successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to add note: " + e.getMessage());
            return "addNote";
        }

        return "redirect:/home";
    }


    @GetMapping("/add")
    public String addNotePage(HttpServletResponse response,  HttpSession session) throws InterruptedException {
        Thread.sleep(500);
        csrfTokenService.generateAndStoreToken(session);
        return "addNote";
    }

}