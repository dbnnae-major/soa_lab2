const $ = id => document.getElementById(id);
const fields = ["id", "name", "coordinates.x", "coordinates.y", "creationDate", "age", "wingspan", "speaking", "color", "killer.name", "killer.birthday", "killer.nationality"];
const labels = ["ID", "Имя", "X", "Y", "Дата создания", "Возраст", "Размах", "Говорит", "Цвет", "Убийца", "Дата рождения", "Страна"];

$("dragon-url").value = localStorage.getItem("dragonApiUrl") || "https://se.ifmo.ru:31443/dragon-service/api";
$("killer-url").value = localStorage.getItem("killerApiUrl") || "https://se.ifmo.ru:32443/killer-service/api";

function message(id, text, error = false) {
    const target = $(id);
    target.textContent = text;
    target.classList.toggle("error", error);
}

function baseUrl(service) {
    let url = $(service === "dragon" ? "dragon-url" : "killer-url").value.trim();
    if (!url.startsWith("https://")) throw new Error("Укажите HTTPS-адрес " + service + " API в разделе «Адреса сервисов».");
    while (url.endsWith("/")) url = url.slice(0, -1);
    return url;
}

function xmlDocument(xml) {
    const document = new DOMParser().parseFromString(xml, "application/xml");
    if (document.getElementsByTagName("parsererror").length || document.getElementsByTagNameNS("*", "parsererror").length) {
        throw new Error("Сервис вернул некорректный XML.");
    }
    return document;
}

function child(element, name) {
    if (!element) return null;
    return Array.from(element.children).find(item => item.localName === name) || null;
}

function value(element, name) {
    const node = child(element, name);
    if (!node) return "";
    if (["true", "1"].includes(node.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "nil"))) return "";
    return node.textContent;
}

function readDragon(element) {
    const coordinates = child(element, "coordinates");
    const killer = child(element, "killer");
    const nil = killer && ["true", "1"].includes(killer.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "nil"));
    return {
        id: value(element, "id"),
        name: value(element, "name"),
        x: value(coordinates, "x"),
        y: value(coordinates, "y"),
        creationDate: value(element, "creationDate"),
        age: value(element, "age"),
        wingspan: value(element, "wingspan"),
        speaking: value(element, "speaking"),
        color: value(element, "color"),
        killer: killer && !nil ? {
            name: value(killer, "name"),
            birthday: value(killer, "birthday"),
            nationality: value(killer, "nationality")
        } : null
    };
}

function readDragons(xml) {
    const root = xmlDocument(xml).documentElement;
    if (root.localName !== "dragons") throw new Error("Ожидался список <dragons>.");
    return Array.from(root.children).filter(element => element.localName === "dragon").map(readDragon);
}

function readOneDragon(xml) {
    const root = xmlDocument(xml).documentElement;
    if (root.localName !== "dragon") throw new Error("Ожидался объект <dragon>.");
    return readDragon(root);
}

function tableOfDragons(dragons) {
    const table = document.createElement("table");
    const header = table.createTHead().insertRow();
    for (const title of labels) {
        const th = document.createElement("th");
        th.textContent = title;
        header.appendChild(th);
    }
    const body = table.createTBody();
    for (const dragon of dragons) {
        const row = body.insertRow();
        const cells = [dragon.id, dragon.name, dragon.x, dragon.y, dragon.creationDate, dragon.age,
            dragon.wingspan, dragon.speaking === "true" ? "Да" : dragon.speaking === "false" ? "Нет" : "",
            dragon.color, dragon.killer?.name, dragon.killer?.birthday, dragon.killer?.nationality];
        for (const cellValue of cells) row.insertCell().textContent = cellValue || "—";
    }
    return table;
}

function showRows(containerId, dragons) {
    const container = $(containerId);
    container.replaceChildren();
    if (dragons.length) container.appendChild(tableOfDragons(dragons));
    else container.textContent = "Драконы не найдены.";
}

function showObject(containerId, object) {
    const container = $(containerId);
    const table = document.createElement("table");
    for (const [name, val] of Object.entries(object)) {
        const row = table.insertRow();
        const label = row.insertCell();
        label.textContent = name;
        row.insertCell().textContent = val === "" || val == null ? "—" : String(val);
    }
    container.replaceChildren(table);
}

async function api(service, path, method = "GET", body = null) {
    const url = baseUrl(service) + path;
    let response;
    try {
        response = await fetch(url, {
            method,
            signal: AbortSignal.timeout(20000),
            headers: body === null ? {Accept: "application/xml"} : {Accept: "application/xml", "Content-Type": "application/xml"},
            body
        });
    } catch (error) {
        message(service + "-status", "Сервис недоступен: запрос не завершён. Проверьте сеть и сертификат.", true);
        throw new Error("Не удалось подключиться к сервису. Проверьте HTTPS-адрес, сертификат и разрешение CORS.");
    }

    message(service + "-status", "Сервис отвечает: HTTP " + response.status, response.status >= 500);

    const text = response.status === 204 ? "" : await response.text();
    if (!response.ok) {
        let detail = response.statusText || "Ошибка сервиса";
        if (text) {
            try {
                detail = value(xmlDocument(text).documentElement, "detail") || detail;
            } catch (_) {
                // Если сервер вернул не Problem Details, показываем HTTP-код.
            }
        }
        throw new Error("HTTP " + response.status + ": " + detail);
    }
    return text;
}

