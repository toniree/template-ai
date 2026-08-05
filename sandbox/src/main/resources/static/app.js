// Minimal, dependency-free client for /api/widgets.
// Swap API_BASE / RESOURCE if you rename or add a resource; the render/CRUD
// helpers below are written generically enough to reuse for a new one.

const API_BASE = "/api/widgets";

const tableBody = document.getElementById("widget-table-body");
const listError = document.getElementById("list-error");
const formError = document.getElementById("form-error");
const createForm = document.getElementById("create-form");
const refreshBtn = document.getElementById("refresh-btn");
const searchInput = document.getElementById("search-input");
const pageSizeSelect = document.getElementById("page-size");
const prevPageBtn = document.getElementById("prev-page");
const nextPageBtn = document.getElementById("next-page");
const pageInfo = document.getElementById("page-info");
const toastContainer = document.getElementById("toast-container");

// ---- client-side state ----------------------------------------------------
// The API has no query params for search/sort/paging; the full list is small
// enough for an interview demo to fetch once and slice client-side.
let widgets = [];
let sortKey = "id";
let sortDir = "asc";
let searchTerm = "";
let pageSize = Number(pageSizeSelect.value);
let currentPage = 1;
let editingId = null;

// ---- helpers ----------------------------------------------------------

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

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
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

function isTempId(id) {
  return typeof id === "string" && id.startsWith("tmp-");
}

// ---- derived view: filter -> sort -> paginate ------------------------

function computeView() {
  let filtered = widgets;
  if (searchTerm) {
    const needle = searchTerm.toLowerCase();
    filtered = filtered.filter((w) => w.name.toLowerCase().includes(needle));
  }

  const sorted = [...filtered].sort((a, b) => {
    const av = a[sortKey];
    const bv = b[sortKey];
    let cmp;
    if (sortKey === "quantity") {
      cmp = Number(av) - Number(bv);
    } else {
      cmp = String(av).localeCompare(String(bv), undefined, { numeric: true });
    }
    return sortDir === "asc" ? cmp : -cmp;
  });

  const totalPages = Math.max(1, Math.ceil(sorted.length / pageSize));
  currentPage = Math.min(currentPage, totalPages);
  const start = (currentPage - 1) * pageSize;
  const pageItems = sorted.slice(start, start + pageSize);

  return { pageItems, totalItems: sorted.length, totalPages };
}

// ---- rendering ----------------------------------------------------------

function renderSortIndicators() {
  document.querySelectorAll("th.sortable").forEach((th) => {
    const indicator = th.querySelector(".sort-indicator");
    if (th.dataset.sort === sortKey) {
      indicator.textContent = sortDir === "asc" ? "▲" : "▼";
    } else {
      indicator.textContent = "";
    }
  });
}

function renderRow(widget) {
  const tr = document.createElement("tr");
  const pending = isTempId(widget.id);
  if (pending) tr.classList.add("pending");

  if (editingId === widget.id) {
    tr.innerHTML = `
      <td>${escapeHtml(widget.id)}</td>
      <td><input type="text" class="edit-name" value="${escapeHtml(widget.name)}" maxlength="255"></td>
      <td><input type="number" class="edit-quantity" value="${escapeHtml(widget.quantity)}" min="0" step="1"></td>
      <td class="row-actions">
        <button class="btn btn-primary btn-small" data-action="save">Save</button>
        <button class="btn btn-secondary btn-small" data-action="cancel">Cancel</button>
      </td>
    `;
    tr.querySelector('[data-action="save"]').addEventListener("click", () => saveEdit(widget.id, tr));
    tr.querySelector('[data-action="cancel"]').addEventListener("click", () => {
      editingId = null;
      render();
    });
    return tr;
  }

  tr.innerHTML = `
    <td>${escapeHtml(widget.id)}</td>
    <td>${escapeHtml(widget.name)}</td>
    <td>${escapeHtml(widget.quantity)}</td>
    <td class="row-actions">
      <button class="btn btn-secondary btn-small" data-action="edit" ${pending ? "disabled" : ""}>Edit</button>
      <button class="btn btn-danger" data-action="delete" ${pending ? "disabled" : ""}>Delete</button>
    </td>
  `;
  tr.querySelector('[data-action="edit"]').addEventListener("click", () => {
    editingId = widget.id;
    render();
  });
  tr.querySelector('[data-action="delete"]').addEventListener("click", () => deleteWidget(widget.id));
  return tr;
}

