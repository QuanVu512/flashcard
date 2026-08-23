import {cloneTemplate} from "../../core/templates.js";

function showOtpNotice(container) {
    const fragment = cloneTemplate("auth/otp-notice");
    const dialog = fragment.querySelector("[data-otp-notice]");
    const confirmButton = fragment.querySelector("[data-otp-notice-confirm]");
    if (!dialog || !confirmButton) return;

    const closeNotice = () => dialog.close();
    confirmButton.addEventListener("click", closeNotice, {once: true});
    dialog.addEventListener("cancel", event => event.preventDefault());
    dialog.addEventListener("close", () => {
        dialog.remove();
        container.querySelector(".otp-input")?.focus();
    }, {once: true});

    container.append(fragment);
    dialog.showModal();
}

export {showOtpNotice};
