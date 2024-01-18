package com.example.securenoteapp.controller;

import com.example.securenoteapp.service.CsrfTokenService;
import com.example.securenoteapp.service.NoteService;
import com.example.securenoteapp.model.data.Note;
import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.service.QRCodeService;
import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
@Controller
@RequestMapping("/home")
public class HomeController {

    private final NoteService noteService;

    private final CsrfTokenService csrfTokenService;

    private final QRCodeService qrCodeService;

    @Autowired
    public HomeController(NoteService noteService, CsrfTokenService csrfTokenService, QRCodeService qrCodeService) {
        this.noteService = noteService;
        this.csrfTokenService = csrfTokenService;
        this.qrCodeService = qrCodeService;
    }


    @GetMapping
    public String homePage(HttpSession session, Model model, HttpServletResponse response) throws InterruptedException {
        Thread.sleep(500);

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
                    // TODO: notatka powinna byc szyfrowana i odszyfrowywana na frontendzie
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

    @GetMapping("/credintials")
    public String showCredentials(Model model, HttpServletResponse response, HttpSession session, HttpServletRequest request) throws IOException, WriterException {
        if (session.getAttribute("user") == null) {
            return "redirect:/users/login";
        }
        User user = (User) session.getAttribute("user");
        String qrCodeData = "otpauth://totp/" + user.getUsername() + "?secret=" + user.getTotpSecret() + "&issuer=SecureNoteApp";

        byte[] qrCode = qrCodeService.generateQRCodeImage(qrCodeData, 200, 200);
        String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCode);
        // todo: nie wysylac calego requesta do frontendu, wysylac tylko url do redirecta
        model.addAttribute("httpServletRequest", request);
        model.addAttribute("qrCode", qrCodeBase64);
        model.addAttribute("totpSecret", user.getTotpSecret());
        model.addAttribute("recoveryKeys", user.getRecoveryKeys());

        return "showCredintials";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/users/login";
    }

}