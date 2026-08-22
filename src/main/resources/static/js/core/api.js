import {state} from "./state.js";

const apiBaseUrl = document.querySelector('meta[name="flashcard-api-base-url"]')
    ?.content.trim().replace(/\/$/, "") || "";
let csrfTokenPromise = null;
let refreshPromise = null;

class ApiError extends Error {
    constructor(message, status, details = [], metadata = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.details = details;
        this.retryAfterSeconds = metadata.retryAfterSeconds || 0;
        this.authAction = metadata.authAction || null;
    }
}

async function api(path, options = {}) {
    const {
        redirectOnUnauthorized = true,
        retryAfterCsrfRefresh = true,
        retryAfterRefresh = true,
        ...requestOptions
    } = options;
    const response = await send(path, requestOptions);
    if (response.status === 204) return null;

    const text = await response.text();
    const payload = parsePayload(text);
    if (response.ok) return payload;

    if (response.status === 403 && retryAfterCsrfRefresh && isCsrfFailure(payload)) {
        csrfTokenPromise = null;
        return api(path, {...options, retryAfterCsrfRefresh: false});
    }

    if (response.status === 401 && retryAfterRefresh && !isPublicAuthRequest(path)) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            return api(path, {...options, retryAfterRefresh: false});
        }
    }

    const details = Array.isArray(payload?.details) ? payload.details : [];
    const message = payload?.message || "Yêu cầu chưa thành công.";
    if (response.status === 401 && redirectOnUnauthorized && !isPublicAuthRequest(path)) {
        clearClientSession();
        window.dispatchEvent(new CustomEvent("auth:required"));
    }
    const retryAfterSeconds = Number.parseInt(response.headers.get("Retry-After") || "0", 10);
    throw new ApiError(
        details.length ? `${message} ${details.join(" ")}` : message,
        response.status,
        details,
        {
            retryAfterSeconds: Number.isFinite(retryAfterSeconds) ? retryAfterSeconds : 0,
            authAction: response.headers.get("X-Auth-Action")
        }
    );
}

async function send(path, requestOptions) {
    const headers = Object.assign({Accept: "application/json"}, requestOptions.headers || {});
    const method = (requestOptions.method || "GET").toUpperCase();
    if (requestOptions.body && !(requestOptions.body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
    }
    if (!isSafeMethod(method)) {
        const csrf = await getCsrfToken();
        headers[csrf.headerName] = csrf.token;
    }
    return fetch(apiUrl(path), {
        ...requestOptions,
        headers,
        credentials: "include"
    });
}

function refreshAccessToken() {
    if (!refreshPromise) {
        refreshPromise = send("/api/auth/refresh", {method: "POST"})
            .then(response => response.ok)
            .catch(() => false)
            .finally(() => {
                refreshPromise = null;
            });
    }
    return refreshPromise;
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

function clearClientSession() {
    state.user = null;
    state.sessionChecked = false;
    state.folders = [];
}

function isSafeMethod(method) {
    return ["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
}

function isCsrfFailure(payload) {
    return Array.isArray(payload?.details)
        && payload.details.some(detail => String(detail).includes("CSRF token"));
}

function isPublicAuthRequest(path) {
    return path === "/api/auth/login"
        || path === "/api/auth/register"
        || path === "/api/auth/logout"
        || path === "/api/auth/refresh"
        || path === "/api/auth/otp/verify"
        || path === "/api/auth/otp/resend"
        || path === "/api/auth/google/link";
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

export {api, apiUrl, ApiError};
