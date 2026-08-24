import {createRecognitionWorker} from "./worker-client.js";
import {normalizeStrokes} from "../stroke-normalizer.js";

const recognitionWorker = createRecognitionWorker(
    new URL("../workers/hanzi-worker.js", import.meta.url)
);

function preloadHanzi() {
    return recognitionWorker.preload();
}

function recognizeHanzi(strokes, limit = 8) {
    return recognitionWorker.recognize(normalizeStrokes(strokes), limit);
}

export {preloadHanzi, recognizeHanzi};
