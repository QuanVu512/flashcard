import {createRecognitionWorker} from "./worker-client.js";
import {normalizeStrokes} from "../stroke-normalizer.js";

const recognitionWorker = createRecognitionWorker(
    new URL("../workers/hangul-worker.js", import.meta.url)
);

function preloadHangul() {
    return recognitionWorker.preload();
}

function recognizeHangul(strokes, limit = 8) {
    return recognitionWorker.recognize(normalizeStrokes(strokes), limit);
}

export {preloadHangul, recognizeHangul};