async function action(messageId, operation) {
    message(messageId, "Выполняется запрос...");
    try {
        await operation();
    } catch (error) {
        message(messageId, error.message, true);
    }
}

function queryString(parameters) {
    const query = new URLSearchParams();
    for (const [key, val] of Object.entries(parameters)) {
        if (val !== "") query.append(key, val);
    }
    return query.toString();
}

function addSort() {
    const row = document.createElement("div");
    row.className = "sort-row";
    const field = document.createElement("select");
    field.setAttribute("aria-label", "Поле сортировки");
    for (const name of fields) {
        const option = document.createElement("option");
        option.value = name;
        option.textContent = name;
        field.appendChild(option);
    }
    const direction = document.createElement("select");
    direction.setAttribute("aria-label", "Направление сортировки");
    for (const [val, text] of [["asc", "По возрастанию"], ["desc", "По убыванию"]]) {
        const option = document.createElement("option");
        option.value = val;
        option.textContent = text;
        direction.appendChild(option);
    }
    const remove = document.createElement("button");
    remove.type = "button";
    remove.textContent = "Убрать";
    remove.addEventListener("click", () => row.remove());
    row.append(field, direction, remove);
    $("sort-rows").appendChild(row);
}

async function loadDragons() {
    $("dragon-table").querySelector("tbody").replaceChildren();
    const parameters = {};
    const data = new FormData($("list-form"));
    for (const field of fields) parameters[field] = String(data.get(field) || "");
    parameters.page = $("page").value;
    parameters.pageSize = $("page-size").value;
    const query = new URLSearchParams(queryString(parameters));
    for (const row of $("sort-rows").children) {
        const selects = row.querySelectorAll("select");
        query.append("sort", selects[0].value + "," + selects[1].value);
    }
    const xml = await api("dragon", "/dragons?" + query.toString());
    const dragons = readDragons(xml);
    $("dragon-table").querySelector("tbody").replaceChildren(...Array.from(tableOfDragons(dragons).tBodies[0].rows));
    message("list-message", "Страница " + $("page").value + ". Найдено на странице: " + dragons.length + ".");
}

function appendXml(document, parent, tag, value) {
    if (value === "") return;
    const element = document.createElementNS(null, tag);
    element.textContent = value;
    parent.appendChild(element);
}

function dragonXml(form) {
    const data = new FormData(form);
    const xmlDoc = document.implementation.createDocument(null, "dragon");
    const root = xmlDoc.documentElement;
    appendXml(xmlDoc, root, "name", String(data.get("name") || ""));
    const coordinates = xmlDoc.createElement("coordinates");
    appendXml(xmlDoc, coordinates, "x", String(data.get("x") || ""));
    appendXml(xmlDoc, coordinates, "y", String(data.get("y") || ""));
    root.appendChild(coordinates);
    appendXml(xmlDoc, root, "age", String(data.get("age") || ""));
    appendXml(xmlDoc, root, "wingspan", String(data.get("wingspan") || ""));
    appendXml(xmlDoc, root, "speaking", String(data.get("speaking") || ""));
    appendXml(xmlDoc, root, "color", String(data.get("color") || ""));
    if ($("has-killer").checked) {
        const killer = xmlDoc.createElement("killer");
        appendXml(xmlDoc, killer, "name", String(data.get("killerName") || ""));
        appendXml(xmlDoc, killer, "birthday", String(data.get("killerBirthday") || ""));
        appendXml(xmlDoc, killer, "nationality", String(data.get("killerNationality") || ""));
        root.appendChild(killer);
    }
    return new XMLSerializer().serializeToString(xmlDoc);
}

$("settings-form").addEventListener("submit", event => {
    event.preventDefault();
    action("settings-message", async () => {
        const dragon = baseUrl("dragon");
        const killer = baseUrl("killer");
        localStorage.setItem("dragonApiUrl", dragon);
        localStorage.setItem("killerApiUrl", killer);
        message("settings-message", "Адреса сохранены.");
    });
});

$("add-sort").addEventListener("click", addSort);
$("list-form").addEventListener("submit", event => {
    event.preventDefault();
    action("list-message", loadDragons);
});
$("previous-page").addEventListener("click", () => {
    const page = Number($("page").value);
    if (page > 0) $("page").value = String(page - 1);
    action("list-message", loadDragons);
});
$("next-page").addEventListener("click", () => {
    if (!$("list-form").reportValidity()) return;
    $("page").value = String(Number($("page").value) + 1);
    action("list-message", loadDragons);
});

