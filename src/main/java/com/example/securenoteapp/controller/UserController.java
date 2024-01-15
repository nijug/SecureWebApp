package com.example.securenoteapp.controller;

import com.example.securenoteapp.model.data.User;
import com.example.securenoteapp.service.CsrfTokenService;
import com.example.securenoteapp.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    private final CsrfTokenService csrfTokenService;

    @Autowired
    public UserController(UserService userService, CsrfTokenService csrfTokenService) {
        this.userService = userService;
        this.csrfTokenService = csrfTokenService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("user") User user, Model model, HttpSession session) {
        try {
            User loggedInUser = userService.login(user);
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
    public String registerPage(Model model, HttpServletResponse response) {
        response.addHeader("Content-Security-Policy", "script-src 'self'");
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user, Model model) {
        try {
            userService.register(user, model);
            return "redirect:/users/login";
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
    public String showForgotPasswordForm(HttpSession session, HttpServletResponse response, Model model) {
        response.addHeader("Content-Security-Policy", "script-src 'self'");
        csrfTokenService.generateAndStoreToken(session);
        return "forgotPassword";
    }

    @PostMapping("/forgotPassword")
    public String processForgotPasswordForm(@RequestParam String username,@RequestParam String csrfToken, HttpSession session, Model model) {
            if (!csrfTokenService.isTokenValid(session, csrfToken)) {
                throw new IllegalArgumentException("CSRF token does not match.");
            }
        try {
            String response = "http://localhost:8080/users/resetPassword?token=" + userService.forgotPassword(username);
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
    public String showResetPasswordForm(@RequestParam String token, Model model, HttpServletResponse response) {
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

}


