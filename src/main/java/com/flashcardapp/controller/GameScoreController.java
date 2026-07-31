package com.flashcardapp.controller;

import com.flashcardapp.dto.GameScoreRequest;
import com.flashcardapp.dto.GameScoreResponse;
import com.flashcardapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameScoreController {

    private final UserService userService;

    public GameScoreController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/games/score")
    public GameScoreResponse addGameScore(@RequestBody GameScoreRequest request,
                                          Authentication authentication) {
        long addedScore = Math.max(0, Math.min(request.score(), 1_000_000));
        long totalScore = userService.addScore(authentication.getName(), addedScore);
        return new GameScoreResponse(addedScore, totalScore);
    }
}
