const KANJI_SCRIPT_URL = "/vendor/handwriting/kanji-canvas/kanji-canvas.min.js";
const KANJI_PATTERNS_URL = "/vendor/handwriting/kanji-canvas/ref-patterns.js";
const CANVAS_ID = "worker-canvas";

self.window = self;
self.document = {
    addEventListener() {},
    getElementById() {
        return null;
    }
};

const context = {
    beginPath() {},
    clearRect() {},
    closePath() {},
    fillRect() {},
    fillText() {},
    lineTo() {},
    moveTo() {},
    stroke() {},
    strokeText() {}
};

function initialize() {
    importScripts(KANJI_SCRIPT_URL, KANJI_PATTERNS_URL);
    KanjiCanvas[`canvas_${CANVAS_ID}`] = {dataset: {strokeNumbers: "false"}};
    KanjiCanvas[`ctx_${CANVAS_ID}`] = context;
    KanjiCanvas[`w_${CANVAS_ID}`] = 900;
    KanjiCanvas[`h_${CANVAS_ID}`] = 240;
    KanjiCanvas[`recordedPattern_${CANVAS_ID}`] = [];
    self.postMessage({type: "ready"});
}

self.addEventListener("message", event => {
    const {id, strokes, limit} = event.data;
    try {
        KanjiCanvas[`recordedPattern_${CANVAS_ID}`] = strokes;
        const result = KanjiCanvas.recognize(CANVAS_ID) || "";
        const candidates = [...new Set(result.trim().split(/\s+/))]
            .filter(Boolean)
            .slice(0, limit);
        self.postMessage({type: "result", id, candidates});
    } catch (error) {
        self.postMessage({type: "error", id, message: error.message});
    }
});

try {
    initialize();
} catch (error) {
    self.postMessage({type: "initialization-error", message: error.message});
}
