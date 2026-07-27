package com.flashcardapp.service;

import com.flashcardapp.dto.CardLine;
import com.flashcardapp.dto.FlashcardSetForm;
import com.flashcardapp.dto.PracticeQuestion;
import com.flashcardapp.dto.StudyCardView;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.Flashcard;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.entity.Folder;
import com.flashcardapp.repository.FlashcardSetRepository;
import com.flashcardapp.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LibraryService {

    private final FolderRepository folderRepository;
    private final FlashcardSetRepository flashcardSetRepository;

    public LibraryService(FolderRepository folderRepository, FlashcardSetRepository flashcardSetRepository) {
        this.folderRepository = folderRepository;
        this.flashcardSetRepository = flashcardSetRepository;
    }

    @Transactional(readOnly = true)
    public List<Folder> foldersFor(Client client) {
        return folderRepository.findByClientOrderByCreatedAtDesc(client);
    }

    @Transactional
    public Folder createFolder(Client client, String name, String description) {
        Folder folder = new Folder();
        folder.setClient(client);
        folder.setName(name.trim());
        folder.setDescription(trimToNull(description));
        return folderRepository.save(folder);
    }

    @Transactional(readOnly = true)
    public Folder requireFolder(Client client, UUID folderId) {
        return folderRepository.findByIdAndClient(folderId, client)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thư mục"));
    }

    @Transactional(readOnly = true)
    public List<FlashcardSet> setsFor(Client client, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return flashcardSetRepository.findByClientAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(client, keyword.trim());
        }
        return flashcardSetRepository.findByClientOrderByCreatedAtDesc(client);
    }

    @Transactional(readOnly = true)
    public List<FlashcardSet> setsInFolder(Client client, Folder folder) {
        return flashcardSetRepository.findByClientAndFolderOrderByCreatedAtDesc(client, folder);
    }

    @Transactional(readOnly = true)
    public FlashcardSet requireSet(Client client, UUID setId) {
        return flashcardSetRepository.findByIdAndClient(setId, client)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ flashcard"));
    }

    @Transactional
    public FlashcardSet createSet(Client client, FlashcardSetForm form) {
        FlashcardSet set = new FlashcardSet();
        set.setClient(client);
        applySetFields(client, set, form);
        return flashcardSetRepository.save(set);
    }

    @Transactional
    public FlashcardSet updateSet(Client client, UUID setId, FlashcardSetForm form) {
        FlashcardSet set = requireSet(client, setId);
        set.getCards().clear();
        applySetFields(client, set, form);
        return flashcardSetRepository.save(set);
    }

    @Transactional
    public void deleteSet(Client client, UUID setId) {
        FlashcardSet set = requireSet(client, setId);
        flashcardSetRepository.delete(set);
    }

    public FlashcardSetForm toForm(FlashcardSet set) {
        FlashcardSetForm form = new FlashcardSetForm();
        form.setTitle(set.getTitle());
        form.setDescription(set.getDescription());
        form.setFolderId(set.getFolder() == null ? null : set.getFolder().getId());
        form.setCards(set.getCards().stream().map(card -> {
            CardLine line = new CardLine();
            line.setTerm(card.getTerm());
            line.setDefinition(card.getDefinition());
            line.setExample(card.getExample());
            return line;
        }).toList());
        ensureCardRows(form);
        return form;
    }

    public List<StudyCardView> studyCards(FlashcardSet set) {
        return set.getCards().stream()
                .map(card -> new StudyCardView(card.getTerm(), card.getDefinition(), card.getExample()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestion> learnQuestions(Client client, FlashcardSet set) {
        List<Flashcard> cards = cardsForPractice(client, set);
        Collections.shuffle(cards);
        return toPracticeQuestions(cards);
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestion> testQuestions(Client client, FlashcardSet set, int questionCount) {
        List<Flashcard> cards = cardsForPractice(client, set);
        Collections.shuffle(cards);
        return toPracticeQuestions(cards.stream().limit(questionCount).toList());
    }

    @Transactional(readOnly = true)
    public int maxPracticeQuestions(Client client, FlashcardSet set) {
        return cardsForPractice(client, set).size();
    }

    public Map<String, List<FlashcardSet>> groupByDate(List<FlashcardSet> sets) {
        Map<String, List<FlashcardSet>> grouped = new LinkedHashMap<>();
        LocalDateTime today = LocalDateTime.now().minusDays(1);
        for (FlashcardSet set : sets) {
            String key = set.getCreatedAt().isAfter(today)
                    ? "GẦN ĐÂY"
                    : monthLabel(set.getCreatedAt().getMonth(), set.getCreatedAt().getYear());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(set);
        }
        return grouped;
    }

    public void ensureCardRows(FlashcardSetForm form) {
        if (form.getCards() == null) {
            form.setCards(new ArrayList<>());
        }
        while (form.getCards().size() < 4) {
            form.getCards().add(new CardLine());
        }
    }

    public Optional<String> validateCards(FlashcardSetForm form) {
        if (form.getCards() == null || form.getCards().isEmpty()) {
            return Optional.of("Vui lòng thêm ít nhất 1 flashcard");
        }
        long completeCards = form.getCards().stream()
                .filter(CardLine::hasLearningContent)
                .filter(CardLine::isComplete)
                .count();
        boolean hasIncompleteCard = form.getCards().stream()
                .filter(CardLine::hasLearningContent)
                .anyMatch(line -> !line.isComplete());

        if (hasIncompleteCard) {
            return Optional.of("Mỗi flashcard cần có đầy đủ từ vựng và nghĩa");
        }
        if (completeCards == 0) {
            return Optional.of("Vui lòng thêm ít nhất 1 flashcard");
        }
        return Optional.empty();
    }

    private void applySetFields(Client client, FlashcardSet set, FlashcardSetForm form) {
        set.setTitle(form.getTitle().trim());
        set.setDescription(trimToNull(form.getDescription()));
        if (form.getFolderId() == null) {
            set.setFolder(null);
        } else {
            set.setFolder(requireFolder(client, form.getFolderId()));
        }

        int position = 0;
        for (CardLine line : form.getCards()) {
            if (!line.isComplete()) {
                continue;
            }
            Flashcard card = new Flashcard();
            card.setTerm(line.getTerm().trim());
            card.setDefinition(line.getDefinition().trim());
            card.setExample(trimToNull(line.getExample()));
            card.setPosition(position++);
            set.addCard(card);
        }
    }

    private String monthLabel(java.time.Month month, int year) {
        return "THÁNG " + month.getValue() + " NĂM " + year;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private List<Flashcard> cardsForPractice(Client client, FlashcardSet set) {
        if (set.getFolder() == null) {
            return new ArrayList<>(set.getCards());
        }

        return setsInFolder(client, set.getFolder()).stream()
                .flatMap(folderSet -> folderSet.getCards().stream())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<PracticeQuestion> toPracticeQuestions(List<Flashcard> cards) {
        List<String> allDefinitions = cards.stream()
                .map(Flashcard::getDefinition)
                .filter(definition -> definition != null && !definition.isBlank())
                .distinct()
                .toList();

        return cards.stream()
                .map(card -> new PracticeQuestion(
                        card.getTerm(),
                        card.getDefinition(),
                        choicesFor(card.getDefinition(), allDefinitions)
                ))
                .toList();
    }

    private List<String> choicesFor(String correctAnswer, List<String> allDefinitions) {
        List<String> wrongChoices = allDefinitions.stream()
                .filter(definition -> !definition.equals(correctAnswer))
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
