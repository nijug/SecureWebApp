package com.example.securenoteapp.controller;

import com.example.securenoteapp.service.CsrfTokenService;
import com.example.securenoteapp.service.NoteService;
import com.example.securenoteapp.model.data.Note;
import com.example.securenoteapp.model.data.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Controller
@RequestMapping("/home")
public class HomeController {

    private final NoteService noteService;

    private final CsrfTokenService csrfTokenService;

    @Autowired
    public HomeController(NoteService noteService, CsrfTokenService csrfTokenService) {
        this.noteService = noteService;
        this.csrfTokenService = csrfTokenService;
    }


    @GetMapping
    public String homePage(HttpSession session, Model model, HttpServletResponse response) throws InterruptedException {
        Thread.sleep(500);
        System.out.println("Home page requested...");
        response.addHeader("Content-Security-Policy", "script-src 'self'");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
        User user = (User) session.getAttribute("user");  // get the user object from the session
        csrfTokenService.generateAndStoreToken(session);
        if (user != null) {
            List<Note> userNotes = noteService.getUserNotes(user);
            List<Note> sharedNotes = noteService.getSharedNotes(user);
            List<Note> publicNotes = noteService.getPublicNotes();
            model.addAttribute("publicNotes", publicNotes);
            model.addAttribute("myNotes", userNotes);
            model.addAttribute("sharedNotes", sharedNotes);
        }
        else {
            return "redirect:/users/login";
        }
        return "home";
    }
    @PostMapping
    public String decryptNote(HttpSession session, Model model,
                              @ModelAttribute("noteId") Long noteId,
                              @ModelAttribute("secretPassword") String secretPassword,
                              @RequestParam String csrfToken) {
        if (!csrfTokenService.isTokenValid(session, csrfToken)) {
            throw new IllegalArgumentException("CSRF token does not match.");
        }
        User user = (User) session.getAttribute("user");
        if (user != null && noteId != null && secretPassword != null) {
            Note note = noteService.getNoteById(noteId);
            if (note != null && note.isEncrypted()) {
                try {
                    String content = noteService.readNote(note, secretPassword);
                    session.setAttribute("decryptedNote", content);
                    return "redirect:/home/note/" + noteId;
                } catch (Exception e) {
                    model.addAttribute("error", "Failed to read note: " + e.getMessage());
                }
            }
        }
        return "redirect:/home";
    }

    @GetMapping("/note/{noteId}")
    public String showDecryptedNote(@PathVariable Long noteId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            Note note = noteService.getNoteById(noteId);
            if (note != null && note.isEncrypted()) {
                String decryptedNote = (String) session.getAttribute("decryptedNote");
                model.addAttribute("decryptedNote", decryptedNote);
            }
        }
        return "note";
    }

}