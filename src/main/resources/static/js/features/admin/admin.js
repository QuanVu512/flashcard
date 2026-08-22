import {api} from "../../core/api.js";
import {cloneTemplate} from "../../core/templates.js";
import {escapeHtml} from "../../core/ui.js";
import {renderShell} from "../layout/shell.js";

async function renderAdmin() {
    const data = await api("/api/admin");
    const view = document.createElement("div");
    view.append(cloneTemplate("admin/dashboard"));
    Object.entries(data.stats).forEach(([key, value]) => {
        view.querySelector(`[data-stat="${key}"]`).textContent = value;
    });
    view.querySelector("[data-admin-users]").innerHTML = data.users.map(user => `
        <tr><td>${escapeHtml(user.email)}</td><td>${escapeHtml(user.displayName)}</td><td>${escapeHtml(user.role)}</td><td>${user.score}</td><td>${user.enabled ? "Đang mở" : "Đã khóa"}</td><td><button class="btn btn-light btn-sm" data-toggle-user="${user.id}" data-enabled="${!user.enabled}">${user.enabled ? "Khóa" : "Mở khóa"}</button></td></tr>
    `).join("");
    renderShell(view.innerHTML, "admin");
}

async function toggleUserStatus(id, enabled) {
    await api(`/api/admin/users/${id}/status`, {method: "PATCH", body: JSON.stringify({enabled})});
    await renderAdmin();
}

export {renderAdmin, toggleUserStatus};
