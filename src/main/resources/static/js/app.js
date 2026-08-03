(function () {
    const cardRows = document.querySelector("#cardRows");
    const REAL_CARD_NAME = "cardInputName";
    const REAL_CARD_NAME_ATTR = "data-card-input-name";

    function applyInputHints(input) {
        input.setAttribute("autocomplete", "off");
        input.setAttribute("autocapitalize", "off");
        input.setAttribute("autocorrect", "off");
        input.setAttribute("spellcheck", "false");
        input.setAttribute("aria-autocomplete", "none");
        input.setAttribute("data-lpignore", "true");
        input.setAttribute("data-1p-ignore", "true");
    }

    function prepareCardInput(input) {
        applyInputHints(input);
        if (!input.dataset[REAL_CARD_NAME] && input.name) {
            input.dataset[REAL_CARD_NAME] = input.name;
        }
        refreshCardInputId(input);
        if (input.dataset[REAL_CARD_NAME]) {
            input.removeAttribute("name");
        }
    }

    function prepareCardRow(row) {
        row.querySelectorAll("input").forEach((input) => prepareCardInput(input));
    }

    function restoreCardInputNames(scope) {
        scope.querySelectorAll(`input[${REAL_CARD_NAME_ATTR}]`).forEach((input) => {
            input.name = input.dataset[REAL_CARD_NAME];
        });
    }

    function refreshCardInputId(input) {
        if (!cardRows) {
            return;
        }
        const row = input.closest("[data-card-row]");
        if (!row) {
            return;
        }
        if (!input.dataset.cardInputUid) {
            input.dataset.cardInputUid = Math.random().toString(36).slice(2, 10);
        }
        const rowIndex = Array.from(cardRows.querySelectorAll("[data-card-row]")).indexOf(row);
        const kind = cardInputKind(input);
        const id = `fc-${kind}-${Math.max(rowIndex, 0)}-${input.dataset.cardInputUid}`;
        input.id = id;
        input.closest("div")?.querySelector("label")?.setAttribute("for", id);
    }

    function cardInputKind(input) {
        const source = `${input.dataset[REAL_CARD_NAME] || ""} ${input.id || ""}`;
        if (source.includes("phonetic")) {
            return "phonetic";
        }
        if (source.includes("definition")) {
            return "definition";
        }
        if (source.includes("example")) {
            return "example";
        }
        return "term";
    }

    function updateCardInputName(input, index) {
        const currentName = input.dataset[REAL_CARD_NAME] || input.name;
        if (!currentName) {
            return;
        }
        const nextName = currentName.replace(/cards\[\d+]/, `cards[${index}]`);
        input.dataset[REAL_CARD_NAME] = nextName;
        if (input.hasAttribute("name")) {
            input.name = nextName;
        }
    }

    function cardRowTemplate(index) {
        return `
            <div class="term-row" data-card-row>
                <div class="term-number">${index + 1}</div>
                <div class="term-fields">
                    <div>
                        <label class="form-label" for="card-term-${index}">Từ vựng</label>
                        <input class="form-control" id="card-term-${index}" data-card-input-name="cards[${index}].term" data-translation-term autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false" aria-autocomplete="none" data-lpignore="true" data-1p-ignore="true" placeholder="Nhập từ vựng">
                    </div>
                    <div>
                        <label class="form-label" for="card-phonetic-${index}">Phiên âm</label>
                        <input class="form-control" id="card-phonetic-${index}" data-card-input-name="cards[${index}].phonetic" data-translation-phonetic autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false" aria-autocomplete="none" data-lpignore="true" data-1p-ignore="true" placeholder="Nhập phiên âm nếu có">
                    </div>
                    <div>
                        <label class="form-label" for="card-definition-${index}">Nghĩa</label>
                        <input class="form-control" id="card-definition-${index}" data-card-input-name="cards[${index}].definition" data-translation-definition autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false" aria-autocomplete="none" data-lpignore="true" data-1p-ignore="true" placeholder="Nhập nghĩa">
                    </div>
                    <div>
                        <label class="form-label" for="card-example-${index}">Ví dụ</label>
                        <input class="form-control" id="card-example-${index}" data-card-input-name="cards[${index}].example" autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false" aria-autocomplete="none" data-lpignore="true" data-1p-ignore="true" placeholder="Nhập ví dụ nếu có">
                    </div>
                </div>
                <button class="icon-button remove-row" type="button" data-remove-card aria-label="Xóa thẻ">
                    <i class="bi bi-trash3"></i>
                </button>
            </div>
        `;
    }

    function shuffle(items) {
        for (let index = items.length - 1; index > 0; index -= 1) {
            const randomIndex = Math.floor(Math.random() * (index + 1));
            [items[index], items[randomIndex]] = [items[randomIndex], items[index]];
        }
        return items;
    }

    function reindexRows() {
        if (!cardRows) {
            return;
        }
        cardRows.querySelectorAll("[data-card-row]").forEach((row, index) => {
            row.querySelector(".term-number").textContent = String(index + 1);
            row.querySelectorAll("input").forEach((input) => {
                updateCardInputName(input, index);
                if (input.id.includes("term")) {
                    input.id = `card-term-${index}`;
                }
                if (input.id.includes("phonetic")) {
                    input.id = `card-phonetic-${index}`;
                }
                if (input.id.includes("definition")) {
                    input.id = `card-definition-${index}`;
                }
                if (input.id.includes("example")) {
                    input.id = `card-example-${index}`;
                }
            });
            row.querySelectorAll("label").forEach((label) => {
                const text = label.textContent.trim();
                if (text === "Từ vựng") {
                    label.setAttribute("for", `card-term-${index}`);
                }
                if (text === "Phiên âm") {
                    label.setAttribute("for", `card-phonetic-${index}`);
                }
                if (text === "Nghĩa") {
                    label.setAttribute("for", `card-definition-${index}`);
                }
                if (text === "Ví dụ") {
                    label.setAttribute("for", `card-example-${index}`);
                }
            });
            prepareCardRow(row);
        });
    }

    document.addEventListener("click", (event) => {
        const addButton = event.target.closest("[data-add-card]");
        if (addButton && cardRows) {
            const index = cardRows.querySelectorAll("[data-card-row]").length;
            cardRows.insertAdjacentHTML("beforeend", cardRowTemplate(index));
            const newestRow = cardRows.querySelector("[data-card-row]:last-child");
            if (newestRow) {
                prepareCardRow(newestRow);
            }
            const newest = newestRow?.querySelector("input");
            if (newest) {
                newest.focus();
            }
        }

        const removeButton = event.target.closest("[data-remove-card]");
        if (removeButton && cardRows) {
            const rows = cardRows.querySelectorAll("[data-card-row]");
            if (rows.length > 1) {
                removeButton.closest("[data-card-row]").remove();
                reindexRows();
            }
        }
    });

    document.querySelectorAll("form[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const message = form.getAttribute("data-confirm") || "Bạn chắc chắn muốn tiếp tục?";
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll(".set-form").forEach((form) => {
        form.addEventListener("submit", (event) => {
            clearCardValidationErrors(form);
            if (!validateCardRows(form)) {
                event.preventDefault();
                return;
            }
            restoreCardInputNames(form);
        });
    });

    setupFlashcards();
    setupPractice();
    setupFlipGame();
    setupCardEntryFields();
    setupTranslationSuggestions();

    function clearCardValidationErrors(form) {
        form.querySelectorAll(".client-card-error").forEach((error) => error.remove());
        form.querySelectorAll(".is-invalid").forEach((input) => input.classList.remove("is-invalid"));
    }

    function clearInputValidationError(input) {
        input.classList.remove("is-invalid");
        input.parentElement?.querySelectorAll(".client-card-error").forEach((error) => error.remove());
    }

    function validateCardRows(form) {
        if (!cardRows || !form.contains(cardRows)) {
            return true;
        }

        let completeCards = 0;
        let firstInvalid = null;
        const rows = Array.from(cardRows.querySelectorAll("[data-card-row]"));
        rows.forEach((row) => {
            const term = row.querySelector("[data-translation-term]");
            const phonetic = row.querySelector("[data-translation-phonetic]");
            const definition = row.querySelector("[data-translation-definition]");
            const example = row.querySelector("input[id*='example']");
            const hasContent = [term, phonetic, definition, example].some((input) => input?.value.trim());
            const hasTerm = Boolean(term?.value.trim());
            const hasDefinition = Boolean(definition?.value.trim());

            if (hasTerm && hasDefinition) {
                completeCards += 1;
                return;
            }
            if (!hasContent) {
                return;
            }

            if (!hasTerm) {
                firstInvalid = firstInvalid || term;
                showCardValidationError(term, "Vui lòng nhập từ vựng hoặc xóa thẻ này.");
            }
            if (!hasDefinition) {
                firstInvalid = firstInvalid || definition;
                showCardValidationError(definition, "Vui lòng nhập nghĩa hoặc xóa thẻ này.");
            }
        });

        if (completeCards === 0 && !firstInvalid) {
            const firstRow = rows[0];
            const firstTerm = firstRow?.querySelector("[data-translation-term]");
            const firstDefinition = firstRow?.querySelector("[data-translation-definition]");
            firstInvalid = firstTerm || firstDefinition;
            showCardValidationError(firstTerm, "Vui lòng nhập từ vựng.");
            showCardValidationError(firstDefinition, "Vui lòng nhập nghĩa.");
        }

        if (firstInvalid) {
            firstInvalid.focus();
            firstInvalid.scrollIntoView({behavior: "smooth", block: "center"});
            return false;
        }
        return true;
    }

    function showCardValidationError(input, message) {
        if (!input) {
            return;
        }
        input.classList.add("is-invalid");
        const error = document.createElement("div");
        error.className = "field-error client-card-error";
        error.textContent = message;
        input.insertAdjacentElement("afterend", error);
    }

    function setupCardEntryFields() {
        if (!cardRows) {
            return;
        }

        function configureRow(row) {
            prepareCardRow(row);
        }

        cardRows.querySelectorAll("[data-card-row]").forEach((row) => configureRow(row));
        cardRows.addEventListener("focusin", (event) => {
            const row = event.target.closest("[data-card-row]");
            if (row) {
                configureRow(row);
            }
        });
    }

    function setupTranslationSuggestions() {
        if (!cardRows) {
            return;
        }

        const timers = new WeakMap();
        const requestTokens = new WeakMap();
        const MIN_TEXT_LENGTH = 2;
        const DEBOUNCE_MS = 650;
        const REQUEST_TIMEOUT_MS = 12000;

        function termInput(row) {
            return row.querySelector("[data-translation-term], input[name$='.term']");
        }

        function definitionInput(row) {
            return row.querySelector("[data-translation-definition], input[name$='.definition']");
        }

        function phoneticInput(row) {
            return row.querySelector("[data-translation-phonetic], input[name$='.phonetic']");
        }

        function configureRow(row) {
            const term = termInput(row);
            const definition = definitionInput(row);
            const phonetic = phoneticInput(row);
            if (term) {
                term.setAttribute("data-translation-term", "");
            }
            if (phonetic) {
                phonetic.setAttribute("data-translation-phonetic", "");
                suggestionBox(phonetic);
            }
            if (definition) {
                definition.setAttribute("data-translation-definition", "");
                suggestionBox(definition);
            }
        }

        function suggestionBox(input) {
            if (!input) {
                return null;
            }
            const field = input.parentElement;
            field.classList.add("translation-field");
            let box = field.querySelector("[data-translation-suggestions]");
            if (!box) {
                box = document.createElement("div");
                box.className = "translation-suggestions";
                box.setAttribute("data-translation-suggestions", "");
                box.hidden = true;
                input.insertAdjacentElement("afterend", box);
            }
            return box;
        }

        function hideSuggestionBox(input) {
            const box = suggestionBox(input);
            if (box) {
                box.hidden = true;
                box.innerHTML = "";
            }
        }

        function hideSuggestions(row) {
            hideSuggestionBox(definitionInput(row));
            hideSuggestionBox(phoneticInput(row));
        }

        function showStateForInput(input, message, autoHide = false) {
            const box = suggestionBox(input);
            if (!box) {
                return;
            }
            box.innerHTML = "";
            const state = document.createElement("div");
            state.className = "translation-suggestion-state";
            state.textContent = message;
            box.appendChild(state);
            box.hidden = false;
            if (autoHide) {
                window.setTimeout(() => {
                    if (box.contains(state)) {
                        hideSuggestionBox(input);
                    }
                }, 3600);
            }
        }

        function showState(row, message, autoHide = false) {
            showStateForInput(definitionInput(row), message, autoHide);
        }

        function showPhoneticState(row, message, autoHide = false) {
            showStateForInput(phoneticInput(row), message, autoHide);
        }

        function renderSuggestions(row, payload) {
            const definition = definitionInput(row);
            const phonetic = phoneticInput(row);
            if (!definition) {
                return;
            }

            const suggestions = Array.isArray(payload?.suggestions) ? payload.suggestions : [];
            const phoneticSuggestions = Array.isArray(payload?.phoneticSuggestions) ? payload.phoneticSuggestions : [];
            if (!payload?.enabled) {
                showState(row, payload?.message || "Chưa bật gợi ý dịch.", true);
                showPhoneticState(row, payload?.message || "Chưa bật gợi ý phiên âm.", true);
                return;
            }
            if (!suggestions.length && !phoneticSuggestions.length) {
                if (payload?.message) {
                    showState(row, payload.message, true);
                    showPhoneticState(row, payload?.phoneticMessage || "Chưa có gợi ý phiên âm.", true);
                    return;
                }
                hideSuggestions(row);
                return;
            }

            renderSuggestionList(
                definition,
                suggestions,
                payload.detectedLanguage
                    ? `Gợi ý nghĩa (${payload.detectedLanguage} -> ${payload.targetLanguage})`
                    : "Gợi ý nghĩa",
                row
            );
            renderSuggestionList(phonetic, phoneticSuggestions, "Gợi ý phiên âm", row);
            if (!phoneticSuggestions.length && payload?.phoneticMessage && phonetic && !phonetic.value.trim()) {
                showPhoneticState(row, payload.phoneticMessage, true);
            }
        }

        function renderSuggestionList(input, suggestions, title, row) {
            const box = suggestionBox(input);
            if (!box || !input || !suggestions.length) {
                hideSuggestionBox(input);
                return;
            }

            box.innerHTML = "";
            const label = document.createElement("div");
            label.className = "translation-suggestion-label";
            label.textContent = title;
            box.appendChild(label);

            suggestions.forEach((suggestion) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "translation-suggestion-item";
                button.textContent = suggestion;
                button.addEventListener("click", () => {
                    input.value = suggestion;
                    hideSuggestionBox(input);
                    input.focus();
                });
                box.appendChild(button);
            });
            box.hidden = false;
        }

        async function requestSuggestion(text) {
            const controller = new AbortController();
            const timeoutId = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
            try {
                const params = new URLSearchParams({text});
                const response = await fetch(`/api/translation/suggest?${params.toString()}`, {
                    headers: {
                        Accept: "application/json"
                    },
                    credentials: "same-origin",
                    cache: "no-store",
                    signal: controller.signal
                });
                return response.ok ? response.json() : null;
            } finally {
                window.clearTimeout(timeoutId);
            }
        }

        async function fetchSuggestion(row, text) {
            const token = Symbol("translation-request");
            requestTokens.set(row, token);
            showState(row, "Đang tìm nghĩa...");
            showPhoneticState(row, "Đang tìm phiên âm...");

            try {
                let payload = await requestSuggestion(text);
                if (!payload) {
                    await new Promise((resolve) => window.setTimeout(resolve, 900));
                    payload = await requestSuggestion(text);
                }
                if (!payload) {
                    showState(row, "Dịch đang phản hồi chậm. Thử lại sau một chút.", true);
                    showPhoneticState(row, "Phiên âm đang phản hồi chậm. Thử lại sau một chút.", true);
                    return;
                }
                if (requestTokens.get(row) === token) {
                    renderSuggestions(row, payload);
                }
            } catch (error) {
                if (requestTokens.get(row) === token) {
                    showState(row, "Dịch đang phản hồi chậm. Thử lại sau một chút.", true);
                    showPhoneticState(row, "Phiên âm đang phản hồi chậm. Thử lại sau một chút.", true);
                }
            }
        }

        function scheduleSuggestion(row) {
            configureRow(row);
            const term = termInput(row);
            const definition = definitionInput(row);
            const phonetic = phoneticInput(row);
            if (!term || !definition) {
                return;
            }
            const text = term.value.trim();
            const hasSuggestionsTarget = !definition.value.trim() || (phonetic && !phonetic.value.trim());
            if (!hasSuggestionsTarget && document.activeElement !== term) {
                hideSuggestions(row);
                return;
            }
            if (text.length < MIN_TEXT_LENGTH) {
                hideSuggestions(row);
                return;
            }

            window.clearTimeout(timers.get(row));
            const timerId = window.setTimeout(() => fetchSuggestion(row, text), DEBOUNCE_MS);
            timers.set(row, timerId);
        }

        cardRows.querySelectorAll("[data-card-row]").forEach((row) => configureRow(row));
        cardRows.addEventListener("input", (event) => {
            const row = event.target.closest("[data-card-row]");
            if (!row) {
                return;
            }
            clearInputValidationError(event.target);
            configureRow(row);
            if (event.target === termInput(row)) {
                scheduleSuggestion(row);
            }
            if (event.target === definitionInput(row) || event.target === phoneticInput(row)) {
                hideSuggestionBox(event.target);
            }
        });
        cardRows.addEventListener("focusin", (event) => {
            const row = event.target.closest("[data-card-row]");
            if (!row) {
                return;
            }
            configureRow(row);
            if (event.target === termInput(row)) {
                scheduleSuggestion(row);
            }
        });
        document.addEventListener("click", (event) => {
            if (event.target.closest(".translation-field")) {
                return;
            }
            cardRows.querySelectorAll("[data-card-row]").forEach((row) => hideSuggestions(row));
        });
    }

    function setupFlashcards() {
        const stage = document.querySelector("[data-study-stage]");
        if (!stage) {
            return;
        }

        let cards = Array.isArray(window.studyCards) ? [...window.studyCards] : [];
        const term = document.querySelector("[data-card-term]");
        const phonetic = document.querySelector("[data-card-phonetic]");
        const phoneticWrap = document.querySelector("[data-card-phonetic-wrap]");
        const definition = document.querySelector("[data-card-definition]");
        const example = document.querySelector("[data-card-example]");
        const exampleWrap = document.querySelector("[data-card-example-wrap]");
        const counter = document.querySelector("[data-card-counter]");
        const progressFill = document.querySelector("[data-progress-fill]");
        const prevButton = document.querySelector("[data-prev-card]");
        const nextButton = document.querySelector("[data-next-card]");
        const flipButton = document.querySelector("[data-flip-card]");
        const shuffleButton = document.querySelector("[data-shuffle-cards]");
        let currentIndex = 0;
        let flipped = false;

        function renderStudyCard() {
            if (!cards.length) {
                term.textContent = "Chưa có thẻ";
                phoneticWrap.hidden = true;
                definition.textContent = "Hãy thêm flashcard để bắt đầu học";
                exampleWrap.hidden = true;
                counter.textContent = "0 / 0";
                progressFill.style.width = "0";
                prevButton.disabled = true;
                nextButton.disabled = true;
                return;
            }

            const card = cards[currentIndex];
            term.textContent = card.term;
            if (card.phonetic) {
                phonetic.textContent = card.phonetic;
                phoneticWrap.hidden = false;
            } else {
                phoneticWrap.hidden = true;
            }
            definition.textContent = card.definition;
            if (card.example) {
                example.textContent = card.example;
                exampleWrap.hidden = false;
            } else {
                exampleWrap.hidden = true;
            }

            stage.classList.toggle("is-flipped", flipped);
            counter.textContent = `${currentIndex + 1} / ${cards.length}`;
            progressFill.style.width = `${((currentIndex + 1) / cards.length) * 100}%`;
            prevButton.disabled = currentIndex === 0;
            nextButton.disabled = currentIndex === cards.length - 1;
        }

        function flipCard() {
            flipped = !flipped;
            renderStudyCard();
        }

        stage.addEventListener("click", flipCard);
        flipButton.addEventListener("click", flipCard);
        prevButton.addEventListener("click", () => {
            if (currentIndex > 0) {
                currentIndex -= 1;
                flipped = false;
                renderStudyCard();
            }
        });
        nextButton.addEventListener("click", () => {
            if (currentIndex < cards.length - 1) {
                currentIndex += 1;
                flipped = false;
                renderStudyCard();
            }
        });
        if (shuffleButton) {
            shuffleButton.addEventListener("click", () => {
                cards = shuffle([...cards]);
                currentIndex = 0;
                flipped = false;
                renderStudyCard();
            });
        }
        document.addEventListener("keydown", (event) => {
            if (event.target.closest("input, textarea, select, button, a")) {
                return;
            }
            if (event.key === " " || event.key === "Enter") {
                event.preventDefault();
                flipCard();
            }
            if (event.key === "ArrowLeft") {
                prevButton.click();
            }
            if (event.key === "ArrowRight") {
                nextButton.click();
            }
        });

        renderStudyCard();
    }

    function setupPractice() {
        const root = document.querySelector("[data-practice-root]");
        if (!root) {
            return;
        }

        const mode = root.dataset.practiceMode || "learn";
        const questions = Array.isArray(window.practiceQuestions) ? [...window.practiceQuestions] : [];
        const term = root.querySelector("[data-practice-term]");
        const answerList = root.querySelector("[data-answer-list]");
        const feedback = root.querySelector("[data-practice-feedback]");
        const progress = root.querySelector("[data-practice-progress]");
        const scoreElement = root.querySelector("[data-practice-score]");
        const totalElement = root.querySelector("[data-practice-total]");
        const countElement = root.querySelector("[data-practice-count]");
        const nextButton = root.querySelector("[data-next-question]");
        const skipButton = root.querySelector("[data-skip-question]");
        const resultPanel = root.querySelector("[data-practice-result]");
        const summary = root.querySelector("[data-practice-summary]");
        const restartButton = root.querySelector("[data-restart-practice]");
        const timerElement = root.querySelector("[data-test-timer]");
        let index = 0;
        let score = 0;
        let answered = false;
        let timerId = null;

        if (totalElement) {
            totalElement.textContent = String(questions.length);
        }

        function renderQuestion() {
            if (!questions.length) {
                finishPractice(false);
                return;
            }
            answered = false;
            const question = questions[index];
            term.textContent = question.term;
            feedback.textContent = "";
            feedback.className = "practice-feedback";
            nextButton.hidden = true;
            if (skipButton) {
                skipButton.hidden = false;
            }
            if (countElement) {
                countElement.textContent = `${index + 1} / ${questions.length}`;
            }
            if (scoreElement) {
                scoreElement.textContent = String(score);
            }
            progress.style.width = `${(index / questions.length) * 100}%`;

            answerList.innerHTML = "";
            question.choices.forEach((choice, choiceIndex) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "answer-option";
                button.innerHTML = `<span>${choiceIndex + 1}</span><strong></strong>`;
                button.querySelector("strong").textContent = choice;
                button.addEventListener("click", () => chooseAnswer(button, choice));
                answerList.appendChild(button);
            });
        }

        function chooseAnswer(button, choice) {
            if (answered) {
                return;
            }
            answered = true;
            const question = questions[index];
            const isCorrect = choice === question.correctAnswer;
            if (isCorrect) {
                score += 1;
            }

            answerList.querySelectorAll(".answer-option").forEach((option) => {
                const optionText = option.querySelector("strong").textContent;
                option.disabled = true;
                if (optionText === question.correctAnswer) {
                    option.classList.add("is-correct");
                }
            });
            if (!isCorrect) {
                button.classList.add("is-wrong");
            }

            feedback.textContent = isCorrect ? "Chính xác" : `Đáp án đúng: ${question.correctAnswer}`;
            feedback.classList.add(isCorrect ? "is-correct" : "is-wrong");
            nextButton.textContent = index === questions.length - 1 ? "Xem kết quả" : "Câu tiếp theo";
            nextButton.hidden = false;
            if (skipButton) {
                skipButton.hidden = true;
            }
            if (scoreElement) {
                scoreElement.textContent = String(score);
            }
        }

        function skipQuestion() {
            if (answered) {
                return;
            }
            const question = questions[index];
            answered = true;
            answerList.querySelectorAll(".answer-option").forEach((option) => {
                const optionText = option.querySelector("strong").textContent;
                option.disabled = true;
                if (optionText === question.correctAnswer) {
                    option.classList.add("is-correct");
                }
            });
            feedback.textContent = `Đáp án đúng: ${question.correctAnswer}`;
            feedback.classList.add("is-wrong");
            nextButton.textContent = index === questions.length - 1 ? "Xem kết quả" : "Câu tiếp theo";
            nextButton.hidden = false;
            if (skipButton) {
                skipButton.hidden = true;
            }
        }

        function goNext() {
            if (index >= questions.length - 1) {
                finishPractice(false);
                return;
            }
            index += 1;
            renderQuestion();
        }

        function finishPractice(timedOut) {
            if (timerId) {
                window.clearInterval(timerId);
            }
            root.querySelector(".practice-card")?.setAttribute("hidden", "hidden");
            root.querySelector(".practice-progress-strip")?.setAttribute("hidden", "hidden");
            root.querySelector(".test-status-row")?.setAttribute("hidden", "hidden");
            resultPanel.hidden = false;
            progress.style.width = "100%";
            const total = questions.length;
            if (!total) {
                summary.textContent = "Bộ này chưa có thẻ để học.";
                return;
            }
            const prefix = timedOut ? "Hết giờ. " : "";
            summary.textContent = `${prefix}Bạn đúng ${score}/${total} câu.`;
        }

        function restartPractice() {
            index = 0;
            score = 0;
            answered = false;
            resultPanel.hidden = true;
            root.querySelector(".practice-card")?.removeAttribute("hidden");
            root.querySelector(".practice-progress-strip")?.removeAttribute("hidden");
            shuffle(questions);
            renderQuestion();
        }

        function startTimer() {
            if (mode !== "test" || !timerElement) {
                return;
            }
            let seconds = Math.max(1, Number(root.dataset.testMinutes || 1)) * 60;
            const renderTimer = () => {
                const minutes = Math.floor(seconds / 60);
                const remainingSeconds = seconds % 60;
                timerElement.textContent = `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
            };
            renderTimer();
            timerId = window.setInterval(() => {
                seconds -= 1;
                renderTimer();
                if (seconds <= 0) {
                    finishPractice(true);
                }
            }, 1000);
        }

        nextButton.addEventListener("click", goNext);
        if (skipButton) {
            skipButton.addEventListener("click", skipQuestion);
        }
        if (restartButton) {
            restartButton.addEventListener("click", restartPractice);
        }

        renderQuestion();
        startTimer();
    }

    function setupFlipGame() {
        const root = document.querySelector("[data-flip-game-root]");
        if (!root) {
            return;
        }

        const sourceCards = Array.isArray(window.flipGameCards)
            ? window.flipGameCards.filter((card) => textValue(card.term) && textValue(card.definition))
            : [];
        const board = root.querySelector("[data-flip-board]");
        const scoreElement = root.querySelector("[data-flip-score]");
        const movesElement = root.querySelector("[data-flip-moves]");
        const comboElement = root.querySelector("[data-flip-combo]");
        const result = root.querySelector("[data-flip-result]");
        const summary = root.querySelector("[data-flip-summary]");
        const restartButton = root.querySelector("[data-flip-restart]");
        const playAgainButton = root.querySelector("[data-flip-play-again]");
        const soundButton = root.querySelector("[data-flip-sound]");
        const soundIcon = soundButton?.querySelector("i");
        const maxPairs = 12;

        let deck = [];
        let selected = [];
        let matchedPairs = 0;
        let moves = 0;
        let score = 0;
        let combo = 0;
        let locked = false;
        let scoreSaved = false;
        let soundEnabled = readSoundPreference();
        let audioContext = null;

        restartButton?.addEventListener("click", () => {
            playSound("shuffle");
            startGame();
        });
        playAgainButton?.addEventListener("click", () => {
            playSound("shuffle");
            startGame();
        });
        soundButton?.addEventListener("click", () => {
            soundEnabled = !soundEnabled;
            saveSoundPreference(soundEnabled);
            renderSoundState();
            if (soundEnabled) {
                playSound("toggle");
            }
        });
        renderSoundState();
        startGame();

        function startGame() {
            const selectedCards = shuffle([...sourceCards]).slice(0, maxPairs);
            selected = [];
            matchedPairs = 0;
            moves = 0;
            score = 0;
            combo = 0;
            locked = false;
            scoreSaved = false;
            result.hidden = true;
            deck = shuffle(selectedCards.flatMap((card, index) => [
                {id: `${index}-term`, pairId: index, kind: "term", label: "T\u1eeb v\u1ef1ng", value: textValue(card.term)},
                {id: `${index}-definition`, pairId: index, kind: "definition", label: "Ngh\u0129a", value: textValue(card.definition)}
            ]));
            renderStats();
            renderBoard();
        }

        function renderBoard() {
            board.innerHTML = "";
            board.classList.toggle("is-empty", deck.length === 0);
            if (!deck.length) {
                const empty = document.createElement("div");
                empty.className = "flip-game-empty";
                empty.innerHTML = `<i class="bi bi-grid-3x3-gap"></i><strong>Ch\u01b0a c\u00f3 \u0111\u1ee7 th\u1ebb \u0111\u1ec3 ch\u01a1i</strong><span>H\u00e3y th\u00eam t\u1eeb v\u1ef1ng v\u00e0 ngh\u0129a tr\u01b0\u1edbc nh\u00e9.</span>`;
                board.appendChild(empty);
                return;
            }

            deck.forEach((card) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "flip-memory-card";
                button.dataset.cardId = card.id;
                button.innerHTML = `
                    <span class="flip-memory-face flip-memory-front">
                        <i class="bi bi-stars"></i>
                    </span>
                    <span class="flip-memory-face flip-memory-back">
                        <small></small>
                        <strong></strong>
                    </span>
                `;
                button.querySelector("small").textContent = card.label;
                button.querySelector("strong").textContent = card.value;
                button.addEventListener("click", () => flipCard(button, card));
                board.appendChild(button);
            });
        }

        function flipCard(button, card) {
            if (locked || button.classList.contains("is-flipped") || button.classList.contains("is-matched")) {
                return;
            }
            button.classList.add("is-flipped");
            playSound("flip");
            selected.push({button, card});
            if (selected.length === 2) {
                checkSelectedPair();
            }
        }

        function checkSelectedPair() {
            moves += 1;
            const [first, second] = selected;
            const isMatch = first.card.pairId === second.card.pairId && first.card.kind !== second.card.kind;
            if (isMatch) {
                combo += 1;
                matchedPairs += 1;
                const gained = 100 + combo * 20 + Math.max(0, deck.length - moves) * 3;
                score += gained;
                first.button.classList.add("is-matched");
                second.button.classList.add("is-matched");
                selected = [];
                playSound("match");
                renderStats();
                if (matchedPairs === deck.length / 2) {
                    window.setTimeout(finishGame, 420);
                }
                return;
            }

            combo = 0;
            locked = true;
            first.button.classList.add("is-wrong");
            second.button.classList.add("is-wrong");
            playSound("miss");
            renderStats();
            window.setTimeout(() => {
                first.button.classList.remove("is-flipped", "is-wrong");
                second.button.classList.remove("is-flipped", "is-wrong");
                selected = [];
                locked = false;
            }, 780);
        }

        function finishGame() {
            const pairCount = deck.length / 2;
            const perfectMoves = pairCount;
            const bonus = Math.max(0, perfectMoves * 2 - moves) * 25;
            score += bonus;
            renderStats();
            summary.textContent = `B\u1ea1n gh\u00e9p ${pairCount} c\u1eb7p trong ${moves} l\u01b0\u1ee3t v\u00e0 nh\u1eadn ${score} \u0111i\u1ec3m.`;
            result.hidden = false;
            playSound("finish");
            saveGameScore();
        }

        function renderStats() {
            scoreElement.textContent = String(score);
            movesElement.textContent = String(moves);
            comboElement.textContent = String(combo);
        }

        async function saveGameScore() {
            if (scoreSaved || score <= 0) {
                return;
            }
            scoreSaved = true;
            const token = document.querySelector("meta[name='_csrf']")?.content;
            const header = document.querySelector("meta[name='_csrf_header']")?.content;
            const headers = {
                "Content-Type": "application/json",
                Accept: "application/json"
            };
            if (token && header) {
                headers[header] = token;
            }

            try {
                const response = await fetch("/api/games/score", {
                    method: "POST",
                    headers,
                    credentials: "same-origin",
                    body: JSON.stringify({score})
                });
                if (!response.ok) {
                    throw new Error("Score save failed");
                }
                const payload = await response.json();
                const totalScore = Number(payload?.totalScore);
                const clientScore = document.querySelector("[data-client-score]");
                if (clientScore && Number.isFinite(totalScore)) {
                    clientScore.textContent = String(totalScore);
                }
            } catch (error) {
                scoreSaved = false;
            }
        }

        function readSoundPreference() {
            try {
                return window.localStorage.getItem("flipSound") !== "off";
            } catch (error) {
                return true;
            }
        }

        function saveSoundPreference(enabled) {
            try {
                window.localStorage.setItem("flipSound", enabled ? "on" : "off");
            } catch (error) {
                // Sound still works for this page even if the browser blocks storage.
            }
        }

        function renderSoundState() {
            if (!soundButton) {
                return;
            }
            soundButton.classList.toggle("is-muted", !soundEnabled);
            soundButton.setAttribute("aria-pressed", String(soundEnabled));
            if (soundIcon) {
                soundIcon.className = soundEnabled ? "bi bi-volume-up-fill" : "bi bi-volume-mute-fill";
            }
        }

        function playSound(name) {
            if (!soundEnabled) {
                return;
            }
            const context = getAudioContext();
            if (!context) {
                return;
            }
            if (context.state === "suspended") {
                context.resume().catch(() => {});
            }

            const now = context.currentTime;
            if (name === "flip") {
                tone(context, 420, now, 0.07, "triangle", 0.014);
                tone(context, 560, now + 0.045, 0.08, "triangle", 0.012);
                return;
            }
            if (name === "match") {
                [523, 659, 784].forEach((frequency, index) => {
                    tone(context, frequency, now + index * 0.055, 0.12, "sine", 0.024);
                });
                return;
            }
            if (name === "miss") {
                tone(context, 260, now, 0.09, "triangle", 0.014);
                tone(context, 196, now + 0.075, 0.12, "triangle", 0.011);
                return;
            }
            if (name === "finish") {
                [523, 659, 784, 1046].forEach((frequency, index) => {
                    tone(context, frequency, now + index * 0.075, 0.16, "sine", 0.026);
                });
                return;
            }
            if (name === "shuffle") {
                softNoise(context, now, 0.16, 0.011);
                tone(context, 330, now + 0.02, 0.09, "triangle", 0.012);
                tone(context, 440, now + 0.1, 0.1, "triangle", 0.011);
                return;
            }
            if (name === "toggle") {
                tone(context, 660, now, 0.1, "sine", 0.018);
            }
        }

        function getAudioContext() {
            const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
            if (!AudioContextCtor) {
                return null;
            }
            if (!audioContext) {
                audioContext = new AudioContextCtor();
            }
            return audioContext;
        }

        function tone(context, frequency, start, duration, type, volume) {
            const oscillator = context.createOscillator();
            const gain = context.createGain();
            oscillator.type = type;
            oscillator.frequency.setValueAtTime(frequency, start);
            gain.gain.setValueAtTime(0.0001, start);
            gain.gain.exponentialRampToValueAtTime(volume, start + 0.012);
            gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
            oscillator.connect(gain).connect(context.destination);
            oscillator.start(start);
            oscillator.stop(start + duration + 0.03);
        }

        function softNoise(context, start, duration, volume) {
            const frameCount = Math.max(1, Math.floor(context.sampleRate * duration));
            const buffer = context.createBuffer(1, frameCount, context.sampleRate);
            const data = buffer.getChannelData(0);
            for (let index = 0; index < frameCount; index += 1) {
                const fade = 1 - index / frameCount;
                data[index] = (Math.random() * 2 - 1) * fade;
            }

            const source = context.createBufferSource();
            const filter = context.createBiquadFilter();
            const gain = context.createGain();
            source.buffer = buffer;
            filter.type = "lowpass";
            filter.frequency.setValueAtTime(900, start);
            gain.gain.setValueAtTime(0.0001, start);
            gain.gain.exponentialRampToValueAtTime(volume, start + 0.01);
            gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
            source.connect(filter).connect(gain).connect(context.destination);
            source.start(start);
            source.stop(start + duration + 0.02);
        }

        function textValue(value) {
            return String(value || "").trim();
        }
    }
})();
