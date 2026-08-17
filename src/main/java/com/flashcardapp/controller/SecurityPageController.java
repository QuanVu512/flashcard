package com.flashcardapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecurityPageController {

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
