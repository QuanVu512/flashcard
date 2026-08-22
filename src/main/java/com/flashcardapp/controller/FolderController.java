package com.flashcardapp.controller;

import com.flashcardapp.dto.FolderRequest;
import com.flashcardapp.dto.FolderResponse;
import com.flashcardapp.entity.Client;
import com.flashcardapp.service.FolderService;
import com.flashcardapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FolderController {

    private final UserService userService;
    private final FolderService folderService;

    public FolderController(UserService userService, FolderService folderService) {
        this.userService = userService;
        this.folderService = folderService;
    }

    @GetMapping("/api/folders")
    public List<FolderResponse> folders(Authentication authentication) {
        Client client = userService.currentClient(authentication.getName());
        return folderService.foldersFor(client).stream().map(FolderResponse::from).toList();
    }

    @PostMapping("/api/folders")
    public FolderResponse createFolder(@Valid @RequestBody FolderRequest request,
                                       Authentication authentication) {
        Client client = userService.currentClient(authentication.getName());
        return FolderResponse.from(folderService.create(client, request.name(), request.description()));
    }

    @PutMapping("/api/folders/{id}")
    public FolderResponse updateFolder(@PathVariable UUID id,
                                       @Valid @RequestBody FolderRequest request,
                                       Authentication authentication) {
        Client client = userService.currentClient(authentication.getName());
        return FolderResponse.from(folderService.update(client, id, request.name(), request.description()));
    }

    @DeleteMapping("/api/folders/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID id,
                                             Authentication authentication) {
        Client client = userService.currentClient(authentication.getName());
        folderService.delete(client, id);
        return ResponseEntity.noContent().build();
    }
}
