function createStrokeCapture(canvas, onStrokeComplete, onStrokeStart = () => {}) {
    const context = canvas.getContext("2d");
    const strokes = [];
    let activePointerId = null;
    let currentStroke = null;

    context.lineWidth = 5;
    context.lineCap = "round";
    context.lineJoin = "round";
    context.strokeStyle = "#17213f";

    const pointFromEvent = event => {
        const rect = canvas.getBoundingClientRect();
        return [
            ((event.clientX - rect.left) / rect.width) * canvas.width,
            ((event.clientY - rect.top) / rect.height) * canvas.height
        ];
    };

    const addPoint = event => {
        const point = pointFromEvent(event);
        const previous = currentStroke?.[currentStroke.length - 1];
        if (previous && previous[0] === point[0] && previous[1] === point[1]) return;

        currentStroke.push(point);
        context.lineTo(point[0], point[1]);
        context.stroke();
    };

    const finishStroke = event => {
        if (activePointerId !== event.pointerId || !currentStroke) return;

        addPoint(event);
        if (currentStroke.length === 1) {
            const [x, y] = currentStroke[0];
            const endpoint = [x + 0.01, y + 0.01];
            currentStroke.push(endpoint);
            context.lineTo(endpoint[0], endpoint[1]);
            context.stroke();
        }

        strokes.push(currentStroke);
        currentStroke = null;
        activePointerId = null;
        onStrokeComplete(strokes.map(stroke => stroke.map(point => [...point])));
    };

    canvas.addEventListener("pointerdown", event => {
        if (activePointerId !== null || event.button > 0) return;

        event.preventDefault();
        onStrokeStart();
        activePointerId = event.pointerId;
        currentStroke = [pointFromEvent(event)];
        canvas.setPointerCapture(event.pointerId);
        context.beginPath();
        context.moveTo(currentStroke[0][0], currentStroke[0][1]);
    });

    canvas.addEventListener("pointermove", event => {
        if (activePointerId !== event.pointerId || !currentStroke) return;

        event.preventDefault();
        const events = typeof event.getCoalescedEvents === "function"
            ? event.getCoalescedEvents()
            : [event];
        events.forEach(addPoint);
    });

    canvas.addEventListener("pointerup", finishStroke);
    canvas.addEventListener("pointercancel", finishStroke);

    return {
        clear() {
            context.clearRect(0, 0, canvas.width, canvas.height);
            strokes.length = 0;
            currentStroke = null;
            activePointerId = null;
        },
        hasStrokes() {
            return strokes.length > 0;
        }
    };
}

export {createStrokeCapture};
