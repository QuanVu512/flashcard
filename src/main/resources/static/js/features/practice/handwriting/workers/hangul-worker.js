const HANGUL_SCRIPT_URL = "/vendor/handwriting/hangul-js/hangul.min.js";
const PDOLLAR_SCRIPT_URL = "/vendor/handwriting/pdollar/pdollar.js";
const JAMO_PATTERNS_URL = "/js/features/practice/handwriting/templates/hangul-jamo-patterns.js";

const INITIALS = new Set([
    "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
    "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
]);
const VOWELS = new Set([
    "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ",
    "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
]);
const FINALS = new Set([
    "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ",
    "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ",
    "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
]);
const ALL_JAMO = new Set([...INITIALS, ...VOWELS, ...FINALS]);
const VERTICAL_VOWELS = new Set(["ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅣ"]);
const HORIZONTAL_VOWELS = new Set(["ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ"]);

let templates = [];

function toPointCloud(strokes) {
    const points = [];
    strokes.forEach((stroke, strokeIndex) => {
        stroke.forEach(([x, y]) => {
            points.push(new Point(x, y, strokeIndex + 1));
        });
    });
    return points;
}

function compileTemplates() {
    templates = self.HANGUL_JAMO_PATTERNS.map(pattern => ({
        label: pattern.label,
        cloud: new PointCloud(pattern.label, toPointCloud(pattern.strokes))
    }));
}

function bounds(strokes) {
    const points = strokes.flat();
    const xs = points.map(point => point[0]);
    const ys = points.map(point => point[1]);
    const left = Math.min(...xs);
    const right = Math.max(...xs);
    const top = Math.min(...ys);
    const bottom = Math.max(...ys);
    return {
        centerX: (left + right) / 2,
        centerY: (top + bottom) / 2,
        width: Math.max(right - left, 1),
        height: Math.max(bottom - top, 1)
    };
}

function rankJamo(strokes, allowedLabels, limit) {
    const points = toPointCloud(strokes);
    if (points.length < 2) return [];

    const candidate = new PointCloud("", points);
    const bestByLabel = new Map();
    templates.forEach(template => {
        if (!allowedLabels.has(template.label)) return;

        const distance = GreedyCloudMatch(candidate.Points, template.cloud);
        const current = bestByLabel.get(template.label);
        if (!current || distance < current.distance) {
            bestByLabel.set(template.label, {label: template.label, distance});
        }
    });

    return [...bestByLabel.values()]
        .sort((first, second) => first.distance - second.distance)
        .slice(0, limit);
}

function layoutPenalty(initialStrokes, vowelStrokes, vowel, finalStrokes) {
    const initialBounds = bounds(initialStrokes);
    const vowelBounds = bounds(vowelStrokes);
    let penalty = 0;

    if (VERTICAL_VOWELS.has(vowel)) {
        const tolerance = Math.max(initialBounds.width, vowelBounds.width) * 0.18;
        if (initialBounds.centerX > vowelBounds.centerX + tolerance) penalty += 0.45;
    } else if (HORIZONTAL_VOWELS.has(vowel)) {
        const tolerance = Math.max(initialBounds.height, vowelBounds.height) * 0.18;
        if (initialBounds.centerY > vowelBounds.centerY + tolerance) penalty += 0.45;
    }

    if (finalStrokes?.length) {
        const finalBounds = bounds(finalStrokes);
        const upperCenter = Math.max(initialBounds.centerY, vowelBounds.centerY);
        if (finalBounds.centerY <= upperCenter) penalty += 0.55;
    }

    return penalty;
}

function addCandidate(candidateCosts, text, cost) {
    if (!text || text.length !== 1) return;

    const currentCost = candidateCosts.get(text);
    if (currentCost === undefined || cost < currentCost) {
        candidateCosts.set(text, cost);
    }
}

function recognize(strokes, limit) {
    const candidateCosts = new Map();
    const segmentCache = new Map();
    const strokeCount = strokes.length;

    const rankSegment = (start, end, role, allowedLabels, matchLimit = 3) => {
        const key = `${start}:${end}:${role}`;
        if (!segmentCache.has(key)) {
            segmentCache.set(
                key,
                rankJamo(strokes.slice(start, end), allowedLabels, matchLimit)
            );
        }
        return segmentCache.get(key);
    };

    rankSegment(0, strokeCount, "all", ALL_JAMO, limit).forEach(match => {
        addCandidate(candidateCosts, match.label, match.distance + 0.35);
    });

    const maxInitialEnd = Math.min(strokeCount - 1, 10);
    for (let initialEnd = 1; initialEnd <= maxInitialEnd; initialEnd++) {
        const initialMatches = rankSegment(0, initialEnd, "initial", INITIALS);
        const maxVowelEnd = Math.min(strokeCount, initialEnd + 7);

        for (let vowelEnd = initialEnd + 1; vowelEnd <= maxVowelEnd; vowelEnd++) {
            const vowelMatches = rankSegment(initialEnd, vowelEnd, "vowel", VOWELS);
            const finalStrokeCount = strokeCount - vowelEnd;
            if (finalStrokeCount > 12) continue;

            const finalMatches = finalStrokeCount > 0
                ? rankSegment(vowelEnd, strokeCount, "final", FINALS)
                : [{label: "", distance: 0}];

            initialMatches.forEach(initial => {
                vowelMatches.forEach(vowel => {
                    finalMatches.forEach(final => {
                        const assembled = Hangul.assemble([
                            initial.label,
                            vowel.label,
                            ...(final.label ? [final.label] : [])
                        ]);
                        if (!Hangul.isComplete(assembled)) return;

                        const componentCount = final.label ? 3 : 2;
                        const averageDistance = (
                            initial.distance + vowel.distance + final.distance
                        ) / componentCount;
                        const penalty = layoutPenalty(
                            strokes.slice(0, initialEnd),
                            strokes.slice(initialEnd, vowelEnd),
                            vowel.label,
                            final.label ? strokes.slice(vowelEnd) : undefined
                        );
                        addCandidate(candidateCosts, assembled, averageDistance + penalty - 0.25);
                    });
                });
            });
        }
    }

    return [...candidateCosts.entries()]
        .sort((first, second) => first[1] - second[1])
        .slice(0, limit)
        .map(([candidate]) => candidate);
}

function initialize() {
    self.window = self;
    importScripts(HANGUL_SCRIPT_URL, PDOLLAR_SCRIPT_URL, JAMO_PATTERNS_URL);
    compileTemplates();
    self.postMessage({type: "ready"});
}

self.addEventListener("message", event => {
    const {id, strokes, limit} = event.data;
    try {
        self.postMessage({type: "result", id, candidates: recognize(strokes, limit)});
    } catch (error) {
        self.postMessage({type: "error", id, message: error.message});
    }
});

try {
    initialize();
} catch (error) {
    self.postMessage({type: "initialization-error", message: error.message});
}
