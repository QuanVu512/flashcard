import {createRecognitionWorker} from "./worker-client.js";

const recognitionWorker = createRecognitionWorker(
    new URL("../workers/kanji-worker.js", import.meta.url)
);

function preloadKanji() {
    return recognitionWorker.preload();
}

function recognizeKanji(strokes, limit = 8) {
    return recognitionWorker.recognize(strokes, limit);
}

export {preloadKanji, recognizeKanji};
