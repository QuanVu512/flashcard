const DEFAULT_SAMPLE_SPACING = 8;
const DEFAULT_MAX_POINTS_PER_STROKE = 32;
const DEFAULT_MIN_POINT_DISTANCE = 1;
const TAP_OFFSET = 0.01;

function pointDistance(first, second) {
    return Math.hypot(second[0] - first[0], second[1] - first[1]);
}

function isValidPoint(point) {
    return Array.isArray(point)
        && point.length >= 2
        && Number.isFinite(point[0])
        && Number.isFinite(point[1]);
}

function sanitizeStroke(stroke, minimumDistance) {
    if (!Array.isArray(stroke)) return [];

    const validPoints = stroke
        .filter(isValidPoint)
        .map(point => [point[0], point[1]]);
    if (validPoints.length === 0) return [];

    const sanitized = [validPoints[0]];
    for (let index = 1; index < validPoints.length; index++) {
        const point = validPoints[index];
        if (pointDistance(sanitized[sanitized.length - 1], point) >= minimumDistance) {
            sanitized.push(point);
        }
    }

    const finalPoint = validPoints[validPoints.length - 1];
    if (pointDistance(sanitized[sanitized.length - 1], finalPoint) > 0) {
        sanitized.push(finalPoint);
    }
    if (sanitized.length === 1) {
        const [x, y] = sanitized[0];
        sanitized.push([x + TAP_OFFSET, y + TAP_OFFSET]);
    }

    return sanitized;
}

function resampleStroke(stroke, sampleSpacing, maxPoints) {
    if (stroke.length <= 2) return stroke.map(point => [...point]);

    const cumulativeLengths = [0];
    for (let index = 1; index < stroke.length; index++) {
        cumulativeLengths.push(
            cumulativeLengths[index - 1] + pointDistance(stroke[index - 1], stroke[index])
        );
    }

    const totalLength = cumulativeLengths[cumulativeLengths.length - 1];
    if (totalLength === 0) {
        const [x, y] = stroke[0];
        return [[x, y], [x + TAP_OFFSET, y + TAP_OFFSET]];
    }

    const targetCount = Math.min(
        maxPoints,
        Math.max(2, Math.ceil(totalLength / sampleSpacing) + 1)
    );
    const interval = totalLength / (targetCount - 1);
    const result = [[...stroke[0]]];
    let segmentIndex = 1;

    for (let sampleIndex = 1; sampleIndex < targetCount - 1; sampleIndex++) {
        const targetDistance = interval * sampleIndex;
        while (
            segmentIndex < cumulativeLengths.length - 1
            && cumulativeLengths[segmentIndex] < targetDistance
        ) {
            segmentIndex++;
        }

        const segmentStart = stroke[segmentIndex - 1];
        const segmentEnd = stroke[segmentIndex];
        const segmentStartDistance = cumulativeLengths[segmentIndex - 1];
        const segmentLength = cumulativeLengths[segmentIndex] - segmentStartDistance;
        const ratio = segmentLength === 0
            ? 0
            : (targetDistance - segmentStartDistance) / segmentLength;
        result.push([
            segmentStart[0] + ((segmentEnd[0] - segmentStart[0]) * ratio),
            segmentStart[1] + ((segmentEnd[1] - segmentStart[1]) * ratio)
        ]);
    }

    result.push([...stroke[stroke.length - 1]]);
    return result;
}

function positiveNumber(value, fallback) {
    return Number.isFinite(value) && value > 0 ? value : fallback;
}

function positiveInteger(value, fallback) {
    return Number.isInteger(value) && value >= 2 ? value : fallback;
}

function normalizeStrokes(strokes, options = {}) {
    if (!Array.isArray(strokes)) return [];

    const sampleSpacing = positiveNumber(options.sampleSpacing, DEFAULT_SAMPLE_SPACING);
    const minimumDistance = positiveNumber(options.minimumDistance, DEFAULT_MIN_POINT_DISTANCE);
    const maxPoints = positiveInteger(options.maxPointsPerStroke, DEFAULT_MAX_POINTS_PER_STROKE);

    return strokes
        .map(stroke => sanitizeStroke(stroke, minimumDistance))
        .filter(stroke => stroke.length >= 2)
        .map(stroke => resampleStroke(stroke, sampleSpacing, maxPoints));
}

export {normalizeStrokes};