function render() {
  const { pageItems, totalItems, totalPages } = computeView();

  tableBody.replaceChildren();
  if (!pageItems.length) {
    const tr = document.createElement("tr");
    const message = widgets.length === 0 ? "No widgets yet" : "No widgets match your search";
    tr.innerHTML = `<td colspan="4" class="empty">${escapeHtml(message)}</td>`;
    tableBody.appendChild(tr);
  } else {
    pageItems.forEach((w) => tableBody.appendChild(renderRow(w)));
  }

  pageInfo.textContent = `Page ${currentPage} of ${totalPages} (${totalItems} widget${totalItems === 1 ? "" : "s"})`;
  prevPageBtn.disabled = currentPage <= 1;
  nextPageBtn.disabled = currentPage >= totalPages;
  renderSortIndicators();
}

// ---- data operations (optimistic where it's safe to roll back) --------

async function loadWidgets() {
  showError(listError, null);
  try {
    widgets = await fetchJson(API_BASE);
    render();
  } catch (err) {
    showError(listError, err.message);
    widgets = [];
    render();
  }
}

async function createWidget(payload) {
  const tempId = `tmp-${Date.now()}`;
  const optimisticWidget = { id: tempId, ...payload };
  widgets = [...widgets, optimisticWidget];
  render();

  try {
    const created = await fetchJson(API_BASE, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    widgets = widgets.map((w) => (w.id === tempId ? created : w));
    render();
    showToast(`Created "${created.name}"`, "success");
  } catch (err) {
    widgets = widgets.filter((w) => w.id !== tempId);
    render();
    showError(formError, err.message);
  }
}

async function deleteWidget(id) {
  const index = widgets.findIndex((w) => w.id === id);
  if (index === -1) return;
  const [removed] = widgets.splice(index, 1);
  widgets = [...widgets];
  render();

  try {
    await fetchJson(`${API_BASE}/${encodeURIComponent(id)}`, { method: "DELETE" });
    showToast(`Deleted "${removed.name}"`, "success");
  } catch (err) {
    widgets = [...widgets.slice(0, index), removed, ...widgets.slice(index)];
    render();
    showError(listError, err.message);
    showToast(err.message, "error");
  }
}

async function saveEdit(id, rowEl) {
  const name = rowEl.querySelector(".edit-name").value.trim();
  const quantity = Number(rowEl.querySelector(".edit-quantity").value);

  if (!name) {
    showToast("Name cannot be empty", "error");
    return;
  }

  const index = widgets.findIndex((w) => w.id === id);
  if (index === -1) return;
  const previous = widgets[index];
  widgets = widgets.map((w) => (w.id === id ? { ...w, name, quantity } : w));
  editingId = null;
  render();

  try {
    const updated = await fetchJson(`${API_BASE}/${encodeURIComponent(id)}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, quantity }),
    });
    widgets = widgets.map((w) => (w.id === id ? updated : w));
    render();
    showToast(`Updated "${updated.name}"`, "success");
  } catch (err) {
    widgets = widgets.map((w) => (w.id === id ? previous : w));
    render();
    showToast(err.message, "error");
  }
}

// ---- event wiring ----------------------------------------------------

createForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  showError(formError, null);

  const name = document.getElementById("name").value.trim();
  const quantity = Number(document.getElementById("quantity").value);

  await createWidget({ name, quantity });
  createForm.reset();
  document.getElementById("quantity").value = 0;
});

refreshBtn.addEventListener("click", loadWidgets);

let searchDebounce;
searchInput.addEventListener("input", (event) => {
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(() => {
    searchTerm = event.target.value.trim();
    currentPage = 1;
    render();
  }, 150);
});

pageSizeSelect.addEventListener("change", (event) => {
  pageSize = Number(event.target.value);
  currentPage = 1;
  render();
});

prevPageBtn.addEventListener("click", () => {
  currentPage = Math.max(1, currentPage - 1);
  render();
});

nextPageBtn.addEventListener("click", () => {
  currentPage += 1;
  render();
});

document.querySelectorAll("th.sortable").forEach((th) => {
  th.addEventListener("click", () => {
    const key = th.dataset.sort;
    if (sortKey === key) {
      sortDir = sortDir === "asc" ? "desc" : "asc";
    } else {
      sortKey = key;
      sortDir = "asc";
    }
    render();
  });
});

loadWidgets();
