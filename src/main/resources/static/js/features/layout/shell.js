import {app, state} from "../../core/state.js";
import {cloneTemplate} from "../../core/templates.js";
import {escapeHtml} from "../../core/ui.js";

function renderShell(content, active = "library", activeFolderId = null) {
    document.body.className = "app-body";
    app.onclick = null;
    app.onsubmit = null;
    app.replaceChildren(cloneTemplate("fragments/shell"));

    app.querySelector("[data-sidebar-nav]").innerHTML = `
        <a class="${active === "home" ? "active" : ""}" data-nav="/library"><i class="bi bi-house"></i><span>Trang chủ</span></a>
        <a class="${active === "library" ? "active" : ""}" data-nav="/library"><i class="bi bi-folder"></i><span>Thư viện</span></a>
        ${state.user?.role === "ROLE_ADMIN" ? `<a class="${active === "admin" ? "active" : ""}" data-nav="/admin"><i class="bi bi-shield-check"></i><span>Admin</span></a>` : ""}`;
    app.querySelector("[data-folder-list]").innerHTML = state.folders.map(folder => `
        <a class="${folder.id === activeFolderId ? "active-folder" : ""}" data-nav="/folders/${folder.id}"><i class="bi bi-folder"></i><span>${escapeHtml(folder.name)}</span></a>`).join("") +
        `<button class="sidebar-action" type="button" data-create-folder><i class="bi bi-plus-lg"></i><span>Thư mục mới</span></button>`;
    app.querySelector("[data-score]").textContent = state.user?.score || 0;
    app.querySelector("[data-avatar]").textContent = (state.user?.displayName || "U").slice(0, 1).toUpperCase();
    app.querySelector("[data-page-content]").innerHTML = content;
}

function renderSetModeShell(set, activeMode, innerHtml) {
    const fragment = cloneTemplate("fragments/set-mode");
    const wrapper = document.createElement("div");
    wrapper.append(fragment);
    wrapper.querySelector("[data-mode-title]").textContent = set.title;
    wrapper.querySelector("[data-edit-set]").dataset.nav = `/sets/${set.id}/edit`;
    wrapper.querySelector("[data-mode-tabs]").innerHTML = [
        modeTab("flashcards", "Flashcards", `/sets/${set.id}`, activeMode),
        modeTab("learn", "Learn", `/sets/${set.id}/learn`, activeMode),
        modeTab("test", "Test", `/sets/${set.id}/test/setup`, activeMode),
        modeTab("flip", "Flip game", `/sets/${set.id}/flip`, activeMode)
    ].join("");
    wrapper.querySelector("[data-mode-content]").innerHTML = innerHtml;
    renderShell(wrapper.innerHTML, "library");
}

function modeTab(mode, label, path, activeMode) {
    return `<button class="mode-tab ${mode === activeMode ? "active" : ""}" data-nav="${path}">${escapeHtml(label)}</button>`;
}

export {renderSetModeShell, renderShell};
