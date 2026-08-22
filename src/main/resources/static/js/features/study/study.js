import {api} from "../../core/api.js";
import {app, state} from "../../core/state.js";
import {escapeHtml, shuffle} from "../../core/ui.js";
import {renderSetModeShell} from "../layout/shell.js";

async function renderStudy(setId) {
    const set = await api(`/api/sets/${setId}/study`);
    renderSetModeShell(set, "flashcards", `<div data-study-root></div>`);

    let index = 0;
    let flipped = false;
    let cards = set.cards.slice();

    const draw = () => {
        const card = cards[index] || {};
        document.querySelector("[data-study-root]").innerHTML = `
            <div class="study-progress">${index + 1} / ${cards.length}</div>
            <div class="study-card ${flipped ? "is-flipped" : ""}" data-flip-card>
                <div class="study-hint">Nhấn Space để lật thẻ</div>
                <div class="study-label">${flipped ? "Nghĩa" : "Từ vựng"}</div>
                <div class="study-term">${escapeHtml(flipped ? card.definition : card.term)}</div>
                ${!flipped && card.phonetic ? `<div class="study-phonetic">/${escapeHtml(card.phonetic)}/</div>` : ""}
                ${flipped && card.example ? `<div class="study-example">${escapeHtml(card.example)}</div>` : ""}
            </div>
            <div class="study-controls">
                <button class="btn btn-light" data-prev-card><i class="bi bi-arrow-left"></i></button>
                <button class="btn btn-primary" data-toggle-card><i class="bi bi-arrow-repeat"></i> Lật thẻ</button>
                <button class="btn btn-light" data-next-card><i class="bi bi-arrow-right"></i></button>
                <button class="btn btn-light" data-shuffle-cards><i class="bi bi-shuffle"></i> Đảo thứ tự</button>
            </div>
        `;
    };

    draw();
    app.onclick = event => {
        if (event.target.closest("[data-toggle-card], [data-flip-card]")) {
            flipped = !flipped;
            draw();
        } else if (event.target.closest("[data-prev-card]")) {
            index = (index - 1 + cards.length) % cards.length;
            flipped = false;
            draw();
        } else if (event.target.closest("[data-next-card]")) {
            index = (index + 1) % cards.length;
            flipped = false;
            draw();
        } else if (event.target.closest("[data-shuffle-cards]")) {
            cards = shuffle(cards);
            index = 0;
            flipped = false;
            draw();
        }
    };

    state.keyHandler = event => {
        if (event.code === "Space") {
            event.preventDefault();
            flipped = !flipped;
            draw();
        } else if (event.code === "ArrowLeft") {
            index = (index - 1 + cards.length) % cards.length;
            flipped = false;
            draw();
        } else if (event.code === "ArrowRight") {
            index = (index + 1) % cards.length;
            flipped = false;
            draw();
        }
    };
    document.addEventListener("keydown", state.keyHandler);
}

export {renderStudy};
