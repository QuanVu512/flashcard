package com.flashcardapp.service;

import com.flashcardapp.dto.PracticeQuestion;
import com.flashcardapp.dto.StudyCardView;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.Flashcard;
import com.flashcardapp.entity.FlashcardSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PracticeService {

    public static final String MODE_MEANING = "meaning";
    public static final String MODE_TERM = "term";

    private final FlashcardSetService flashcardSetService;

    public PracticeService(FlashcardSetService flashcardSetService) {
        this.flashcardSetService = flashcardSetService;
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestion> learnQuestions(Client client,
                                                 FlashcardSet set,
                                                 String testMode) {
        List<Flashcard> cards = cardsForPractice(client, set);
        Collections.shuffle(cards);
        return toQuestions(cards, cards, testMode);
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestion> testQuestions(Client client,
                                                FlashcardSet set,
                                                int questionCount,
                                                String testMode) {
        List<Flashcard> cards = cardsForPractice(client, set);
        Collections.shuffle(cards);
        return toQuestions(cards.stream().limit(questionCount).toList(), cards, testMode);
    }

    @Transactional(readOnly = true)
    public int maxQuestions(Client client, FlashcardSet set) {
        return cardsForPractice(client, set).size();
    }

    @Transactional(readOnly = true)
    public List<StudyCardView> gameCards(Client client, FlashcardSet set) {
        return cardsForPractice(client, set).stream()
                .map(flashcardSetService::toStudyCard)
                .toList();
    }

    public String normalizeMode(String testMode) {
        return MODE_TERM.equalsIgnoreCase(testMode) ? MODE_TERM : MODE_MEANING;
    }

    public String questionLabel(String testMode) {
        return MODE_TERM.equals(normalizeMode(testMode)) ? "Nghĩa" : "Từ vựng";
    }

    private List<Flashcard> cardsForPractice(Client client, FlashcardSet set) {
        if (set.getFolder() == null) return new ArrayList<>(set.getCards());
        return flashcardSetService.findInFolder(client, set.getFolder()).stream()
                .flatMap(folderSet -> folderSet.getCards().stream())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<PracticeQuestion> toQuestions(List<Flashcard> selectedCards,
                                               List<Flashcard> allCards,
                                               String testMode) {
        if (MODE_TERM.equals(normalizeMode(testMode))) {
            List<String> allTerms = allCards.stream()
                    .map(Flashcard::getTerm)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList();
            return selectedCards.stream()
                    .map(card -> new PracticeQuestion(
                            card.getDefinition(),
                            card.getTerm(),
                            choicesFor(card.getTerm(), allTerms)
                    ))
                    .toList();
        }

        List<String> allDefinitions = allCards.stream()
                .map(Flashcard::getDefinition)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return selectedCards.stream()
                .map(card -> new PracticeQuestion(
                        card.getTerm(),
                        card.getDefinition(),
                        choicesFor(card.getDefinition(), allDefinitions)
                ))
                .toList();
    }

    private List<String> choicesFor(String correctAnswer, List<String> allAnswers) {
        List<String> wrongChoices = allAnswers.stream()
                .filter(answer -> !answer.equals(correctAnswer))
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(wrongChoices);

        List<String> choices = new ArrayList<>();
        choices.add(correctAnswer);
        choices.addAll(wrongChoices.stream().limit(3).toList());
        Collections.shuffle(choices);
        return choices;
    }
}
