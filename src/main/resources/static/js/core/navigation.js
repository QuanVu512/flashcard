import {api} from "./api.js";
import {state} from "./state.js";

function navigate(path, replace = false) {
    const method = replace ? "replaceState" : "pushState";
    history[method]({}, "", path);
    window.dispatchEvent(new PopStateEvent("popstate"));
}

function clearSession() {
    state.user = null;
    state.sessionChecked = true;
    state.folders = [];
}

async function logout(redirect = true) {
    await api("/api/auth/logout", {method: "POST", redirectOnUnauthorized: false});
    clearSession();
    if (redirect) navigate("/login", true);
}

export {navigate, clearSession, logout};
