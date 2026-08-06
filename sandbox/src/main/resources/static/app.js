/*
 * The whole UI: one screen over one REST resource, written directly against the DOM.
 *
 * Deliberately not a config-driven renderer. At one or two screens the indirection costs more than
 * it saves, and widening a generic renderer to fit an unlike screen (a dashboard, a wizard, a
 * detail view) is the most expensive mistake you can make mid-interview. If you need a second
 * screen, copy the parts you need; if you need a different kind of screen, write it separately.
 *
 * Retargeting this to a new resource: change API, STATUSES/STATUS_LABELS, the <thead> in
 * index.html, and the cells in taskRow(). Everything else is domain-free.
 */

const API = "/api/tasks";
const STATUSES = ["TODO", "IN_PROGRESS", "DONE"];
const STATUS_LABELS = { TODO: "To do", IN_PROGRESS: "In progress", DONE: "Done" };

const el = (id) => document.getElementById(id);

let tasks = [];

// ---- helpers --------------------------------------------------------------

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

const timestamp = (iso) =>
  iso ? new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }) : "—";

function toast(text, type = "success") {
  const node = document.createElement("div");
  node.className = `toast toast-${type}`;
  node.textContent = text;
  el("toasts").appendChild(node);
  setTimeout(() => node.remove(), 4000);
}

/** Every call goes through here, so the backend's ApiError shape is parsed in exactly one place. */
async function request(path, { method = "GET", body } = {}) {
  const response = await fetch(path, {
    method,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const error = await response.json();
      message = error.details?.length ? `${error.message}: ${error.details.join("; ")}` : error.message || message;
    } catch {
      /* non-JSON error body — keep the status-code message */
    }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

// ---- rendering ------------------------------------------------------------

function render() {
  const body = el("task-rows");
  if (!tasks.length) {
    body.innerHTML = `<tr><td colspan="6" class="empty">No tasks yet</td></tr>`;
    return;
  }
  body.replaceChildren(...tasks.map(taskRow));
}

/** Placeholder row while a fetch is in flight, so the table never looks empty-but-loaded. */
function renderLoading() {
  el("task-rows").innerHTML = `<tr><td colspan="6" class="empty">Loading…</td></tr>`;
}

function taskRow(task) {
  const tr = document.createElement("tr");
  tr.innerHTML = `
    <td>${task.id}</td>
    <td>${escapeHtml(task.title)}</td>
    <td class="muted">${task.description ? escapeHtml(task.description) : "—"}</td>
    <td>
      <select class="status" data-status="${task.status}" aria-label="Status">
        ${STATUSES.map((s) =>
          `<option value="${s}" ${s === task.status ? "selected" : ""}>${STATUS_LABELS[s]}</option>`).join("")}
      </select>
    </td>
    <td class="muted">${timestamp(task.updatedAt)}</td>
    <td class="row-actions">
      <button type="button" class="btn btn-secondary btn-small" data-act="view">View</button>
      <button type="button" class="btn btn-secondary btn-small" data-act="delete">Delete</button>
    </td>`;

  const status = tr.querySelector("select");
  status.addEventListener("change", () =>
    act(status, () => request(`${API}/${task.id}`, { method: "PATCH", body: { status: status.value } }),
      "Status updated"));

  tr.querySelector('[data-act="view"]').addEventListener("click", () => openDetail(task.id));

  const remove = tr.querySelector('[data-act="delete"]');
  remove.addEventListener("click", () =>
    act(remove, () => request(`${API}/${task.id}`, { method: "DELETE" }), "Task deleted"));

  return tr;
}

// ---- detail view ----------------------------------------------------------

/** Fetches one record by id. Its own request and its own render — not a filtered list row. */
async function openDetail(id) {
  const panel = el("detail-panel");
  const error = el("detail-error");
  const fields = el("detail-fields");

  panel.hidden = false;
  error.hidden = true;
  fields.innerHTML = `<div class="empty">Loading…</div>`;
  panel.scrollIntoView({ behavior: "smooth", block: "nearest" });

  try {
    const task = await request(`${API}/${id}`);
    el("detail-title").textContent = task.title;
    fields.replaceChildren(...[
      ["ID", task.id],
      ["Title", task.title],
      ["Description", task.description || "—"],
      ["Status", STATUS_LABELS[task.status] ?? task.status],
      ["Created", timestamp(task.createdAt)],
      ["Updated", timestamp(task.updatedAt)],
    ].flatMap(([label, value]) => {
      const dt = document.createElement("dt");
      dt.textContent = label;
      const dd = document.createElement("dd");
      dd.textContent = value;
      return [dt, dd];
    }));
  } catch (err) {
    fields.replaceChildren();
    error.textContent = err.message;
    error.hidden = false;
  }
}

el("close-detail").addEventListener("click", () => {
  el("detail-panel").hidden = true;
});

/**
 * Runs a row action with the control disabled for the duration, so an impatient double-click is
 * one request rather than two. Reloads either way — on failure that resyncs the row back to what
 * the server actually holds, instead of leaving the select showing a change that never landed.
 */
async function act(control, run, message) {
  control.disabled = true;
  try {
    await run();
    toast(message);
  } catch (err) {
    toast(err.message, "error");
  } finally {
    await load();
  }
}

// ---- actions --------------------------------------------------------------

async function load() {
  const status = el("status-filter").value;
  const error = el("list-error");
  error.hidden = true;
  renderLoading();

  try {
    tasks = await request(status ? `${API}?status=${status}` : API);
  } catch (err) {
    tasks = [];
    error.textContent = err.message;
    error.hidden = false;
  }
  render();
}

async function create(event) {
  event.preventDefault();
  const form = event.target;
  const button = form.querySelector('button[type="submit"]');
  const error = el("form-error");
  error.hidden = true;

  // A double-click is two POSTs and two tasks. Hold the button down until the call resolves, and
  // re-enable in `finally` so a validation failure doesn't strand the form.
  button.disabled = true;
  const data = new FormData(form);

  try {
    await request(API, {
      method: "POST",
      body: { title: data.get("title"), description: data.get("description") || null },
    });
    form.reset();
    toast("Task created");
    await load();
  } catch (err) {
    error.textContent = err.message;
    error.hidden = false;
  } finally {
    button.disabled = false;
  }
}

// ---- wiring ---------------------------------------------------------------

el("status-filter").insertAdjacentHTML("beforeend",
  STATUSES.map((s) => `<option value="${s}">${STATUS_LABELS[s]}</option>`).join(""));

el("create-form").addEventListener("submit", create);
el("status-filter").addEventListener("change", load);
el("refresh").addEventListener("click", load);

load();
