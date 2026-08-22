const state = {
    user: null,
    sessionChecked: false,
    folders: [],
    keyHandler: null,
    timer: null,
    suggestionTimers: new WeakMap()
};

const app = document.querySelector("#app");

export {state, app};
