import {api} from "../../core/api.js";
import {app} from "../../core/state.js";
import {escapeHtml, shuffle} from "../../core/ui.js";
import {renderSetModeShell} from "../layout/shell.js";

async function renderFlipGame(setId) {
    const set = await api(`/api/sets/${setId}/flip`);
    const sourceCards = shuffle(set.cards).slice(0, 12);
    const deck = shuffle(sourceCards.flatMap((card, index) => [
        {pair: index, text: card.term, type: "term"},
        {pair: index, text: card.definition, type: "definition"}
    ]));
    let open = [];
    const matched = new Set();
    let moves = 0;

    renderSetModeShell(set, "flip", `<div data-flip-root></div>`);
    const root = document.querySelector("[data-flip-root]");
    const draw = () => {
        root.innerHTML = `
            <section class="flip-panel">
                <div class="practice-top"><span>Lượt: ${moves}</span><span>Đã ghép: ${matched.size} / ${sourceCards.length}</span></div>
                <div class="memory-grid">
                    ${deck.map((card, index) => {
                        const shown = open.includes(index) || matched.has(card.pair);
                        return `<button class="memory-card ${shown ? "open" : ""}" data-memory="${index}">${shown ? escapeHtml(card.text) : "?"}</button>`;
                    }).join("")}
                </div>
            </section>
        `;
    };

    app.onclick = async event => {
        const button = event.target.closest("[data-memory]");
        if (!button) return;
        const cardIndex = Number(button.dataset.memory);
        if (open.includes(cardIndex) || matched.has(deck[cardIndex].pair) || open.length >= 2) return;

        open.push(cardIndex);
        if (open.length === 2) {
            moves++;
            const [first, second] = open;
            if (deck[first].pair === deck[second].pair && deck[first].type !== deck[second].type) {
                matched.add(deck[first].pair);
                open = [];
                if (matched.size === sourceCards.length) {
                    await api("/api/games/score", {
                        method: "POST",
                        body: JSON.stringify({score: Math.max(10, 250 - moves * 5)})
                    });
                    root.innerHTML = `<section class="practice-panel text-center"><h2>Ghép xong!</h2><p>Bạn hoàn thành trong ${moves} lượt.</p><button class="btn btn-primary" data-nav="/sets/${set.id}/flip">Chơi lại</button></section>`;
                    return;
                }
            } else {
                setTimeout(() => {
                    open = [];
                    draw();
                }, 700);
            }
        }
        draw();
    };
    draw();
}

export {renderFlipGame};