$("get-form").addEventListener("submit", event => {
    event.preventDefault();
    action("get-message", async () => {
        $("get-result").replaceChildren();
        const id = new FormData(event.currentTarget).get("id");
        const dragon = readOneDragon(await api("dragon", "/dragons/" + encodeURIComponent(id)));
        showObject("get-result", {
            ID: dragon.id, Имя: dragon.name, X: dragon.x, Y: dragon.y,
            "Дата создания": dragon.creationDate, Возраст: dragon.age, "Размах крыльев": dragon.wingspan,
            Говорит: dragon.speaking === "true" ? "Да" : "Нет", Цвет: dragon.color,
            Убийца: dragon.killer?.name, "Дата рождения убийцы": dragon.killer?.birthday,
            "Страна убийцы": dragon.killer?.nationality
        });
        message("get-message", "Дракон найден.");
    });
});

$("has-killer").addEventListener("change", event => {
    $("killer-fields").hidden = !event.target.checked;
    $("dragon-form").elements.namedItem("killerName").required = event.target.checked;
});

$("dragon-form").addEventListener("submit", event => {
    event.preventDefault();
    const method = event.submitter?.dataset.method || "POST";
    action("dragon-message", async () => {
        const id = new FormData(event.currentTarget).get("id");
        if (method === "PUT" && !id) throw new Error("Для обновления укажите ID.");
        const path = method === "PUT" ? "/dragons/" + encodeURIComponent(id) : "/dragons";
        $("dragon-result").replaceChildren();
        const dragon = readOneDragon(await api("dragon", path, method, dragonXml(event.currentTarget)));
        showObject("dragon-result", {ID: dragon.id, Имя: dragon.name, X: dragon.x, Y: dragon.y,
            "Дата создания": dragon.creationDate, Возраст: dragon.age, Размах: dragon.wingspan,
            Говорит: dragon.speaking === "true" ? "Да" : "Нет", Цвет: dragon.color,
            Убийца: dragon.killer?.name, "Дата рождения убийцы": dragon.killer?.birthday,
            "Страна убийцы": dragon.killer?.nationality});
        message("dragon-message", method === "POST" ? "Дракон создан." : "Дракон обновлён.");
    });
});

$("delete-form").addEventListener("submit", event => {
    event.preventDefault();
    action("delete-message", async () => {
        const id = new FormData(event.currentTarget).get("id");
        await api("dragon", "/dragons/" + encodeURIComponent(id), "DELETE");
        message("delete-message", "Дракон удалён (204).");
    });
});

$("prefix-form").addEventListener("submit", event => {
    event.preventDefault();
    action("prefix-message", async () => {
        const prefix = new FormData(event.currentTarget).get("prefix");
        showRows("prefix-result", readDragons(await api("dragon", "/dragons/name/starts-with?" + queryString({prefix}))));
        message("prefix-message", "Поиск завершён.");
    });
});

$("less-form").addEventListener("submit", event => {
    event.preventDefault();
    action("less-message", async () => {
        const killerName = new FormData(event.currentTarget).get("killerName");
        const root = xmlDocument(await api("dragon", "/dragons/killer/less-than/count?" + queryString({killerName}))).documentElement;
        if (root.localName !== "count") throw new Error("Ожидался ответ <count>.");
        $("less-result").textContent = "Количество: " + root.textContent.trim();
        message("less-message", "Подсчёт завершён.");
    });
});

$("greater-form").addEventListener("submit", event => {
    event.preventDefault();
    action("greater-message", async () => {
        const killerName = new FormData(event.currentTarget).get("killerName");
        showRows("greater-result", readDragons(await api("dragon", "/dragons/killer/greater-than?" + queryString({killerName}))));
        message("greater-message", "Поиск завершён.");
    });
});

$("kill-form").addEventListener("submit", event => {
    event.preventDefault();
    action("kill-message", async () => {
        const id = new FormData(event.currentTarget).get("id");
        await api("killer", "/killer/dragon/" + encodeURIComponent(id) + "/kill", "POST");
        message("kill-message", "Операция выполнена (204).");
    });
});

$("team-form").addEventListener("submit", event => {
    event.preventDefault();
    action("team-message", async () => {
        const data = new FormData(event.currentTarget);
        const parts = ["id", "name", "size", "startCaveId"].map(name => encodeURIComponent(data.get(name)));
        const root = xmlDocument(await api("killer", "/killer/teams/create/" + parts.join("/"), "POST")).documentElement;
        if (root.localName !== "team") throw new Error("Ожидался ответ <team>.");
        showObject("team-result", {
            ID: value(root, "id"), Название: value(root, "name"),
            Участников: value(root, "size"), "Стартовая пещера": value(root, "startCaveId")
        });
        message("team-message", "Команда создана (201).");
    });
});
