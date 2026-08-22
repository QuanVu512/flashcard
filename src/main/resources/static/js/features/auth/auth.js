import {api} from "../../core/api.js";
import {returnPath} from "../../core/auth-routes.js";
import {navigate} from "../../core/navigation.js";
import {app, state} from "../../core/state.js";
import {cloneTemplate} from "../../core/templates.js";
import {showFormError} from "../../core/ui.js";

function renderAuth(mode) {
    document.body.className = "auth-body";
    app.replaceChildren(cloneTemplate(`auth/${mode}`));
}

async function handleAuthSubmit(form) {
    const mode = form.dataset.authForm;
    const errorBox = form.querySelector("[data-form-error]");
    errorBox.hidden = true;
    const data = Object.fromEntries(new FormData(form).entries());
    if (mode === "register" && data.password !== data.confirmPassword) {
        showFormError(errorBox, "Mật khẩu xác nhận chưa khớp.");
        return;
    }
    try {
        const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
        const payload = await api(endpoint, {method: "POST", body: JSON.stringify(data)});
        state.user = payload.user;
        state.sessionChecked = true;
        navigate(returnPath(), true);
    } catch (error) {
        showFormError(errorBox, error.message);
    }
}

export {handleAuthSubmit, renderAuth};
