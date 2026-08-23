import {api, apiUrl} from "../../core/api.js";
import {returnPath} from "../../core/auth-routes.js";
import {navigate} from "../../core/navigation.js";
import {app, state} from "../../core/state.js";
import {cloneTemplate} from "../../core/templates.js";
import {showFormError} from "../../core/ui.js";

let activeOtpChallenge = null;
let resendCountdownTimer = null;
const REGISTRATION_PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{6,32}$/;
const REGISTRATION_PASSWORD_MESSAGE =
    "Mật khẩu phải dài từ 6 đến 32 ký tự và có ít nhất 1 chữ cái, 1 chữ số.";

function renderAuth(mode) {
    clearResendCountdown();
    activeOtpChallenge = null;
    document.body.className = "auth-body";
    const query = new URLSearchParams(window.location.search);
    const templateName = mode === "login" && query.get("googleLink") === "1"
        ? "auth/google-link"
        : `auth/${mode}`;
    app.replaceChildren(cloneTemplate(templateName));

    if (templateName === "auth/login") {
        const googleLogin = app.querySelector("[data-google-login]");
        googleLogin.href = apiUrl(
            `/api/auth/google/start?returnTo=${encodeURIComponent(returnPath())}`
        );
        if (query.has("oauthError")) {
            showFormError(
                app.querySelector("[data-form-error]"),
                query.get("oauthError") === "notConfigured"
                    ? "Đăng nhập Google chưa được cấu hình."
                    : "Google chưa thể xác thực tài khoản. Vui lòng thử lại."
            );
        }
    }
    if (templateName === "auth/register") {
        setupRegistrationPasswordValidation();
    }
}

function setupRegistrationPasswordValidation() {
    const form = app.querySelector('[data-auth-form="register"]');
    const passwordInput = form?.querySelector('[name="password"]');
    const passwordError = form?.querySelector("[data-password-error]");
    if (!form || !passwordInput || !passwordError) return;

    passwordInput.addEventListener("input", () => {
        const shouldShowError = !passwordError.hidden || passwordInput.value.length > 0;
        validateRegistrationPassword(form, shouldShowError);
    });
    passwordInput.addEventListener("blur", () => validateRegistrationPassword(form, true));
}

function validateRegistrationPassword(form, showError) {
    const passwordInput = form.querySelector('[name="password"]');
    const passwordError = form.querySelector("[data-password-error]");
    const isValid = REGISTRATION_PASSWORD_PATTERN.test(passwordInput?.value || "");

    if (passwordInput) {
        passwordInput.setAttribute("aria-invalid", String(!isValid));
    }
    if (!passwordError) return isValid;
    if (!isValid && showError) {
        showFormError(passwordError, REGISTRATION_PASSWORD_MESSAGE);
    } else {
        passwordError.hidden = true;
        passwordError.textContent = "";
    }
    return isValid;
}

async function handleAuthSubmit(form) {
    const mode = form.dataset.authForm;
    const errorBox = form.querySelector("[data-form-error]");
    errorBox.hidden = true;
    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    setLoginSubmitting(form, true);

    try {
        if (mode === "otp") {
            await verifyOtp(form);
            return;
        }
        if (mode === "google-link") {
            await linkGoogle(form);
            return;
        }

        const data = Object.fromEntries(new FormData(form).entries());
        if (mode === "register" && !validateRegistrationPassword(form, true)) {
            return;
        }
        if (mode === "register" && data.password !== data.confirmPassword) {
            showFormError(errorBox, "Mật khẩu xác nhận chưa khớp.");
            return;
        }

        const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
        const payload = await api(endpoint, {method: "POST", body: JSON.stringify(data)});
        handleAuthFlow(payload);
    } catch (error) {
        if (mode === "otp" && error.authAction === "LOGIN") {
            clearResendCountdown();
            activeOtpChallenge = null;
            navigate("/login", true);
            return;
        }
        showFormError(errorBox, error.message);
    } finally {
        if (submitButton.isConnected) {
            submitButton.disabled = false;
            setLoginSubmitting(form, false);
        }
    }
}

function setLoginSubmitting(form, isSubmitting) {
    if (form.dataset.authForm !== "login") return;

    const submitButton = form.querySelector('button[type="submit"]');
    const submitLabel = form.querySelector("[data-submit-label]");
    const submitSpinner = form.querySelector("[data-submit-spinner]");
    const progress = form.querySelector("[data-login-progress]");
    if (!submitButton || !submitLabel || !submitSpinner || !progress) return;

    form.setAttribute("aria-busy", String(isSubmitting));
    submitButton.setAttribute("aria-busy", String(isSubmitting));
    submitButton.classList.toggle("is-loading", isSubmitting);
    submitLabel.textContent = isSubmitting ? "Đang đăng nhập..." : "Đăng nhập";
    submitSpinner.hidden = !isSubmitting;
    progress.hidden = !isSubmitting;
}

