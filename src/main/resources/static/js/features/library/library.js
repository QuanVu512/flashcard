import {api} from "../../core/api.js";
import {navigate} from "../../core/navigation.js";
import {state} from "../../core/state.js";
import {cloneTemplate} from "../../core/templates.js";
import {emptyState, escapeAttr, escapeHtml, hideBox, queryString, showFormError} from "../../core/ui.js";
import {renderShell} from "../layout/shell.js";

async function renderLibrary(folderId = null) {
    const endpoint = folderId ? `/api/folders/${folderId}/library` : `/api/library${queryString("q")}`;
    const data = await api(endpoint);
    state.user = data.user;
    state.folders = data.folders;
    const currentFolder = folderId ? data.folders.find(folder => folder.id === folderId) : null;
    const view = document.createElement("div");
    view.append(cloneTemplate("library"));
    view.querySelector("[data-library-label]").textContent = currentFolder ? "Thư mục" : "Thư viện";
    view.querySelector("[data-library-title]").textContent = currentFolder?.name || "Thư viện của bạn";
    view.querySelector('[name="q"]').value = new URLSearchParams(location.search).get("q") || "";
    view.querySelector("[data-set-list]").innerHTML = data.sets.length
        ? data.sets.map(renderSetCard).join("")
        : emptyState("Chưa có bộ flashcard nào.");
    renderShell(view.innerHTML, "library", folderId);
}

function renderSetCard(set) {
    const row = cloneTemplate("fragments/set-card").querySelector(".set-row");
    row.dataset.nav = `/sets/${set.id}`;
    row.querySelector("[data-set-meta]").textContent = `${set.cardCount} thẻ${set.folder ? ` • ${set.folder.name}` : ""}`;
    row.querySelector("[data-set-title]").textContent = set.title;
    const description = row.querySelector("[data-set-description]");
    description.textContent = set.description || "";
    description.hidden = !set.description;
    row.querySelector("[data-edit-set]").dataset.nav = `/sets/${set.id}/edit`;
    row.querySelector("[data-delete-set]").dataset.deleteSet = set.id;
    return row.outerHTML;
}

async function renderSetForm(setId = null) {
    const [folders, form] = await Promise.all([
        api("/api/folders"),
        setId ? api(`/api/sets/${setId}/form`) : Promise.resolve(defaultSetForm())
    ]);
    if (!setId) form.folderId = new URLSearchParams(location.search).get("folderId") || form.folderId;
    state.folders = folders;

    const view = document.createElement("div");
    view.append(cloneTemplate("set-form"));
    view.querySelector("[data-form-title]").textContent = setId ? "Sửa bộ flashcard" : "Tạo bộ flashcard";
    const setForm = view.querySelector("[data-set-form]");
    setForm.dataset.setForm = setId || "";
    setForm.querySelector('[name="title"]').value = form.title || "";
    setForm.querySelector('[name="description"]').value = form.description || "";
    const folderSelect = setForm.querySelector('[name="folderId"]');
    folders.forEach(folder => folderSelect.add(new Option(folder.name, folder.id, false, form.folderId === folder.id)));
    setForm.querySelector("[data-card-list]").innerHTML = (form.cards || []).map((card, index) => renderCardEditor(card, index)).join("");
    setForm.querySelector("[data-cancel-set]").dataset.nav = setId ? `/sets/${setId}` : "/library";
    renderShell(view.innerHTML, "library");
}

function renderCardEditor(card = {}, index = 0) {
    const row = cloneTemplate("fragments/card-editor").querySelector("[data-card-row]");
    row.querySelector(".card-index").textContent = index + 1;
    ["term", "phonetic", "definition", "example"].forEach(field => {
        row.querySelector(`[data-card-field="${field}"]`).value = card[field] || "";
    });
    return row.outerHTML;
}

async function saveSet(form) {
    const setId = form.dataset.setForm;
    const errorBox = form.querySelector("[data-form-error]");
    errorBox.hidden = true;
    const payload = collectSetForm(form);
    const invalid = validateSetPayload(payload, form);
    if (invalid) return showFormError(errorBox, invalid);
    try {
        const saved = await api(setId ? `/api/sets/${setId}` : "/api/sets", {
            method: setId ? "PUT" : "POST",
            body: JSON.stringify(payload)
        });
        navigate(`/sets/${saved.id}`, true);
    } catch (error) {
        showFormError(errorBox, error.message);
    }
}

