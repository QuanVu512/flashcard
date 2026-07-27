(function () {
    const cardRows = document.querySelector("#cardRows");

    function cardRowTemplate(index) {
        return `
            <div class="term-row" data-card-row>
                <div class="term-number">${index + 1}</div>
                <div class="term-fields">
                    <div>
                        <label class="form-label" for="card-term-${index}">Từ vựng</label>
                        <input class="form-control" id="card-term-${index}" name="cards[${index}].term" placeholder="Nhập từ vựng">
                    </div>
                    <div>
                        <label class="form-label" for="card-definition-${index}">Nghĩa</label>
                        <input class="form-control" id="card-definition-${index}" name="cards[${index}].definition" placeholder="Nhập nghĩa">
                    </div>
                    <div>
                        <label class="form-label" for="card-example-${index}">Ví dụ</label>
                        <input class="form-control" id="card-example-${index}" name="cards[${index}].example" placeholder="Nhập ví dụ nếu có">
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
                input.name = input.name.replace(/cards\[\d+]/, `cards[${index}]`);
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
        });
    }

    document.addEventListener("click", (event) => {
        const addButton = event.target.closest("[data-add-card]");
        if (addButton && cardRows) {
            const index = cardRows.querySelectorAll("[data-card-row]").length;
            cardRows.insertAdjacentHTML("beforeend", cardRowTemplate(index));
            const newest = cardRows.querySelector("[data-card-row]:last-child input");
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

    setupFlashcards();
    setupPractice();

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
