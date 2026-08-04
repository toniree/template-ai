// Minimal, dependency-free client for /api/widgets.
// Swap API_BASE / RESOURCE if you rename or add a resource; the render/CRUD
// helpers below are written generically enough to reuse for a new one.

const API_BASE = "/api/widgets";

const tableBody = document.getElementById("widget-table-body");
const listError = document.getElementById("list-error");
const formError = document.getElementById("form-error");
const createForm = document.getElementById("create-form");
const refreshBtn = document.getElementById("refresh-btn");

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function showError(el, message) {
  if (!message) {
    el.hidden = true;
    el.textContent = "";
    return;
  }
  el.hidden = false;
  el.textContent = message;
}

// The backend's GlobalExceptionHandler returns { message, details: [...] } on errors.
async function extractErrorMessage(response) {
  try {
    const body = await response.json();
    if (body.details && body.details.length) {
      return `${body.message}: ${body.details.join("; ")}`;
    }
    return body.message || `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(await extractErrorMessage(response));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

function renderRow(widget) {
  const tr = document.createElement("tr");
  tr.innerHTML = `
    <td>${escapeHtml(widget.id)}</td>
    <td>${escapeHtml(widget.name)}</td>
    <td>${escapeHtml(widget.quantity)}</td>
    <td><button class="danger" data-id="${escapeHtml(widget.id)}">Delete</button></td>
  `;
  tr.querySelector("button").addEventListener("click", () => deleteWidget(widget.id));
  return tr;
}

function renderTable(widgets) {
  tableBody.replaceChildren();
  if (!widgets.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="4" class="empty">No widgets yet</td>`;
    tableBody.appendChild(tr);
    return;
  }
  widgets.forEach((w) => tableBody.appendChild(renderRow(w)));
}

async function loadWidgets() {
  showError(listError, null);
  try {
    const widgets = await fetchJson(API_BASE);
    renderTable(widgets);
  } catch (err) {
    showError(listError, err.message);
    tableBody.replaceChildren();
  }
}

async function createWidget(payload) {
  return fetchJson(API_BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

async function deleteWidget(id) {
  try {
    await fetchJson(`${API_BASE}/${encodeURIComponent(id)}`, { method: "DELETE" });
    await loadWidgets();
  } catch (err) {
    showError(listError, err.message);
  }
}

createForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  showError(formError, null);

  const name = document.getElementById("name").value.trim();
  const quantity = Number(document.getElementById("quantity").value);

  try {
    await createWidget({ name, quantity });
    createForm.reset();
    document.getElementById("quantity").value = 0;
    await loadWidgets();
  } catch (err) {
    showError(formError, err.message);
  }
});

refreshBtn.addEventListener("click", loadWidgets);

loadWidgets();
