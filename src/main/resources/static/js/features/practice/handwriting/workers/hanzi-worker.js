const HANZI_SCRIPT_URL = "/vendor/handwriting/hanzi-lookup/hanzi_lookup.js";
const HANZI_WASM_URL = "/vendor/handwriting/hanzi-lookup/hanzi_lookup_bg.wasm";

async function initialize() {
    importScripts(HANZI_SCRIPT_URL);
    await wasm_bindgen(HANZI_WASM_URL);
    self.postMessage({type: "ready"});
}

self.addEventListener("message", event => {
    const {id, strokes, limit} = event.data;
    try {
        const matches = JSON.parse(wasm_bindgen.lookup(strokes, limit));
        self.postMessage({
            type: "result",
            id,
            candidates: matches.map(match => match.hanzi)
        });
    } catch (error) {
        self.postMessage({type: "error", id, message: error.message});
    }
});

initialize().catch(error => {
    self.postMessage({type: "initialization-error", message: error.message});
});
