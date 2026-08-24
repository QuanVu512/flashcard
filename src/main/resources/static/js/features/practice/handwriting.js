import {createCandidatePanel} from "./handwriting/candidate-panel.js";
import {createStrokeCapture} from "./handwriting/stroke-capture.js";

const LOCAL_RECOGNIZERS = {
    "zh-Hans": {
        async preload() {
            const {preloadHanzi} = await import("./handwriting/recognizers/hanzi-recognizer.js");
            return preloadHanzi();
        },
        async recognize(strokes) {
            const {recognizeHanzi} = await import("./handwriting/recognizers/hanzi-recognizer.js");
            return recognizeHanzi(strokes);
        }
    },
    ja: {
        async preload() {
            const {preloadKanji} = await import("./handwriting/recognizers/kanji-recognizer.js");
            return preloadKanji();
        },
        async recognize(strokes) {
            const {recognizeKanji} = await import("./handwriting/recognizers/kanji-recognizer.js");
            return recognizeKanji(strokes);
        }
    },
    ko: {
        async preload() {
            const {preloadHangul} = await import("./handwriting/recognizers/hangul-recognizer.js");
            return preloadHangul();
        },
        async recognize(strokes) {
            const {recognizeHangul} = await import("./handwriting/recognizers/hangul-recognizer.js");
            return recognizeHangul(strokes);
        }
    }
};

function setupHandwritingCanvas(root) {
    const canvas = root.querySelector("[data-handwriting-canvas]");
    const status = root.querySelector("[data-handwriting-status]");
    const answerInput = root.querySelector('[name="answer"]');
    const languageSelect = root.querySelector("[data-handwriting-language]");
    if (!canvas || canvas.dataset.ready === "true") return;

    canvas.dataset.ready = "true";
    let recognitionVersion = 0;
    let recognitionTimer;
    let latestStrokes = [];

    const candidates = createCandidatePanel(root, candidate => {
        answerInput.value += candidate;
        answerInput.dispatchEvent(new Event("input", {bubbles: true}));
        clearDrawing(false);
        answerInput.focus();
    });

    const capture = createStrokeCapture(
        canvas,
        strokes => {
            latestStrokes = strokes;
            clearTimeout(recognitionTimer);
            const recognizer = LOCAL_RECOGNIZERS[languageSelect?.value];
            const version = ++recognitionVersion;
            if (!recognizer) {
                candidates.clear();
                return;
            }

            candidates.loading();
            recognitionTimer = setTimeout(async () => {
                try {
                    const matches = await recognizer.recognize(strokes);
                    if (version === recognitionVersion) candidates.show(matches);
                } catch (error) {
                    if (version === recognitionVersion) candidates.clear();
                    console.warn("Local handwriting recognition failed.", error);
                }
            }, 220);
        },
        () => {
            clearTimeout(recognitionTimer);
            recognitionVersion++;
            candidates.clear();
        }
    );

    function clearDrawing(announce = true) {
        clearTimeout(recognitionTimer);
        recognitionVersion++;
        capture.clear();
        latestStrokes = [];
        candidates.clear();
        if (!announce) return;

        status.textContent = "Bảng vẽ đã được làm sạch.";
        status.className = "handwriting-status";
    }

    root.querySelector("[data-clear-handwriting]")?.addEventListener("click", () => {
        clearDrawing();
    });
    languageSelect?.addEventListener("change", () => {
        clearDrawing(false);
        preloadSelectedRecognizer();
    });
    root.querySelector("[data-recognize-handwriting]")?.addEventListener("click", async () => {
        const recognizer = LOCAL_RECOGNIZERS[languageSelect?.value];
        if (!recognizer) {
            status.textContent = "Hãy chọn ngôn ngữ nhận dạng.";
            status.className = "handwriting-status error";
            return;
        }
        if (!capture.hasStrokes()) {
            status.textContent = "Hãy viết một chữ lên bảng trước đã.";
            status.className = "handwriting-status error";
            return;
        }

        status.textContent = "Đang nhận dạng chữ viết...";
        status.className = "handwriting-status";
        try {
            const matches = await recognizer.recognize(latestStrokes);
            if (!matches.length) {
                status.textContent = "Chưa nhận ra chữ nào.";
                status.className = "handwriting-status error";
                return;
            }
            answerInput.value = matches[0];
            answerInput.dispatchEvent(new Event("input", {bubbles: true}));
            candidates.show(matches);
            status.textContent = "Đã nhận dạng xong.";
            status.className = "handwriting-status success";
        } catch (error) {
            status.textContent = error.message;
            status.className = "handwriting-status error";
        }
    });

    function preloadSelectedRecognizer() {
        LOCAL_RECOGNIZERS[languageSelect?.value]?.preload().catch(error => {
            console.warn("Local handwriting recognizer preload failed.", error);
        });
    }

    preloadSelectedRecognizer();
}

export {setupHandwritingCanvas};
