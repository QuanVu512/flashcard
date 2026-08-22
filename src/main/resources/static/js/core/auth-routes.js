const DEFAULT_AFTER_LOGIN_PATH = "/library";

function loginPath(pathname = window.location.pathname, search = window.location.search) {
    const requestedPath = `${pathname}${search}`;
    return isDefaultDestination(requestedPath)
        ? "/login"
        : `/login?returnTo=${encodeURIComponent(requestedPath)}`;
}

function returnPath(search = window.location.search) {
    const requestedPath = new URLSearchParams(search).get("returnTo");
    return isSafeLocalPath(requestedPath) && !isDefaultDestination(requestedPath)
        ? requestedPath
        : DEFAULT_AFTER_LOGIN_PATH;
}

function isDefaultDestination(path) {
    return path === "/" || path === DEFAULT_AFTER_LOGIN_PATH;
}

function isSafeLocalPath(path) {
    if (typeof path !== "string" || !path.startsWith("/")) return false;
    try {
        return new URL(path, window.location.origin).origin === window.location.origin;
    } catch {
        return false;
    }
}

export {loginPath, returnPath};
