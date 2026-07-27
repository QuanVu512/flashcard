package com.flashcardapp.controller;

import com.flashcardapp.entity.Client;
import com.flashcardapp.service.LibraryService;
import com.flashcardapp.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ShellModelAdvice {

    private final UserService userService;
    private final LibraryService libraryService;

    public ShellModelAdvice(UserService userService, LibraryService libraryService) {
        this.userService = userService;
        this.libraryService = libraryService;
    }

    @ModelAttribute
    public void addShellData(Model model, Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return;
        }
        Client client = userService.currentClient(authentication.getName());
        model.addAttribute("client", client);
        model.addAttribute("folders", libraryService.foldersFor(client));
    }
}
