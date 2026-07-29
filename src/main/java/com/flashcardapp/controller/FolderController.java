package com.flashcardapp.controller;

import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.Folder;
import com.flashcardapp.service.LibraryService;
import com.flashcardapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class FolderController {

    private final UserService userService;
    private final LibraryService libraryService;

    public FolderController(UserService userService, LibraryService libraryService) {
        this.userService = userService;
        this.libraryService = libraryService;
    }

    @PostMapping("/folders")
    public String createFolder(@RequestParam String name,
                               @RequestParam(required = false) String description,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("folderError", "Vui lòng nhập tên thư mục");
            return "redirect:/library";
        }
        Client client = userService.currentClient(authentication.getName());
        Folder folder = libraryService.createFolder(client, name, description);
        redirectAttributes.addFlashAttribute("folderCreated", folder.getName());
        return "redirect:/folders/" + folder.getId();
    }

    @PostMapping("/folders/{id}/edit")
    public String updateFolder(@PathVariable UUID id,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("folderError", "Vui lòng nhập tên thư mục");
            return "redirect:/folders/" + id;
        }
        Client client = userService.currentClient(authentication.getName());
        Folder folder = libraryService.updateFolder(client, id, name, description);
        redirectAttributes.addFlashAttribute("folderUpdated", folder.getName());
        return "redirect:/folders/" + folder.getId();
    }

    @PostMapping("/folders/{id}/delete")
    public String deleteFolder(@PathVariable UUID id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Client client = userService.currentClient(authentication.getName());
        libraryService.deleteFolder(client, id);
        redirectAttributes.addFlashAttribute("folderDeleted", true);
        return "redirect:/library";
    }
}
