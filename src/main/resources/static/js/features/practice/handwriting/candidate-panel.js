function createCandidatePanel(root, onSelect) {
    const panel = root.querySelector("[data-handwriting-candidates]");
    const list = root.querySelector("[data-handwriting-candidate-list]");

    const clear = () => {
        panel.setAttribute("aria-busy", "false");
        list.replaceChildren();
    };

    panel.addEventListener("click", event => {
        const button = event.target.closest("[data-handwriting-candidate]");
        if (!button) return;
        onSelect(button.dataset.handwritingCandidate);
    });

    return {
        clear,
        loading() {
            panel.setAttribute("aria-busy", "true");
            list.replaceChildren();
        },
        show(candidates) {
            const uniqueCandidates = [...new Set(candidates)].filter(Boolean).slice(0, 8);
            if (!uniqueCandidates.length) {
                clear();
                return;
            }

            panel.setAttribute("aria-busy", "false");
            list.replaceChildren(...uniqueCandidates.map(candidate => {
                const button = document.createElement("button");
                button.className = "handwriting-candidate";
                button.type = "button";
                button.dataset.handwritingCandidate = candidate;
                button.textContent = candidate;
                button.setAttribute("aria-label", `Chọn ${candidate}`);
                return button;
            }));
        }
    };
}

export {createCandidatePanel};
