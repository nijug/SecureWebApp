package com.example.securenoteapp.controller;

import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.service.CsrfTokenService;
import com.example.securenoteapp.service.QRCodeService;
import com.example.securenoteapp.service.UserService;
import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    private final CsrfTokenService csrfTokenService;

    private final QRCodeService qrCodeService;

    @Autowired
    public UserController(UserService userService, CsrfTokenService csrfTokenService, QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
        this.userService = userService;
        this.csrfTokenService = csrfTokenService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(Model model,HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/home";
        }
        csrfTokenService.generateAndStoreToken(session);
        model.addAttribute("user", new User());
        return "login";
    }


    @PostMapping("/login")
    public String login(@ModelAttribute("user") User user, @RequestParam("totp") String totp,@RequestParam String csrfToken ,Model model, HttpSession session) {
        try {
            if (!csrfTokenService.isTokenValid(session, csrfToken)) {
                throw new IllegalArgumentException("CSRF token does not match.");
            }
            User loggedInUser = userService.login(user, totp,session);
            // TODO: nie zapisywac calego obiektu usera w sesji, zamiast tego zapisywać samo id
            session.setAttribute("user", loggedInUser);
            return "redirect:/home";
        } catch (InterruptedException e) {
            model.addAttribute("error", "Login failed. Please try again.");
            return "login";
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }
    @GetMapping("/register")
    public String registerPage(Model model, HttpSession session) {
        // TODO: CSP powinien byc na cala strone a nie na tylko jednego page
        model.addAttribute("user", new User());
        csrfTokenService.generateAndStoreToken(session);
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user,@RequestParam String csrfToken, Model model,HttpSession session) {
        try {

            userService.register(user);
            session.setAttribute("user", user);
            if (!csrfTokenService.isTokenValid(session, csrfToken)) {
                throw new IllegalArgumentException("CSRF token does not match.");
            }
            return "redirect:/users/credintials";
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        catch (InterruptedException e) {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }
    }

    @GetMapping("/forgotPassword")
    public String showForgotPasswordForm(HttpSession session, HttpServletResponse response) {
        // TODO: csrf na cala strone a nie na jeden route
        csrfTokenService.generateAndStoreToken(session);
        return "forgotPassword";
    }

    @PostMapping("/forgotPassword")
    public String processForgotPasswordForm(@RequestParam String username,@RequestParam String csrfToken, HttpSession session, Model model) {
            if (!csrfTokenService.isTokenValid(session, csrfToken)) {
                throw new IllegalArgumentException("CSRF token does not match.");
            }
        try {
            String response = "http://localhost/users/resetPassword?token=" + userService.forgotPassword(username);
            System.out.println("Reset link: " + response);
            model.addAttribute("message", response);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (InterruptedException e) {
            model.addAttribute("error", "Password reset failed. Please try again.");
        }
        return "forgotPassword";
    }

    @GetMapping("/resetPassword")
    public String showResetPasswordForm(@RequestParam String token, Model model, HttpServletResponse response, HttpSession session) {
        csrfTokenService.generateAndStoreToken(session);
        response.addHeader("Content-Security-Policy", "script-src 'self'");
        model.addAttribute("token", token);
        return "resetPassword";
    }

    @PostMapping("/resetPassword")
    public String processResetPasswordForm(@RequestParam String token, @RequestParam String password, @RequestParam String csrfToken, HttpSession session, Model model) {
        if (!csrfTokenService.isTokenValid(session, csrfToken)) {
            throw new IllegalArgumentException("CSRF token does not match.");
        }
        try {
            userService.resetPassword(token, password);
            model.addAttribute("message", "Password successfully reset.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (InterruptedException e) {
            model.addAttribute("error", "Password reset failed. Please try again.");
        }
        return "resetPassword";
    }

    @GetMapping("/credintials")
    public String showCredentials(Model model, HttpServletResponse response, HttpSession session, HttpServletRequest request) throws IOException, WriterException {
        User user = (User) session.getAttribute("user");
        String qrCodeData = "otpauth://totp/" + user.getUsername() + "?secret=" + user.getTotpSecret() + "&issuer=YourAppName";

        byte[] qrCode = qrCodeService.generateQRCodeImage(qrCodeData, 200, 200);
        String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCode);

        model.addAttribute("httpServletRequest", request);
        model.addAttribute("qrCode", qrCodeBase64);
        model.addAttribute("totpSecret", user.getTotpSecret());
        model.addAttribute("recoveryKeys", user.getRecoveryKeys());
        session.removeAttribute("user");


        return "showCredintials";
    }
}


