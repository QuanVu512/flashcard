package com.flashcardapp.controller;

import com.flashcardapp.entity.Client;
import com.flashcardapp.helper.security.SecurityUtil;
import com.flashcardapp.service.LibraryService;
import com.flashcardapp.service.UserService;
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
    public void addShellData(Model model) {
        if (!SecurityUtil.isLoggedIn()) {
            return;
        }
        String username = SecurityUtil.currentUsername().orElse("");
        if (username.isBlank()) {
            return;
        }
        Client client = userService.currentClient(username);
        model.addAttribute("client", client);
        model.addAttribute("folders", libraryService.foldersFor(client));
    }
}
