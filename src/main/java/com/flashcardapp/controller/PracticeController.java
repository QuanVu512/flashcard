package com.flashcardapp.controller;

import com.flashcardapp.dto.FlashcardSetDetailResponse;
import com.flashcardapp.dto.FlashcardSetSummaryResponse;
import com.flashcardapp.dto.PracticeSessionResponse;
import com.flashcardapp.dto.TestSetupResponse;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.service.FlashcardSetService;
import com.flashcardapp.service.PracticeService;
import com.flashcardapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sets/{setId}")
public class PracticeController {

    private static final String ANSWER_MODE_CHOICE = "choice";
    private static final String ANSWER_MODE_WRITE = "write";

    private final UserService userService;
    private final FlashcardSetService flashcardSetService;
    private final PracticeService practiceService;

    public PracticeController(UserService userService,
                              FlashcardSetService flashcardSetService,
                              PracticeService practiceService) {
        this.userService = userService;
        this.flashcardSetService = flashcardSetService;
        this.practiceService = practiceService;
    }

    @GetMapping("/study")
    public FlashcardSetDetailResponse study(@PathVariable UUID setId,
                                            Authentication authentication) {
        FlashcardSet set = ownedSet(authentication, setId);
        return FlashcardSetDetailResponse.from(set, flashcardSetService.toStudyCards(set));
    }

    @GetMapping("/learn")
    public PracticeSessionResponse learn(
            @PathVariable UUID setId,
            @RequestParam(defaultValue = PracticeService.MODE_MEANING) String testMode,
            @RequestParam(defaultValue = ANSWER_MODE_CHOICE) String answerMode,
            Authentication authentication
    ) {
        Client client = currentClient(authentication);
        FlashcardSet set = flashcardSetService.requireOwnedSet(client, setId);
        String safeMode = practiceService.normalizeMode(testMode);
        return new PracticeSessionResponse(
                FlashcardSetSummaryResponse.from(set),
                safeMode,
                normalizeAnswerMode(answerMode),
                practiceService.maxQuestions(client, set),
                0,
                practiceService.questionLabel(safeMode),
                practiceService.learnQuestions(client, set, safeMode)
        );
    }

    @GetMapping("/test/setup")
    public TestSetupResponse testSetup(
            @PathVariable UUID setId,
            @RequestParam(defaultValue = PracticeService.MODE_MEANING) String testMode,
            Authentication authentication
    ) {
        Client client = currentClient(authentication);
        FlashcardSet set = flashcardSetService.requireOwnedSet(client, setId);
        int maxQuestions = practiceService.maxQuestions(client, set);
        return new TestSetupResponse(
                FlashcardSetSummaryResponse.from(set),
                maxQuestions,
                Math.min(maxQuestions, 10),
                10,
                practiceService.normalizeMode(testMode)
        );
    }

    @GetMapping("/test")
    public PracticeSessionResponse test(
            @PathVariable UUID setId,
            @RequestParam(defaultValue = "10") int questionCount,
            @RequestParam(defaultValue = "10") int minutes,
            @RequestParam(defaultValue = PracticeService.MODE_MEANING) String testMode,
            Authentication authentication
    ) {
        Client client = currentClient(authentication);
        FlashcardSet set = flashcardSetService.requireOwnedSet(client, setId);
        int maxQuestions = practiceService.maxQuestions(client, set);
        int safeQuestionCount = Math.max(1, Math.min(questionCount, maxQuestions));
        int safeMinutes = Math.max(1, Math.min(minutes, 180));
        String safeMode = practiceService.normalizeMode(testMode);
        return new PracticeSessionResponse(
                FlashcardSetSummaryResponse.from(set),
                safeMode,
                ANSWER_MODE_CHOICE,
                safeQuestionCount,
                safeMinutes,
                practiceService.questionLabel(safeMode),
                practiceService.testQuestions(client, set, safeQuestionCount, safeMode)
        );
    }

    @GetMapping("/flip")
    public FlashcardSetDetailResponse flip(@PathVariable UUID setId,
                                           Authentication authentication) {
        Client client = currentClient(authentication);
        FlashcardSet set = flashcardSetService.requireOwnedSet(client, setId);
        return FlashcardSetDetailResponse.from(set, practiceService.gameCards(client, set));
    }

    private FlashcardSet ownedSet(Authentication authentication, UUID setId) {
        return flashcardSetService.requireOwnedSet(currentClient(authentication), setId);
    }

    private Client currentClient(Authentication authentication) {
        return userService.currentClient(authentication.getName());
    }

    private String normalizeAnswerMode(String answerMode) {
        return ANSWER_MODE_WRITE.equalsIgnoreCase(answerMode)
                ? ANSWER_MODE_WRITE
                : ANSWER_MODE_CHOICE;
    }
}
