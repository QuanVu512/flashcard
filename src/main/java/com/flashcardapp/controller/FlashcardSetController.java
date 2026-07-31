package com.flashcardapp.controller;

import com.flashcardapp.dto.FlashcardSetForm;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.service.LibraryService;
import com.flashcardapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class FlashcardSetController {

    private final UserService userService;
    private final LibraryService libraryService;

    public FlashcardSetController(UserService userService, LibraryService libraryService) {
        this.userService = userService;
        this.libraryService = libraryService;
    }

    @GetMapping("/sets/new")
    public String newSet(@RequestParam(required = false) UUID folderId,
                         Model model) {
        FlashcardSetForm form = new FlashcardSetForm();
        form.setFolderId(folderId);
        libraryService.ensureCardRows(form);
        model.addAttribute("flashcardSetForm", form);
        model.addAttribute("formMode", "create");
        model.addAttribute("formAction", "/sets");
        model.addAttribute("submitLabel", "Lưu");
        return "set-form";
    }

    @PostMapping("/sets")
    public String createSet(@Valid @ModelAttribute("flashcardSetForm") FlashcardSetForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model) {
        libraryService.validateCards(form).ifPresent(message -> bindingResult.reject("cards.invalid", message));
        if (bindingResult.hasErrors()) {
            libraryService.ensureCardRows(form);
            model.addAttribute("formMode", "create");
            model.addAttribute("formAction", "/sets");
            model.addAttribute("submitLabel", "Lưu");
            return "set-form";
        }
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.createSet(client, form);
        return "redirect:/sets/" + set.getId();
    }

    @GetMapping("/sets/{id}")
    public String study(@PathVariable UUID id,
                        Authentication authentication,
                        Model model) {
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.requireSet(client, id);
        model.addAttribute("set", set);
        model.addAttribute("studyCards", libraryService.studyCardsForSet(client, id));
        model.addAttribute("activeMode", "flashcards");
        return "study";
    }

    @GetMapping("/sets/{id}/learn")
    public String learn(@PathVariable UUID id,
                        Authentication authentication,
                        Model model) {
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.requireSet(client, id);
        model.addAttribute("set", set);
        model.addAttribute("questions", libraryService.learnQuestions(client, set));
        model.addAttribute("activeMode", "learn");
        return "learn";
    }

    @GetMapping("/sets/{id}/test/setup")
    public String testSetup(@PathVariable UUID id,
                            @RequestParam(defaultValue = LibraryService.TEST_MODE_MEANING) String testMode,
                            Authentication authentication,
                            Model model) {
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.requireSet(client, id);
        int maxQuestions = libraryService.maxPracticeQuestions(client, set);
        String safeTestMode = libraryService.normalizeTestMode(testMode);
        model.addAttribute("set", set);
        model.addAttribute("activeMode", "test");
        model.addAttribute("maxQuestions", maxQuestions);
        model.addAttribute("defaultQuestions", Math.min(maxQuestions, 10));
        model.addAttribute("defaultMinutes", 10);
        model.addAttribute("testMode", safeTestMode);
        model.addAttribute("meaningMode", LibraryService.TEST_MODE_MEANING);
        model.addAttribute("termMode", LibraryService.TEST_MODE_TERM);
        return "test-setup";
    }

    @GetMapping("/sets/{id}/test")
    public String test(@PathVariable UUID id,
                       @RequestParam(defaultValue = "10") int questionCount,
                       @RequestParam(defaultValue = "10") int minutes,
                       @RequestParam(defaultValue = LibraryService.TEST_MODE_MEANING) String testMode,
                       Authentication authentication,
                       Model model) {
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.requireSet(client, id);
        int maxQuestions = libraryService.maxPracticeQuestions(client, set);
        int safeQuestionCount = Math.max(1, Math.min(questionCount, maxQuestions));
        int safeMinutes = Math.max(1, Math.min(minutes, 180));
        String safeTestMode = libraryService.normalizeTestMode(testMode);

        model.addAttribute("set", set);
        model.addAttribute("questions", libraryService.testQuestions(client, set, safeQuestionCount, safeTestMode));
        model.addAttribute("activeMode", "test");
        model.addAttribute("minutes", safeMinutes);
        model.addAttribute("questionCount", safeQuestionCount);
        model.addAttribute("testMode", safeTestMode);
        model.addAttribute("questionLabel", libraryService.testQuestionLabel(safeTestMode));
        return "test";
    }

    @GetMapping("/sets/{id}/flip")
    public String flipGame(@PathVariable UUID id,
                           Authentication authentication,
                           Model model) {
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.requireSet(client, id);
        model.addAttribute("set", set);
        model.addAttribute("activeMode", "flip");
        model.addAttribute("gameCards", libraryService.gameCards(client, set));
        return "flip-game";
    }

    @GetMapping("/sets/{id}/edit")
    public String editSet(@PathVariable UUID id,
                          Authentication authentication,
                          Model model) {
        Client client = userService.currentClient(authentication.getName());
        FlashcardSet set = libraryService.requireSet(client, id);
        model.addAttribute("set", set);
        model.addAttribute("flashcardSetForm", libraryService.formForSet(client, id));
        model.addAttribute("formMode", "edit");
        model.addAttribute("formAction", "/sets/" + id + "/edit");
        model.addAttribute("submitLabel", "Lưu");
        return "set-form";
    }

    @PostMapping("/sets/{id}/edit")
    public String updateSet(@PathVariable UUID id,
                            @Valid @ModelAttribute("flashcardSetForm") FlashcardSetForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model) {
        libraryService.validateCards(form).ifPresent(message -> bindingResult.reject("cards.invalid", message));
        if (bindingResult.hasErrors()) {
            libraryService.ensureCardRows(form);
            model.addAttribute("formMode", "edit");
            model.addAttribute("formAction", "/sets/" + id + "/edit");
            model.addAttribute("submitLabel", "Lưu");
            return "set-form";
        }
        Client client = userService.currentClient(authentication.getName());
        libraryService.updateSet(client, id, form);
        return "redirect:/sets/" + id;
    }

    @PostMapping("/sets/{id}/delete")
    public String deleteSet(@PathVariable UUID id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        Client client = userService.currentClient(authentication.getName());
        libraryService.deleteSet(client, id);
        redirectAttributes.addFlashAttribute("setDeleted", true);
        return "redirect:/library";
    }
}
