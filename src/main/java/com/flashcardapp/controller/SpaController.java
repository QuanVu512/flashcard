package com.flashcardapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/access-denied",
            "/library",
            "/admin",
            "/folders/{id}",
            "/sets/new",
            "/sets/{id}",
            "/sets/{id}/edit",
            "/sets/{id}/learn",
            "/sets/{id}/test/setup",
            "/sets/{id}/test",
            "/sets/{id}/flip"
    })
    public String index() {
        return "forward:/index.html";
    }
}
