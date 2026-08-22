package com.flashcardapp.controller;

import com.flashcardapp.dto.FlashcardSetDetailResponse;
import com.flashcardapp.dto.FlashcardSetForm;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.service.FlashcardSetService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sets")
public class FlashcardSetController {

    private final UserService userService;
    private final FlashcardSetService flashcardSetService;

    public FlashcardSetController(UserService userService,
                                  FlashcardSetService flashcardSetService) {
        this.userService = userService;
        this.flashcardSetService = flashcardSetService;
    }

    @PostMapping
    public FlashcardSetDetailResponse create(@Valid @RequestBody FlashcardSetForm form,
                                             Authentication authentication) {
        validate(form);
        Client client = currentClient(authentication);
        return toDetail(flashcardSetService.create(client, form));
    }

    @GetMapping("/{id}")
    public FlashcardSetDetailResponse detail(@PathVariable UUID id,
                                             Authentication authentication) {
        return toDetail(flashcardSetService.requireOwnedSet(currentClient(authentication), id));
    }

    @GetMapping("/{id}/form")
    public FlashcardSetForm form(@PathVariable UUID id,
                                 Authentication authentication) {
        return flashcardSetService.formFor(currentClient(authentication), id);
    }

    @PutMapping("/{id}")
    public FlashcardSetDetailResponse update(@PathVariable UUID id,
                                             @Valid @RequestBody FlashcardSetForm form,
                                             Authentication authentication) {
        validate(form);
        return toDetail(flashcardSetService.update(currentClient(authentication), id, form));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       Authentication authentication) {
        flashcardSetService.delete(currentClient(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private Client currentClient(Authentication authentication) {
        return userService.currentClient(authentication.getName());
    }

    private FlashcardSetDetailResponse toDetail(FlashcardSet set) {
        return FlashcardSetDetailResponse.from(set, flashcardSetService.toStudyCards(set));
    }

    private void validate(FlashcardSetForm form) {
        flashcardSetService.ensureCardRows(form);
        flashcardSetService.validateCards(form).ifPresent(message -> {
            throw new IllegalArgumentException(message);
        });
    }
}
