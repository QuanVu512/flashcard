import {state} from "./state.js";

const apiBaseUrl = document.querySelector('meta[name="flashcard-api-base-url"]')
    ?.content.trim().replace(/\/$/, "") || "";
let csrfTokenPromise = null;

class ApiError extends Error {
    constructor(message, status, details = []) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.details = details;
    }
}

async function api(path, options = {}) {
    const {redirectOnUnauthorized = true, ...requestOptions} = options;
    const headers = Object.assign({Accept: "application/json"}, requestOptions.headers || {});
    const method = (requestOptions.method || "GET").toUpperCase();

    if (requestOptions.body && !(requestOptions.body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
    }
    if (!isSafeMethod(method)) {
        const csrf = await getCsrfToken();
        headers[csrf.headerName] = csrf.token;
    }

    const response = await fetch(apiUrl(path), {
        ...requestOptions,
        headers,
        credentials: "include"
    });
    if (response.status === 204) return null;

    const text = await response.text();
    const payload = parsePayload(text);
    if (!response.ok) {
        const details = Array.isArray(payload?.details) ? payload.details : [];
        const message = payload?.message || "Yêu cầu chưa thành công.";
        if (response.status === 401 && redirectOnUnauthorized && !isPublicAuthRequest(path)) {
            state.user = null;
            state.sessionChecked = false;
            state.folders = [];
            window.dispatchEvent(new CustomEvent("auth:required"));
        }
        throw new ApiError(details.length ? `${message} ${details.join(" ")}` : message, response.status, details);
    }
    return payload;
}

function getCsrfToken() {
    if (!csrfTokenPromise) {
        csrfTokenPromise = fetch(apiUrl("/api/auth/csrf"), {
            headers: {Accept: "application/json"},
            credentials: "include"
        })
            .then(async response => {
                const payload = parsePayload(await response.text());
                if (!response.ok || !payload?.headerName || !payload?.token) {
                    throw new ApiError(payload?.message || "Không thể khởi tạo bảo vệ CSRF.", response.status);
                }
                return payload;
            })
            .catch(error => {
                csrfTokenPromise = null;
                throw error;
            });
    }
    return csrfTokenPromise;
}

function isSafeMethod(method) {
    return ["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
}

function isPublicAuthRequest(path) {
    return path === "/api/auth/login" || path === "/api/auth/register" || path === "/api/auth/logout";
}

function parsePayload(text) {
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return {message: text};
    }
}

function apiUrl(path) {
    return `${apiBaseUrl}${path}`;
}

export {api, ApiError};
