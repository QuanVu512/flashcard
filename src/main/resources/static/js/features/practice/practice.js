import {api} from "../../core/api.js";
import {app, state} from "../../core/state.js";
import {escapeAttr, escapeHtml, formatTime, sameAnswer, shuffle} from "../../core/ui.js";
import {renderSetModeShell} from "../layout/shell.js";
import {setupHandwritingCanvas} from "./handwriting.js";

async function renderLearn(setId) {
    const params = new URLSearchParams(window.location.search);
    const mode = params.get("testMode") || "meaning";
    const answerMode = params.get("answerMode") || "choice";
    const session = await api(`/api/sets/${setId}/learn?testMode=${encodeURIComponent(mode)}&answerMode=${encodeURIComponent(answerMode)}`);

    renderSetModeShell(session.set, "learn", `
        <form class="learn-toolbar" data-learn-mode-form="${setId}">
            <label class="form-label">Chế độ
                <select class="form-select" name="testMode">
                    <option value="meaning" ${mode === "meaning" ? "selected" : ""}>Ôn tập nghĩa</option>
                    <option value="term" ${mode === "term" ? "selected" : ""}>Ôn tập từ</option>
                </select>
            </label>
            <label class="form-label">Cách trả lời
                <select class="form-select" name="answerMode">
                    <option value="choice" ${answerMode === "choice" ? "selected" : ""}>Chọn đáp án</option>
                    <option value="write" ${answerMode === "write" ? "selected" : ""}>Viết đáp án</option>
                </select>
            </label>
            <button class="btn btn-light" type="submit">Áp dụng</button>
        </form>
        <div data-practice-root></div>
    `);
    runPractice(session, false);
}

async function renderTestSetup(setId) {
    const params = new URLSearchParams(window.location.search);
    const mode = params.get("testMode") || "meaning";
    const setup = await api(`/api/sets/${setId}/test/setup?testMode=${encodeURIComponent(mode)}`);

    renderSetModeShell(setup.set, "test", `
        <section class="practice-panel">
            <h2>Thiết lập bài test</h2>
            <form data-test-setup-form="${setId}" class="test-setup-grid">
                <label class="form-label">Chế độ
                    <select class="form-select" name="testMode">
                        <option value="meaning" ${setup.mode === "meaning" ? "selected" : ""}>Ôn tập nghĩa</option>
                        <option value="term" ${setup.mode === "term" ? "selected" : ""}>Ôn tập từ</option>
                    </select>
                </label>
                <label class="form-label">Số câu
                    <input class="form-control" name="questionCount" type="number" min="1" max="${setup.maxQuestions}" value="${setup.defaultQuestions}">
                </label>
                <label class="form-label">Thời gian phút
                    <input class="form-control" name="minutes" type="number" min="1" max="180" value="${setup.defaultMinutes}">
                </label>
                <button class="btn btn-primary" type="submit">Bắt đầu</button>
            </form>
        </section>
    `);
}

async function renderTest(setId) {
    const params = new URLSearchParams(window.location.search);
    const query = new URLSearchParams({
        questionCount: params.get("questionCount") || "10",
        minutes: params.get("minutes") || "10",
        testMode: params.get("testMode") || "meaning"
    });
    const session = await api(`/api/sets/${setId}/test?${query}`);
    renderSetModeShell(session.set, "test", `<div data-practice-root></div>`);
    runPractice(session, true);
}

