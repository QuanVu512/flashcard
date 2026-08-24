function createRecognitionWorker(workerUrl) {
    let worker;
    let readyPromise;
    let resolveReady;
    let rejectReady;
    let requestSequence = 0;
    let activeRequest;
    let queuedRequest;

    const rejectPending = error => {
        if (activeRequest) {
            clearTimeout(activeRequest.timeoutId);
            activeRequest.reject(error);
            activeRequest = undefined;
        }
        if (queuedRequest) {
            queuedRequest.reject(error);
            queuedRequest = undefined;
        }
    };

    const failWorker = error => {
        rejectReady?.(error);
        rejectPending(error);
        worker?.terminate();
        worker = undefined;
        readyPromise = undefined;
        resolveReady = undefined;
        rejectReady = undefined;
    };

    const dispatchNext = () => {
        if (activeRequest || !queuedRequest) return;

        const request = queuedRequest;
        queuedRequest = undefined;
        const id = ++requestSequence;
        const timeoutId = setTimeout(() => {
            failWorker(new Error("Nhận dạng chữ viết tay quá thời gian chờ."));
        }, 30000);

        activeRequest = {...request, id, timeoutId};
        worker.postMessage({id, strokes: request.strokes, limit: request.limit});
    };

    const enqueueLatest = (strokes, limit) => new Promise((resolve, reject) => {
        if (queuedRequest) queuedRequest.resolve([]);
        queuedRequest = {strokes, limit, resolve, reject};
        dispatchNext();
    });

    const ensureReady = () => {
        if (readyPromise) return readyPromise;

        readyPromise = new Promise((resolve, reject) => {
            resolveReady = resolve;
            rejectReady = reject;
        });
        worker = new Worker(workerUrl);

        worker.addEventListener("message", event => {
            const message = event.data || {};
            if (message.type === "ready") {
                resolveReady();
                return;
            }
            if (message.type === "initialization-error") {
                const error = new Error(message.message || "Không thể tải bộ nhận dạng chữ viết tay.");
                failWorker(error);
                return;
            }

            if (!activeRequest || activeRequest.id !== message.id) return;

            const request = activeRequest;
            clearTimeout(activeRequest.timeoutId);
            activeRequest = undefined;
            if (message.type === "error") {
                const error = new Error(message.message || "Không thể nhận dạng chữ viết tay.");
                request.reject(error);
                failWorker(error);
                return;
            }

            request.resolve(message.candidates || []);
            dispatchNext();
        });

        worker.addEventListener("error", event => {
            const error = new Error(event.message || "Bộ nhận dạng chữ viết tay đã dừng.");
            failWorker(error);
        });

        return readyPromise;
    };

    return {
        preload: ensureReady,
        async recognize(strokes, limit = 8) {
            await ensureReady();
            return enqueueLatest(strokes, limit);
        }
    };
}

export {createRecognitionWorker};
