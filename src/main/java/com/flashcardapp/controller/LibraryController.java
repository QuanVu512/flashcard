package com.flashcardapp.controller;

import com.flashcardapp.dto.FlashcardSetSummaryResponse;
import com.flashcardapp.dto.FolderResponse;
import com.flashcardapp.dto.LibraryResponse;
import com.flashcardapp.dto.UserProfileResponse;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.entity.Folder;
import com.flashcardapp.service.FlashcardSetService;
import com.flashcardapp.service.FolderService;
import com.flashcardapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class LibraryController {

    private final UserService userService;
    private final FolderService folderService;
    private final FlashcardSetService flashcardSetService;

    public LibraryController(UserService userService,
                             FolderService folderService,
                             FlashcardSetService flashcardSetService) {
        this.userService = userService;
        this.folderService = folderService;
        this.flashcardSetService = flashcardSetService;
    }

    @GetMapping("/api/library")
    public LibraryResponse library(@RequestParam(value = "q", required = false) String keyword,
                                   Authentication authentication) {
        AppUser user = userService.currentUser(authentication.getName());
        Client client = user.getClient();
        return libraryResponse(user, flashcardSetService.findForLibrary(client, keyword));
    }

    @GetMapping("/api/folders/{id}/library")
    public LibraryResponse folderLibrary(@PathVariable UUID id,
                                         Authentication authentication) {
        AppUser user = userService.currentUser(authentication.getName());
        Client client = user.getClient();
        Folder folder = folderService.requireOwnedFolder(client, id);
        return libraryResponse(user, flashcardSetService.findInFolder(client, folder));
    }

    private LibraryResponse libraryResponse(AppUser user, List<FlashcardSet> sets) {
        Client client = user.getClient();
        return new LibraryResponse(
                UserProfileResponse.from(user),
                folderService.foldersFor(client).stream().map(FolderResponse::from).toList(),
                sets.stream().map(FlashcardSetSummaryResponse::from).toList()
        );
    }
}