function runPractice(session, timed) {
    let index = 0;
    let correct = 0;
    let missed = [];
    let questions = session.questions.slice();
    let bonusRound = false;
    let remainingSeconds = session.minutes * 60;
    const originalQuestionCount = questions.length;
    const root = document.querySelector("[data-practice-root]");

    const finish = () => {
        stopTimer();
        if (!timed && !bonusRound && missed.length) {
            questions = shuffle(missed);
            missed = [];
            index = 0;
            bonusRound = true;
            root.innerHTML = `<section class="practice-panel"><h2>Bonus round</h2><p>Ôn lại các câu chưa chắc.</p><button class="btn btn-primary" data-start-bonus>Tiếp tục</button></section>`;
            return;
        }
        const score = Math.round((correct / Math.max(1, originalQuestionCount)) * 100);
        root.innerHTML = `<section class="practice-panel text-center"><h2>Hoàn thành</h2><p>Điểm: ${score}%</p><button class="btn btn-primary" data-nav="/sets/${session.set.id}">Về bộ thẻ</button></section>`;
    };

    const draw = () => {
        if (index >= questions.length) {
            finish();
            return;
        }
        const question = questions[index];
        const isWrite = session.answerMode === "write";
        root.innerHTML = `
            <section class="practice-panel">
                <div class="practice-top">
                    <span>${index + 1} / ${questions.length}</span>
                    ${timed ? `<span data-timer>${formatTime(remainingSeconds)}</span>` : ""}
                </div>
                <div class="question-label">${escapeHtml(session.questionLabel)}</div>
                <h2>${escapeHtml(question.term)}</h2>
                ${isWrite ? handwritingAnswer() : choiceAnswer(question)}
                <button class="link-button" data-dont-know>Không biết?</button>
            </section>
        `;
        if (isWrite) setupHandwritingCanvas(root);
    };

    const answer = value => {
        const question = questions[index];
        if (sameAnswer(value, question.correctAnswer)) {
            correct++;
        } else {
            missed.push(question);
        }
        index++;
        draw();
    };

    if (timed && remainingSeconds > 0) {
        state.timer = setInterval(() => {
            remainingSeconds--;
            const badge = document.querySelector("[data-timer]");
            if (badge) badge.textContent = formatTime(remainingSeconds);
            if (remainingSeconds <= 0) finish();
        }, 1000);
    }

    app.onclick = event => {
        const choice = event.target.closest("[data-choice]");
        if (choice) {
            answer(choice.dataset.choice);
            return;
        }
        if (event.target.closest("[data-dont-know]")) {
            missed.push(questions[index]);
            index++;
            draw();
        } else if (event.target.closest("[data-start-bonus]")) {
            draw();
        }
    };
    app.onsubmit = event => {
        const form = event.target.closest("[data-write-answer]");
        if (!form) return;
        event.preventDefault();
        answer(new FormData(form).get("answer") || "");
    };
    draw();
}

function choiceAnswer(question) {
    return `
        <div class="choice-grid">
            ${question.choices.map(choice => `<button class="choice-card" data-choice="${escapeAttr(choice)}">${escapeHtml(choice)}</button>`).join("")}
        </div>
    `;
}

function handwritingAnswer() {
    return `
        <form data-write-answer>
            <input class="form-control" name="answer" placeholder="Nhập đáp án">
            <button class="btn btn-primary" type="submit">Trả lời</button>
        </form>
        <div class="handwriting-candidates" data-handwriting-candidates aria-label="Gợi ý chữ viết tay">
            <div class="handwriting-candidate-list" data-handwriting-candidate-list aria-live="polite"></div>
        </div>
        <section class="handwriting-box">
            <div class="handwriting-head">
                <strong><i class="bi bi-pencil-square"></i> Bảng vẽ</strong>
                <select class="form-select" data-handwriting-language>
                    <option value="" selected disabled>---Chọn ngôn ngữ---</option>
                    <option value="zh-Hans">Tiếng Trung</option>
                    <option value="ja">Tiếng Nhật</option>
                    <option value="ko">Tiếng Hàn</option>
                </select>
                <button class="btn btn-light" type="button" data-clear-handwriting><i class="bi bi-eraser"></i> Xóa</button>
                <button class="btn btn-primary" type="button" data-recognize-handwriting><i class="bi bi-check-lg"></i> Xong</button>
            </div>
            <canvas data-handwriting-canvas width="900" height="240"></canvas>
            <div class="handwriting-status" data-handwriting-status>Vẽ đáp án rồi bấm Xong.</div>
        </section>
    `;
}

function stopTimer() {
    if (!state.timer) return;
    clearInterval(state.timer);
    state.timer = null;
}

export {renderLearn, renderTest, renderTestSetup};
