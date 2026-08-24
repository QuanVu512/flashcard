package com.flashcardapp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diagnostic trace for the local Chinese handwriting flow.
 *
 * Symptom: suggestions disappear after a few strokes or repeated Clear -> draw cycles.
 * Expected: every valid latest stroke snapshot returns candidates and Clear starts a clean cycle.
 * Entry point: /sets/{setId}/learn?testMode=term&answerMode=write, language zh-Hans.
 * Reproduction: draw partial/multi-stroke characters, clear repeatedly, then draw again.
 * Visibility limit: traces the real stroke capture, worker client and WASM lookup, but not browser DOM/CSP.
 */
class HandwritingRecognitionLogicTraceTest {

    // REAL_INPUT: override only when Node is not available as "node" or more clear cycles are needed.
    private static final String NODE_COMMAND_ENV = "HANDWRITING_TRACE_NODE";
    private static final String CLEAR_CYCLES_ENV = "HANDWRITING_TRACE_CYCLES";
    private static final Duration TRACE_TIMEOUT = Duration.ofSeconds(60);

    @Test
    void tracesStrokeQueueAndRepeatedClearCycles() throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path strokeCapture = projectRoot.resolve("src/main/resources/static/js/features/practice/handwriting/stroke-capture.js");
        Path strokeNormalizer = projectRoot.resolve("src/main/resources/static/js/features/practice/handwriting/stroke-normalizer.js");
        Path workerClient = projectRoot.resolve("src/main/resources/static/js/features/practice/handwriting/recognizers/worker-client.js");
        Path hanziScript = projectRoot.resolve("src/main/resources/static/vendor/handwriting/hanzi-lookup/hanzi_lookup.js");
        Path hanziWasm = projectRoot.resolve("src/main/resources/static/vendor/handwriting/hanzi-lookup/hanzi_lookup_bg.wasm");

        assertThat(List.of(strokeCapture, strokeNormalizer, workerClient, hanziScript, hanziWasm))
                .allSatisfy(path -> assertThat(path).as("Required trace input %s", path).isRegularFile());

        String nodeCommand = environmentOrDefault(NODE_COMMAND_ENV, "node");
        String clearCycles = environmentOrDefault(CLEAR_CYCLES_ENV, "4");
        Path traceScript = Files.createTempFile("handwriting-recognition-logic-trace-", ".mjs");
        Path traceOutput = Files.createTempFile("handwriting-recognition-logic-trace-", ".log");

