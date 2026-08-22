package com.flashcardapp.service;

import com.flashcardapp.dto.CardLine;
import com.flashcardapp.dto.FlashcardSetForm;
import com.flashcardapp.dto.StudyCardView;
import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.Flashcard;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.entity.Folder;
import com.flashcardapp.helper.exception.ResourceNotFoundException;
import com.flashcardapp.repository.FlashcardSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FlashcardSetService {

    private final FlashcardSetRepository flashcardSetRepository;
    private final FolderService folderService;

    public FlashcardSetService(FlashcardSetRepository flashcardSetRepository,
                               FolderService folderService) {
        this.flashcardSetRepository = flashcardSetRepository;
        this.folderService = folderService;
    }

    @Transactional(readOnly = true)
    public List<FlashcardSet> findForLibrary(Client client, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return flashcardSetRepository
                    .findByClientAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(client, keyword.trim());
        }
        return flashcardSetRepository.findByClientOrderByCreatedAtDesc(client);
    }

    @Transactional(readOnly = true)
    public List<FlashcardSet> findInFolder(Client client, Folder folder) {
        return flashcardSetRepository.findByClientAndFolderOrderByCreatedAtDesc(client, folder);
    }

    @Transactional(readOnly = true)
    public FlashcardSet requireOwnedSet(Client client, UUID setId) {
        return flashcardSetRepository.findByIdAndClient(setId, client)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ flashcard"));
    }

    @Transactional
    public FlashcardSet create(Client client, FlashcardSetForm form) {
        FlashcardSet set = new FlashcardSet();
        set.setClient(client);
        applyFields(client, set, form);
        return flashcardSetRepository.save(set);
    }

    @Transactional
    public FlashcardSet update(Client client, UUID setId, FlashcardSetForm form) {
        FlashcardSet set = requireOwnedSet(client, setId);
        set.getCards().clear();
        applyFields(client, set, form);
        return flashcardSetRepository.save(set);
    }

    @Transactional
    public void delete(Client client, UUID setId) {
        flashcardSetRepository.delete(requireOwnedSet(client, setId));
    }

    @Transactional(readOnly = true)
    public FlashcardSetForm formFor(Client client, UUID setId) {
        FlashcardSet set = requireOwnedSet(client, setId);
        FlashcardSetForm form = new FlashcardSetForm();
        form.setTitle(set.getTitle());
        form.setDescription(set.getDescription());
        form.setFolderId(set.getFolder() == null ? null : set.getFolder().getId());
        form.setCards(new ArrayList<>(set.getCards().stream().map(this::toCardLine).toList()));
        ensureCardRows(form);
        return form;
    }

    public List<StudyCardView> toStudyCards(FlashcardSet set) {
        return set.getCards().stream()
                .map(this::toStudyCard)
                .toList();
    }

    public StudyCardView toStudyCard(Flashcard card) {
        return new StudyCardView(
                card.getTerm(),
                card.getDefinition(),
                card.getPhonetic(),
                card.getExample()
        );
    }

    public void ensureCardRows(FlashcardSetForm form) {
        if (form.getCards() == null) form.setCards(new ArrayList<>());
        while (form.getCards().size() < 4) {
            form.getCards().add(new CardLine());
        }
    }

    public Optional<String> validateCards(FlashcardSetForm form) {
        if (form.getCards() == null || form.getCards().isEmpty()) {
            return Optional.of("Vui lòng thêm ít nhất 1 flashcard");
        }
        boolean hasIncompleteCard = form.getCards().stream()
                .filter(CardLine::hasLearningContent)
                .anyMatch(line -> !line.isComplete());
        if (hasIncompleteCard) {
            return Optional.of("Mỗi flashcard cần có đầy đủ từ vựng và nghĩa");
        }
        boolean hasCompleteCard = form.getCards().stream().anyMatch(CardLine::isComplete);
        return hasCompleteCard
                ? Optional.empty()
                : Optional.of("Vui lòng thêm ít nhất 1 flashcard");
    }

    private void applyFields(Client client, FlashcardSet set, FlashcardSetForm form) {
        set.setTitle(form.getTitle().trim());
        set.setDescription(trimToNull(form.getDescription()));
        set.setFolder(form.getFolderId() == null
                ? null
                : folderService.requireOwnedFolder(client, form.getFolderId()));

        int position = 0;
        for (CardLine line : form.getCards()) {
            if (!line.isComplete()) continue;
            Flashcard card = new Flashcard();
            card.setTerm(line.getTerm().trim());
            card.setDefinition(line.getDefinition().trim());
            card.setPhonetic(trimToNull(line.getPhonetic()));
            card.setExample(trimToNull(line.getExample()));
            card.setPosition(position++);
            set.addCard(card);
        }
    }

    private CardLine toCardLine(Flashcard card) {
        CardLine line = new CardLine();
        line.setTerm(card.getTerm());
        line.setDefinition(card.getDefinition());
        line.setPhonetic(card.getPhonetic());
        line.setExample(card.getExample());
        return line;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