async function verifyOtp(form) {
    if (!activeOtpChallenge) {
        throw new Error("Phiên OTP không còn tồn tại. Vui lòng đăng nhập lại.");
    }
    const data = new FormData(form);
    const code = String(data.get("code") || "").trim();
    if (!/^\d{6}$/.test(code)) {
        throw new Error("OTP phải gồm đúng 6 chữ số.");
    }

    const payload = await api("/api/auth/otp/verify", {
        method: "POST",
        body: JSON.stringify({
            challengeId: activeOtpChallenge.challengeId,
            code,
            rememberDevice: data.get("rememberDevice") === "on"
        })
    });
    handleAuthFlow(payload);
}

async function linkGoogle(form) {
    const password = String(new FormData(form).get("password") || "");
    const session = await api("/api/auth/google/link", {
        method: "POST",
        body: JSON.stringify({password})
    });
    completeAuthentication(session);
}

async function handleOtpResend(button) {
    const errorBox = app.querySelector("[data-form-error]");
    if (!activeOtpChallenge) {
        showFormError(errorBox, "Phiên OTP không còn tồn tại. Vui lòng đăng nhập lại.");
        return;
    }

    button.disabled = true;
    errorBox.hidden = true;
    try {
        const payload = await api("/api/auth/otp/resend", {
            method: "POST",
            body: JSON.stringify({challengeId: activeOtpChallenge.challengeId})
        });
        showOtp(payload);
    } catch (error) {
        if (error.authAction === "LOGIN") {
            clearResendCountdown();
            activeOtpChallenge = null;
            navigate("/login", true);
            return;
        }
        showFormError(errorBox, error.message);
        startResendCountdown(activeOtpChallenge.resendAvailableInSeconds);
    } finally {
        if (button.isConnected && !resendCountdownTimer) {
            updateResendButton(button, 0);
        }
    }
}

function handleAuthFlow(payload) {
    if (payload?.status === "OTP_REQUIRED") {
        showOtp(payload);
        return;
    }
    if (payload?.status === "AUTHENTICATED" && payload.session) {
        completeAuthentication(payload.session);
        return;
    }
    throw new Error("Phản hồi đăng nhập không hợp lệ.");
}

function showOtp(payload) {
    clearResendCountdown();
    activeOtpChallenge = {
        challengeId: payload.challengeId,
        maskedEmail: payload.maskedEmail,
        expiresInSeconds: payload.expiresInSeconds,
        resendAvailableInSeconds: Number(payload.resendAvailableInSeconds || 0),
        remainingSends: Number(payload.remainingSends || 0)
    };
    app.replaceChildren(cloneTemplate("auth/otp"));
    app.querySelector("[data-otp-email]").textContent = activeOtpChallenge.maskedEmail;
    startResendCountdown(activeOtpChallenge.resendAvailableInSeconds);
}

function completeAuthentication(session) {
    if (!session?.user) throw new Error("Không nhận được thông tin phiên đăng nhập.");
    clearResendCountdown();
    activeOtpChallenge = null;
    state.user = session.user;
    state.sessionChecked = true;
    navigate(returnPath(), true);
}

function startResendCountdown(seconds) {
    clearResendCountdown();
    const button = app.querySelector("[data-resend-otp]");
    if (!button || !activeOtpChallenge) return;
    if (activeOtpChallenge.remainingSends <= 0) {
        updateResendButton(button, 0);
        return;
    }

    const deadline = Date.now() + Math.max(0, seconds) * 1000;
    const update = () => {
        const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
        activeOtpChallenge.resendAvailableInSeconds = remaining;
        updateResendButton(button, remaining);
        if (remaining <= 0) {
            clearResendCountdown();
        }
    };
    update();
    if (activeOtpChallenge.resendAvailableInSeconds > 0) {
        resendCountdownTimer = window.setInterval(update, 250);
    }
}

function updateResendButton(button, remainingSeconds) {
    if (activeOtpChallenge?.remainingSends <= 0) {
        button.disabled = true;
        button.textContent = "Đã đạt giới hạn gửi";
        return;
    }
    button.disabled = remainingSeconds > 0;
    button.textContent = remainingSeconds > 0
        ? `Gửi lại sau ${remainingSeconds}s`
        : "Gửi lại mã OTP";
}

function clearResendCountdown() {
    if (resendCountdownTimer) {
        window.clearInterval(resendCountdownTimer);
        resendCountdownTimer = null;
    }
}

export {handleAuthSubmit, handleOtpResend, renderAuth};
