import {api} from "../../core/api.js";

function setupHandwritingCanvas(root) {
    const canvas = root.querySelector("[data-handwriting-canvas]");
    const status = root.querySelector("[data-handwriting-status]");
    const answerInput = root.querySelector('[name="answer"]');
    if (!canvas || canvas.dataset.ready === "true") return;

    canvas.dataset.ready = "true";
    const context = canvas.getContext("2d");
    context.lineWidth = 5;
    context.lineCap = "round";
    context.lineJoin = "round";
    context.strokeStyle = "#17213f";
    let drawing = false;
    let dirty = false;

    const point = event => {
        const rect = canvas.getBoundingClientRect();
        return {
            x: ((event.clientX - rect.left) / rect.width) * canvas.width,
            y: ((event.clientY - rect.top) / rect.height) * canvas.height
        };
    };

    canvas.addEventListener("pointerdown", event => {
        drawing = true;
        dirty = true;
        canvas.setPointerCapture(event.pointerId);
        const current = point(event);
        context.beginPath();
        context.moveTo(current.x, current.y);
    });
    canvas.addEventListener("pointermove", event => {
        if (!drawing) return;
        const current = point(event);
        context.lineTo(current.x, current.y);
        context.stroke();
    });
    canvas.addEventListener("pointerup", () => {
        drawing = false;
    });
    canvas.addEventListener("pointercancel", () => {
        drawing = false;
    });

    root.querySelector("[data-clear-handwriting]")?.addEventListener("click", () => {
        context.clearRect(0, 0, canvas.width, canvas.height);
        dirty = false;
        status.textContent = "Bảng vẽ đã được làm sạch.";
        status.className = "handwriting-status";
    });
    root.querySelector("[data-recognize-handwriting]")?.addEventListener("click", async () => {
        if (!dirty) {
            status.textContent = "Hãy viết một chữ lên bảng trước đã.";
            status.className = "handwriting-status error";
            return;
        }

        status.textContent = "Đang nhận dạng chữ viết...";
        status.className = "handwriting-status";
        try {
            const payload = await api("/api/handwriting/recognize", {
                method: "POST",
                body: JSON.stringify({
                    imageData: canvas.toDataURL("image/png"),
                    language: root.querySelector("[data-handwriting-language]")?.value || "en"
                })
            });
            if (!payload.enabled || !payload.text) {
                status.textContent = payload.message || "Chưa nhận ra chữ nào.";
                status.className = "handwriting-status error";
                return;
            }
            answerInput.value = payload.text;
            status.textContent = payload.message || "Đã nhận dạng xong.";
            status.className = "handwriting-status success";
        } catch (error) {
            status.textContent = error.message;
            status.className = "handwriting-status error";
        }
    });
}

export {setupHandwritingCanvas};
