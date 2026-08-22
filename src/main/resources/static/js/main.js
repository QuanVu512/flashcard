import {preloadTemplates} from "./core/templates.js";
import {registerGlobalEvents} from "./app/events.js";
import {redirectToLogin, renderRoute} from "./app/router.js";

const templates = [
    "auth/login",
    "auth/register",
    "fragments/shell",
    "library",
    "set-form",
    "fragments/set-card",
    "fragments/card-editor",
    "fragments/set-mode",
    "admin/dashboard",
    "error/generic"
];

await preloadTemplates(templates);
registerGlobalEvents();
window.addEventListener("popstate", renderRoute);
window.addEventListener("auth:required", redirectToLogin);
await renderRoute();
