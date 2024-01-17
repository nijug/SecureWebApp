package com.example.securenoteapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @PostMapping
    public void honeypot() {
    }
    @GetMapping
    public String GetAdmin() {
        return "admin";
    }
}