async function suggestForRow(row, text) {
    const meaningBox = row.querySelector("[data-meaning-suggestions]");
    const phoneticBox = row.querySelector("[data-phonetic-suggestions]");
    if (!text.trim()) {
        hideBox(meaningBox);
        hideBox(phoneticBox);
        return;
    }
    meaningBox.hidden = false;
    meaningBox.innerHTML = `<div class="suggest-item muted">Đang tìm nghĩa...</div>`;
    phoneticBox.hidden = false;
    phoneticBox.innerHTML = `<div class="suggest-item muted">Đang tìm phiên âm...</div>`;
    try {
        const payload = await api(`/api/translation/suggest?text=${encodeURIComponent(text)}`);
        renderSuggestions(meaningBox, payload.suggestions, payload.message || "Gợi ý nghĩa");
        renderSuggestions(phoneticBox, payload.phoneticSuggestions, payload.phoneticMessage || "Gợi ý phiên âm");
    } catch (error) {
        meaningBox.innerHTML = `<div class="suggest-item error">${escapeHtml(error.message)}</div>`;
        phoneticBox.innerHTML = `<div class="suggest-item error">${escapeHtml(error.message)}</div>`;
    }
}

function renderSuggestions(box, values = [], title = "Gợi ý") {
    box.hidden = false;
    box.innerHTML = values.length
        ? `<div class="suggest-title">${escapeHtml(title)}</div>${values.map(value => `<button type="button" class="suggest-item" data-suggestion-value="${escapeAttr(value)}">${escapeHtml(value)}</button>`).join("")}`
        : `<div class="suggest-item muted">${escapeHtml(title || "Chưa nhận được gợi ý.")}</div>`;
}

function collectSetForm(form) {
    const data = new FormData(form);
    return {
        title: data.get("title") || "",
        description: data.get("description") || "",
        folderId: data.get("folderId") || null,
        cards: Array.from(form.querySelectorAll("[data-card-row]")).map(row => ({
            term: row.querySelector('[data-card-field="term"]').value,
            phonetic: row.querySelector('[data-card-field="phonetic"]').value,
            definition: row.querySelector('[data-card-field="definition"]').value,
            example: row.querySelector('[data-card-field="example"]').value
        }))
    };
}

function validateSetPayload(payload, form) {
    form.querySelectorAll("[data-card-error]").forEach(error => {
        error.hidden = true;
        error.textContent = "";
    });
    if (!payload.title.trim()) return "Vui lòng nhập tên bộ flashcard.";
    let complete = 0;
    let firstError = "";
    payload.cards.forEach((card, index) => {
        const hasContent = [card.term, card.definition, card.phonetic, card.example].some(value => value && value.trim());
        if (card.term.trim() && card.definition.trim()) {
            complete++;
            return;
        }
        if (!hasContent) return;
        const box = form.querySelectorAll("[data-card-row]")[index].querySelector("[data-card-error]");
        const message = "Thẻ này cần có đủ từ vựng và nghĩa, hoặc hãy xóa thẻ.";
        box.textContent = message;
        box.hidden = false;
        firstError ||= message;
    });
    if (!complete && !firstError) return "Vui lòng thêm ít nhất 1 flashcard.";
    return firstError;
}

function defaultSetForm() {
    return {title: "", description: "", folderId: "", cards: [{}, {}, {}, {}]};
}

async function createFolderFromPrompt() {
    const name = window.prompt("Tên thư mục mới");
    if (!name?.trim()) return;
    await api("/api/folders", {method: "POST", body: JSON.stringify({name, description: ""})});
    await renderLibrary();
}

async function deleteSetById(id) {
    if (!window.confirm("Xóa bộ flashcard này?")) return;
    await api(`/api/sets/${id}`, {method: "DELETE"});
    navigate("/library", true);
}

function renumberCards() {
    document.querySelectorAll("[data-card-row]").forEach((row, index) => {
        row.querySelector(".card-index").textContent = index + 1;
    });
}

export {
    createFolderFromPrompt,
    deleteSetById,
    renumberCards,
    renderCardEditor,
    renderLibrary,
    renderSetForm,
    saveSet,
    suggestForRow
};
