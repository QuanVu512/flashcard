const templateCache = new Map();

async function loadTemplate(name) {
    if (!templateCache.has(name)) {
        const response = await fetch(`/views/${name}.html`, {headers: {Accept: "text/html"}});
        if (!response.ok) {
            throw new Error(`Không thể tải giao diện ${name}.`);
        }
        templateCache.set(name, await response.text());
    }

    const template = document.createElement("template");
    template.innerHTML = templateCache.get(name).trim();
    return template.content.cloneNode(true);
}

async function preloadTemplates(names) {
    await Promise.all(names.map(loadTemplate));
}

function cloneTemplate(name) {
    if (!templateCache.has(name)) {
        throw new Error(`Giao diện ${name} chưa được tải.`);
    }
    const template = document.createElement("template");
    template.innerHTML = templateCache.get(name).trim();
    return template.content.cloneNode(true);
}

export {preloadTemplates, loadTemplate, cloneTemplate};
