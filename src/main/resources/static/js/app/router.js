import {api} from "../core/api.js";
import {loginPath, returnPath} from "../core/auth-routes.js";
import {clearSession, navigate} from "../core/navigation.js";
import {app, state} from "../core/state.js";
import {cloneTemplate} from "../core/templates.js";
import {renderAdmin} from "../features/admin/admin.js";
import {renderAuth} from "../features/auth/auth.js";
import {renderFlipGame} from "../features/game/flip-game.js";
import {renderShell} from "../features/layout/shell.js";
import {renderLibrary, renderSetForm} from "../features/library/library.js";
import {renderLearn, renderTest, renderTestSetup} from "../features/practice/practice.js";
import {renderStudy} from "../features/study/study.js";

async function renderRoute() {
    clearPageEffects();
    const path = window.location.pathname;
    try {
        await restoreSession();
        if (isAuthPage(path)) {
            if (state.user) {
                navigate(returnPath(), true);
            } else {
                renderAuth(path === "/login" ? "login" : "register");
            }
            return;
        }
        if (!state.user) {
            redirectToLogin();
            return;
        }

        if (path === "/" || path === "/library") {
            await renderLibrary();
        } else if (path.startsWith("/folders/")) {
            await renderLibrary(path.split("/")[2]);
        } else if (path === "/sets/new") {
            await renderSetForm();
        } else if (/^\/sets\/[^/]+\/edit$/.test(path)) {
            await renderSetForm(path.split("/")[2]);
        } else if (/^\/sets\/[^/]+\/learn$/.test(path)) {
            await renderLearn(path.split("/")[2]);
        } else if (/^\/sets\/[^/]+\/test\/setup$/.test(path)) {
            await renderTestSetup(path.split("/")[2]);
        } else if (/^\/sets\/[^/]+\/test$/.test(path)) {
            await renderTest(path.split("/")[2]);
        } else if (/^\/sets\/[^/]+\/flip$/.test(path)) {
            await renderFlipGame(path.split("/")[2]);
        } else if (/^\/sets\/[^/]+$/.test(path)) {
            await renderStudy(path.split("/")[2]);
        } else if (path === "/admin") {
            await renderAdmin();
        } else {
            navigate("/library", true);
        }
    } catch (error) {
        if (error.status === 401) return;
        renderError(error.message || "Không thể tải dữ liệu.");
    }
}

async function restoreSession() {
    if (state.sessionChecked) return;
    try {
        state.user = await api("/api/auth/me", {redirectOnUnauthorized: false});
    } catch (error) {
        if (error.status !== 401) throw error;
        state.user = null;
    } finally {
        state.sessionChecked = true;
    }
}

function redirectToLogin() {
    clearSession();
    if (isAuthPage(window.location.pathname)) return;
    navigate(loginPath(), true);
}

function clearPageEffects() {
    if (state.keyHandler) {
        document.removeEventListener("keydown", state.keyHandler);
        state.keyHandler = null;
    }
    if (state.timer) {
        clearInterval(state.timer);
        state.timer = null;
    }
    app.onclick = null;
    app.onsubmit = null;
}

function renderError(message) {
    const view = document.createElement("div");
    view.append(cloneTemplate("error/generic"));
    view.querySelector("[data-error-message]").textContent = message;
    renderShell(view.innerHTML);
}

function isAuthPage(path) {
    return path === "/login" || path === "/register";
}

export {redirectToLogin, renderError, renderRoute};
