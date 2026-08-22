import {navigate, logout} from "../core/navigation.js";
import {state} from "../core/state.js";
import {toggleUserStatus} from "../features/admin/admin.js";
import {handleAuthSubmit, handleOtpResend} from "../features/auth/auth.js";
import {
    createFolderFromPrompt,
    deleteSetById,
    renumberCards,
    renderCardEditor,
    saveSet,
    suggestForRow
} from "../features/library/library.js";
import {renderError} from "./router.js";

function registerGlobalEvents() {
    document.addEventListener("click", onGlobalClick);
    document.addEventListener("submit", onGlobalSubmit);
    document.addEventListener("input", onGlobalInput);
}

function onGlobalClick(event) {
    if (event.target.closest("[data-stop]")) {
        event.stopPropagation();
    }

    const nav = event.target.closest("[data-nav]");
    if (nav) {
        event.preventDefault();
        navigate(nav.dataset.nav);
        return;
    }
    if (event.target.closest("[data-logout]")) {
        logout().catch(error => renderError(error.message || "Không thể đăng xuất."));
        return;
    }
    if (event.target.closest("[data-create-folder]")) {
        createFolderFromPrompt().catch(error => renderError(error.message));
        return;
    }
    const resendOtp = event.target.closest("[data-resend-otp]");
    if (resendOtp) {
        handleOtpResend(resendOtp);
        return;
    }

    const deleteSet = event.target.closest("[data-delete-set]");
    if (deleteSet) {
        deleteSetById(deleteSet.dataset.deleteSet).catch(error => renderError(error.message));
        return;
    }
    const addCard = event.target.closest("[data-add-card]");
    if (addCard) {
        const list = document.querySelector("[data-card-list]");
        list.insertAdjacentHTML("beforeend", renderCardEditor({}, list.querySelectorAll("[data-card-row]").length));
        return;
    }
    const removeCard = event.target.closest("[data-remove-card]");
    if (removeCard) {
        removeCard.closest("[data-card-row]").remove();
        renumberCards();
        return;
    }
    const suggestion = event.target.closest("[data-suggestion-value]");
    if (suggestion) {
        const field = suggestion.closest(".suggest-wrap").querySelector("input");
        field.value = suggestion.dataset.suggestionValue;
        field.dispatchEvent(new Event("input", {bubbles: true}));
        return;
    }
    const toggleUser = event.target.closest("[data-toggle-user]");
    if (toggleUser) {
        toggleUserStatus(toggleUser.dataset.toggleUser, toggleUser.dataset.enabled === "true")
            .catch(error => renderError(error.message));
    }
}

function onGlobalSubmit(event) {
    const authForm = event.target.closest("[data-auth-form]");
    if (authForm) {
        event.preventDefault();
        handleAuthSubmit(authForm);
        return;
    }
    const searchForm = event.target.closest("[data-search-form]");
    const topSearch = event.target.closest("[data-top-search]");
    if (searchForm || topSearch) {
        event.preventDefault();
        const q = new FormData(searchForm || topSearch).get("q") || "";
        navigate(q ? `/library?q=${encodeURIComponent(q)}` : "/library");
        return;
    }
    const setForm = event.target.closest("[data-set-form]");
    if (setForm) {
        event.preventDefault();
        saveSet(setForm);
        return;
    }
    const testSetup = event.target.closest("[data-test-setup-form]");
    if (testSetup) {
        event.preventDefault();
        const data = new FormData(testSetup);
        const setId = testSetup.dataset.testSetupForm;
        navigate(`/sets/${setId}/test?questionCount=${data.get("questionCount")}&minutes=${data.get("minutes")}&testMode=${data.get("testMode")}`);
        return;
    }
    const learnMode = event.target.closest("[data-learn-mode-form]");
    if (learnMode) {
        event.preventDefault();
        const data = new FormData(learnMode);
        const setId = learnMode.dataset.learnModeForm;
        navigate(`/sets/${setId}/learn?testMode=${data.get("testMode")}&answerMode=${data.get("answerMode")}`);
    }
}

function onGlobalInput(event) {
    const termInput = event.target.closest('[data-card-field="term"]');
    if (!termInput) return;
    const row = termInput.closest("[data-card-row]");
    const timer = state.suggestionTimers.get(termInput);
    if (timer) clearTimeout(timer);
    state.suggestionTimers.set(termInput, setTimeout(() => suggestForRow(row, termInput.value), 450));
}

export {registerGlobalEvents};
