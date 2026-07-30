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
        form.addEventListener("submit", () => restoreCardInputNames(form));
    });

    setupFlashcards();
    setupPractice();
    setupCardEntryFields();
    setupTranslationSuggestions();

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

        function configureRow(row) {
            const term = termInput(row);
            const definition = definitionInput(row);
            if (term) {
                term.setAttribute("data-translation-term", "");
            }
            if (definition) {
                definition.setAttribute("data-translation-definition", "");
                suggestionBox(row);
            }
        }

        function suggestionBox(row) {
            const definition = definitionInput(row);
            if (!definition) {
                return null;
            }
            const field = definition.parentElement;
            field.classList.add("translation-field");
            let box = field.querySelector("[data-translation-suggestions]");
            if (!box) {
                box = document.createElement("div");
                box.className = "translation-suggestions";
                box.setAttribute("data-translation-suggestions", "");
                box.hidden = true;
                definition.insertAdjacentElement("afterend", box);
            }
            return box;
        }

        function hideSuggestions(row) {
            const box = suggestionBox(row);
            if (box) {
                box.hidden = true;
                box.innerHTML = "";
            }
        }

        function showState(row, message, autoHide = false) {
            const box = suggestionBox(row);
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
                        hideSuggestions(row);
                    }
                }, 2600);
            }
        }

        function renderSuggestions(row, payload) {
            const box = suggestionBox(row);
            const definition = definitionInput(row);
            if (!box || !definition) {
                return;
            }

            const suggestions = Array.isArray(payload?.suggestions) ? payload.suggestions : [];
            if (!payload?.enabled) {
                showState(row, payload?.message || "Chưa bật gợi ý dịch.", true);
                return;
            }
            if (!suggestions.length) {
                if (payload?.message) {
                    showState(row, payload.message, true);
                    return;
                }
                hideSuggestions(row);
                return;
            }

            box.innerHTML = "";
            const label = document.createElement("div");
            label.className = "translation-suggestion-label";
            label.textContent = payload.detectedLanguage
                ? `Gợi ý nghĩa (${payload.detectedLanguage} -> ${payload.targetLanguage})`
                : "Gợi ý nghĩa";
            box.appendChild(label);

            suggestions.forEach((suggestion) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "translation-suggestion-item";
                button.textContent = suggestion;
                button.addEventListener("click", () => {
                    definition.value = suggestion;
                    definition.dispatchEvent(new Event("input", {bubbles: true}));
                    hideSuggestions(row);
                    definition.focus();
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

            try {
                let payload = await requestSuggestion(text);
                if (!payload) {
                    await new Promise((resolve) => window.setTimeout(resolve, 900));
                    payload = await requestSuggestion(text);
                }
                if (!payload) {
                    showState(row, "Dịch đang phản hồi chậm. Thử lại sau một chút.", true);
                    return;
                }
                if (requestTokens.get(row) === token) {
                    renderSuggestions(row, payload);
                }
            } catch (error) {
                if (requestTokens.get(row) === token) {
                    showState(row, "Dịch đang phản hồi chậm. Thử lại sau một chút.", true);
                }
            }
        }

        function scheduleSuggestion(row) {
            configureRow(row);
            const term = termInput(row);
            const definition = definitionInput(row);
            if (!term || !definition) {
                return;
            }
            const text = term.value.trim();
            if (definition.value.trim() && document.activeElement !== term) {
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
            configureRow(row);
            if (event.target === termInput(row)) {
                scheduleSuggestion(row);
            }
            if (event.target === definitionInput(row)) {
                hideSuggestions(row);
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
                definition.textContent = "Hãy thêm flashcard để bắt đầu học";
                counter.textContent = "0 / 0";
                progressFill.style.width = "0";
                prevButton.disabled = true;
                nextButton.disabled = true;
                return;
            }

            const card = cards[currentIndex];
            term.textContent = card.term;
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
})();
