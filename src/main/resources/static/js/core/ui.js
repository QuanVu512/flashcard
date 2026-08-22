function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
    return escapeHtml(value).replaceAll("`", "&#096;");
}

function emptyState(message) {
    return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

function showFormError(box, message) {
    box.textContent = message;
    box.hidden = false;
}

function hideBox(box) {
    if (!box) return;
    box.hidden = true;
    box.innerHTML = "";
}

function queryString(name) {
    const value = new URLSearchParams(location.search).get(name);
    return value ? `?${name}=${encodeURIComponent(value)}` : "";
}

function shuffle(values) {
    const copy = values.slice();
    for (let index = copy.length - 1; index > 0; index--) {
        const target = Math.floor(Math.random() * (index + 1));
        [copy[index], copy[target]] = [copy[target], copy[index]];
    }
    return copy;
}

function sameAnswer(left, right) {
    return String(left || "").trim().toLowerCase() === String(right || "").trim().toLowerCase();
}

function formatTime(totalSeconds) {
    const safe = Math.max(0, totalSeconds);
    return `${Math.floor(safe / 60)}:${String(safe % 60).padStart(2, "0")}`;
}

export {
    escapeAttr,
    escapeHtml,
    emptyState,
    formatTime,
    hideBox,
    queryString,
    sameAnswer,
    shuffle,
    showFormError
};
