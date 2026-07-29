package com.flashcardapp.controller;

import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.entity.Folder;
import com.flashcardapp.service.LibraryService;
import com.flashcardapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
public class LibraryController {

    private final UserService userService;
    private final LibraryService libraryService;

    public LibraryController(UserService userService, LibraryService libraryService) {
        this.userService = userService;
        this.libraryService = libraryService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/library";
    }

    @GetMapping("/library")
    public String library(@RequestParam(value = "q", required = false) String keyword,
                          Authentication authentication,
                          Model model) {
        Client client = userService.currentClient(authentication.getName());
        List<FlashcardSet> sets = libraryService.setsFor(client, keyword);
        model.addAttribute("groupedSets", libraryService.groupByDate(sets));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("currentFolder", null);
        model.addAttribute("pageTitle", "Thư viện của bạn");
        return "library";
    }

    @GetMapping("/folders/{id}")
    public String folder(@PathVariable UUID id,
                         Authentication authentication,
                         Model model) {
        Client client = userService.currentClient(authentication.getName());
        Folder folder = libraryService.requireFolder(client, id);
        List<FlashcardSet> sets = libraryService.setsInFolder(client, folder);
        model.addAttribute("groupedSets", libraryService.groupByDate(sets));
        model.addAttribute("keyword", "");
        model.addAttribute("currentFolder", folder);
        model.addAttribute("pageTitle", folder.getName());
        return "library";
    }
}