        try {
            Files.writeString(traceScript, NODE_TRACE_SCRIPT, StandardCharsets.UTF_8);
            Process process = startTraceProcess(
                    nodeCommand,
                    traceScript,
                    traceOutput,
                    strokeCapture,
                    strokeNormalizer,
                    workerClient,
                    hanziScript,
                    hanziWasm,
                    clearCycles
            );

            boolean finished = process.waitFor(TRACE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }

            String traceLog = Files.readString(traceOutput, StandardCharsets.UTF_8);
            System.out.print(traceLog);

            assertThat(finished)
                    .as("Handwriting trace exceeded %s seconds", TRACE_TIMEOUT.toSeconds())
                    .isTrue();
            assertThat(process.exitValue())
                    .as("Node trace failed. Structured trace:%n%s", traceLog)
                    .isZero();
            assertThat(traceLog)
                    .contains("[ASSERT] expected=latest-and-post-clear-candidates actual=available PASS");
        } finally {
            Files.deleteIfExists(traceScript);
            Files.deleteIfExists(traceOutput);
        }
    }

    private Process startTraceProcess(String nodeCommand,
                                      Path traceScript,
                                      Path traceOutput,
                                      Path strokeCapture,
                                      Path strokeNormalizer,
                                      Path workerClient,
                                      Path hanziScript,
                                      Path hanziWasm,
                                      String clearCycles) throws IOException {
        try {
            return new ProcessBuilder(
                    nodeCommand,
                    traceScript.toString(),
                    strokeCapture.toString(),
                    strokeNormalizer.toString(),
                    workerClient.toString(),
                    hanziScript.toString(),
                    hanziWasm.toString(),
                    clearCycles
            )
                    .redirectErrorStream(true)
                    .redirectOutput(traceOutput.toFile())
                    .start();
        } catch (IOException error) {
            throw new IOException(
                    "Cannot start Node. Set " + NODE_COMMAND_ENV + " to the local Node executable.",
                    error
            );
        }
    }

    private String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static final String NODE_TRACE_SCRIPT = """
            import {readFile} from "node:fs/promises";

            const [strokeCapturePath, strokeNormalizerPath, workerClientPath, hanziScriptPath, hanziWasmPath, cyclesText] = process.argv.slice(2);
            const clearCycles = Number.parseInt(cyclesText, 10);
            const sensitiveKey = /password|passwd|token|secret|cookie|authorization|session|credential|card|cvv/i;
            const traceStartedAt = performance.now();
            let traceSequence = 0;

            function sanitize(value, depth = 0, seen = new WeakSet()) {
                if (value === null || value === undefined || typeof value === "number" || typeof value === "boolean") return value;
                if (typeof value === "string") return value.length > 300 ? `${value.slice(0, 300)}…` : value;
                if (typeof value === "bigint") return `${value}n`;
                if (typeof value === "function") return `[Function ${value.name || "anonymous"}]`;
                if (typeof value !== "object") return `[${typeof value}]`;
                if (seen.has(value)) return "[Circular]";
                if (depth >= 4) return `[${value.constructor?.name || "Object"}]`;

                seen.add(value);
                if (Array.isArray(value)) {
                    return value.slice(0, 20).map(item => sanitize(item, depth + 1, seen));
                }

                const output = {};
                for (const [key, item] of Object.entries(value).slice(0, 20)) {
                    output[key] = sensitiveKey.test(key) ? "[REDACTED]" : sanitize(item, depth + 1, seen);
                }
                return output;
            }

            function trace(phase, boundary, detail = {}, startedAt = traceStartedAt) {
                const sequence = String(++traceSequence).padStart(3, "0");
                const durationMs = Math.round((performance.now() - startedAt) * 100) / 100;
                console.log(`[${sequence}] ${phase.padEnd(5)} ${boundary} detail=${JSON.stringify(sanitize(detail))} durationMs=${durationMs}`);
            }

            async function traced(boundary, input, action) {
                const startedAt = performance.now();
                trace("ENTER", boundary, {input}, startedAt);
                try {
                    const output = await action();
                    trace("EXIT", boundary, {output}, startedAt);
                    return output;
                } catch (error) {
                    trace("THROW", boundary, {error: {name: error.name, message: error.message}}, startedAt);
                    throw error;
                }
            }

            async function importSource(path) {
                const source = await readFile(path, "utf8");
                const encoded = Buffer.from(source, "utf8").toString("base64");
                return import(`data:text/javascript;base64,${encoded}`);
            }

            class TraceCanvas {
                constructor() {
                    this.width = 900;
                    this.height = 240;
                    this.listeners = new Map();
                    this.context = {
                        beginPath() {},
                        clearRect() {},
                        lineTo() {},
                        moveTo() {},
                        stroke() {}
                    };
                }

                addEventListener(type, listener) {
                    const listeners = this.listeners.get(type) || [];
                    listeners.push(listener);
                    this.listeners.set(type, listeners);
                }

                dispatch(type, point, pointerId = 1) {
                    const event = {
                        button: 0,
                        clientX: point[0],
                        clientY: point[1],
                        pointerId,
                        preventDefault() {}
                    };
                    for (const listener of this.listeners.get(type) || []) listener(event);
                }

                getBoundingClientRect() {
                    return {left: 0, top: 0, width: 900, height: 240};
                }

                getContext() {
                    return this.context;
                }

                setPointerCapture() {}
            }

            function drawStroke(canvas, points, pointerId) {
                const startedAt = performance.now();
                trace("ENTER", "canvas.stroke", {pointerId, pointCount: points.length}, startedAt);
                canvas.dispatch("pointerdown", points[0], pointerId);
                for (const point of points.slice(1, -1)) canvas.dispatch("pointermove", point, pointerId);
                canvas.dispatch("pointerup", points.at(-1), pointerId);
                trace("EXIT", "canvas.stroke", {pointerId}, startedAt);
            }

            function densifyStroke(stroke, pointsPerSegment = 40) {
                const dense = [];
                for (let index = 1; index < stroke.length; index++) {
                    const start = stroke[index - 1];
                    const end = stroke[index];
                    for (let step = 0; step < pointsPerSegment; step++) {
                        const ratio = step / pointsPerSegment;
                        dense.push([
                            start[0] + ((end[0] - start[0]) * ratio),
                            start[1] + ((end[1] - start[1]) * ratio)
                        ]);
                    }
                }
                dense.push([...stroke[stroke.length - 1]]);
                return dense;
            }

            function assertTrace(name, expected, actual, condition) {
                console.log(`[ASSERT] name=${name} expected=${expected} actual=${actual}`);
                if (!condition) throw new Error(`${name}: expected ${expected}, received ${actual}`);
            }

            async function main() {
                if (!Number.isInteger(clearCycles) || clearCycles < 1 || clearCycles > 20) {
                    throw new Error("HANDWRITING_TRACE_CYCLES must be an integer from 1 to 20.");
                }

                globalThis.self = globalThis;
                const wasmBytes = await readFile(hanziWasmPath);
                await traced("vendor.hanzi.load", {hanziScriptPath, byteLength: wasmBytes.length}, async () => {
                    await importSource(hanziScriptPath);
                    await globalThis.wasm_bindgen(wasmBytes);
                    return {loaded: true};
                });

                let fakeWorkerSequence = 0;
                class TraceRecognitionWorker {
                    constructor(workerUrl) {
                        this.id = ++fakeWorkerSequence;
                        this.listeners = new Map();
                        this.terminated = false;
                        trace("ENTER", "worker.construct", {workerId: this.id, workerUrl: String(workerUrl)});
                        queueMicrotask(async () => {
                            try {
                                await globalThis.wasm_bindgen(wasmBytes);
                                this.emit("message", {data: {type: "ready"}});
                                trace("EXIT", "worker.ready", {workerId: this.id});
                            } catch (error) {
                                this.emit("message", {data: {type: "initialization-error", message: error.message}});
                            }
                        });
                    }

                    addEventListener(type, listener) {
                        const listeners = this.listeners.get(type) || [];
                        listeners.push(listener);
                        this.listeners.set(type, listeners);
                    }

                    emit(type, event) {
                        if (this.terminated) return;
                        for (const listener of this.listeners.get(type) || []) listener(event);
                    }

                    postMessage(message) {
                        const startedAt = performance.now();
                        trace("ENTER", "worker.lookup", {
                            workerId: this.id,
                            requestId: message.id,
                            strokeCount: message.strokes.length,
                            pointCounts: message.strokes.map(stroke => stroke.length)
                        }, startedAt);
                        setTimeout(() => {
                            if (this.terminated) return;
                            try {
                                const matches = JSON.parse(globalThis.wasm_bindgen.lookup(message.strokes, message.limit));
                                trace("EXIT", "worker.lookup", {
                                    workerId: this.id,
                                    requestId: message.id,
                                    candidates: matches.map(match => match.hanzi)
                                }, startedAt);
                                this.emit("message", {
                                    data: {type: "result", id: message.id, candidates: matches.map(match => match.hanzi)}
                                });
                            } catch (error) {
                                trace("THROW", "worker.lookup", {
                                    workerId: this.id,
                                    requestId: message.id,
                                    error: {name: error.name, message: error.message}
                                }, startedAt);
                                this.emit("message", {data: {type: "error", id: message.id, message: error.message}});
                            }
                        }, 8);
                    }

                    terminate() {
                        this.terminated = true;
                        trace("EXIT", "worker.terminate", {workerId: this.id});
                    }
                }

                globalThis.Worker = TraceRecognitionWorker;
                const {createStrokeCapture} = await importSource(strokeCapturePath);
                const {normalizeStrokes} = await importSource(strokeNormalizerPath);
                const {createRecognitionWorker} = await importSource(workerClientPath);
                const recognition = createRecognitionWorker("trace://hanzi-worker");
                const canvas = new TraceCanvas();
                let snapshots = [];
                const capture = createStrokeCapture(
                        canvas,
                        strokes => {
                            snapshots.push(strokes);
                            trace("EXIT", "strokeCapture.complete", {
                                strokeCount: strokes.length,
                                pointCounts: strokes.map(stroke => stroke.length)
                            });
                        },
                        () => trace("ENTER", "strokeCapture.start")
                );

                const person = [
                    [[450, 35], [440, 70], [420, 110], [390, 155], [350, 195]],
                    [[450, 75], [475, 110], [505, 145], [535, 175], [570, 198]]
                ];
                const sun = [
                    [[350, 35], [350, 80], [350, 130], [350, 190]],
                    [[350, 35], [420, 35], [500, 35], [550, 35], [550, 90], [550, 145], [550, 190]],
                    [[350, 110], [420, 110], [490, 110], [550, 110]],
                    [[350, 190], [420, 190], [490, 190], [550, 190]]
                ];

                const densePerson = person.map(stroke => densifyStroke(stroke));
                const normalizedPerson = normalizeStrokes(densePerson);
                assertTrace(
                        "dense-stroke-input",
                        ">100-points-per-stroke",
                        densePerson.map(stroke => stroke.length),
                        densePerson.every(stroke => stroke.length > 100)
                );
                assertTrace(
                        "dense-stroke-normalization",
                        "2..32-points-per-stroke",
                        normalizedPerson.map(stroke => stroke.length),
                        normalizedPerson.every(stroke => stroke.length >= 2 && stroke.length <= 32)
                );
                const denseCandidates = await traced(
                        "recognition.normalizedDenseInput",
                        {
                            rawPointCounts: densePerson.map(stroke => stroke.length),
                            normalizedPointCounts: normalizedPerson.map(stroke => stroke.length)
                        },
                        () => recognition.recognize(normalizedPerson)
                );
                assertTrace("normalized-dense-candidates", ">0", denseCandidates.length, denseCandidates.length > 0);

                capture.clear();
                snapshots = [];
                drawStroke(canvas, [[450, 120]], 1);
                const tapSnapshot = snapshots.at(-1);
                assertTrace("single-point-normalization", ">=2-points", tapSnapshot[0].length, tapSnapshot[0].length >= 2);
                const tapCandidates = await traced(
                        "recognition.singleTap",
                        {pointCounts: tapSnapshot.map(stroke => stroke.length)},
                        () => recognition.recognize(tapSnapshot)
                );
                assertTrace("single-tap-candidates", ">0", tapCandidates.length, tapCandidates.length > 0);

                capture.clear();
                snapshots = [];
                sun.forEach((stroke, index) => drawStroke(canvas, stroke, index + 10));
                const queueResults = await traced(
                        "recognition.latestQueue",
                        {snapshotCount: snapshots.length},
                        () => Promise.all(snapshots.map(snapshot => recognition.recognize(snapshot)))
                );
                const latestCandidates = queueResults.at(-1);
                assertTrace("latest-queue-candidates", ">0", latestCandidates.length, latestCandidates.length > 0);

                for (let cycle = 1; cycle <= clearCycles; cycle++) {
                    capture.clear();
                    snapshots = [];
                    trace("EXIT", "strokeCapture.clear", {cycle, hasStrokes: capture.hasStrokes()});
                    const character = cycle % 2 === 0 ? sun : person;
                    character.forEach((stroke, index) => drawStroke(canvas, stroke, cycle * 100 + index));

                    for (let index = 0; index < snapshots.length; index++) {
                        const candidates = await traced(
                                "recognition.afterClear",
                                {cycle, snapshotIndex: index, strokeCount: snapshots[index].length},
                                () => recognition.recognize(snapshots[index])
                        );
                        assertTrace(
                                `post-clear-cycle-${cycle}-snapshot-${index + 1}`,
                                ">0",
                                candidates.length,
                                candidates.length > 0
                        );
                    }
                }

                console.log("[ASSERT] expected=latest-and-post-clear-candidates actual=available PASS");
            }

            try {
                await main();
            } catch (error) {
                trace("THROW", "trace.main", {error: {name: error.name, message: error.message, stack: error.stack}});
                process.exitCode = 1;
            }
            """;
}
