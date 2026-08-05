/*
 * Config-driven admin UI. Everything below the RESOURCES block is generic: to add a screen for a
 * new endpoint you add one entry here and change nothing else.
 *
 *   label      tab text
 *   path       API base path
 *   paged      true if the endpoint returns { content, page, size, totalElements, totalPages }
 *   columns    [{ key, label, render? }]
 *   form       { title, submit, fields: [...] } — omit for a read-only screen
 *   filters    [{ name, label, from }] — sent as query params
 *   onCreated  (row) => ({ text, type }) toast, for when "created" isn't the same as "succeeded"
 *   idempotent true if POST moves money — sends a per-submission Idempotency-Key header
 *
 * This covers list/create/table screens. A dashboard, wizard, map or detail workflow should be
 * the smallest bespoke HTML/JS instead — do not widen the renderer below to absorb one.
 */

const RESOURCES = {
  cards: {
    label: "Cards",
    path: "/api/cards",
    title: "Cards",
    subtitle: "Issue cards, set spend limits, freeze anything that looks wrong.",
    columns: [
      { key: "id", label: "ID" },
      { key: "cardholderName", label: "Cardholder" },
      { key: "last4", label: "Card", render: (v) => `<span class="mono">•••• ${v}</span>` },
      { key: "status", label: "Status", render: (v) => pill(v) },
      { key: "spendLimitMinor", label: "Limit", render: (v, r) => money(v, r.currency), align: "right" },
      { key: "spentMinor", label: "Spent", render: (v, r) => money(v, r.currency), align: "right" },
      {
        key: "availableMinor",
        label: "Available",
        align: "right",
        render: (v, r) => `<strong class="${v <= 0 ? "negative" : ""}">${money(v, r.currency)}</strong>`,
      },
    ],
    summary: (rows) => {
      // Totals are only meaningful within one currency. This starter is USD-only; if mixed data
      // ever shows up (via curl or Swagger), show nothing rather than a confidently wrong number.
      const currency = soleCurrency(rows);
      const total = (key) => (currency ? money(sum(rows, key), currency) : "—");
      return [
        { label: "Active cards", value: rows.filter((r) => r.status === "ACTIVE").length },
        { label: "Total limit", value: total("spendLimitMinor") },
        { label: "Total spent", value: total("spentMinor") },
      ];
    },
    form: {
      title: "Issue a card",
      submit: "Issue card",
      fields: [
        { name: "cardholderName", label: "Cardholder", type: "text", required: true, placeholder: "Ada Lovelace" },
        { name: "last4", label: "Last 4", type: "text", required: true, placeholder: "4242", maxlength: 4 },
        { name: "spendLimitMinor", label: "Spend limit", type: "money", required: true, value: "1000" },
        // USD-only starter: totals across currencies are meaningless without an FX rate, and
        // building that is not worth it until a problem actually asks for it.
        { name: "currency", label: "Currency", type: "select", options: ["USD"] },
      ],
    },
    rowActions: [
      {
        label: (r) => (r.status === "FROZEN" ? "Unfreeze" : "Freeze"),
        hidden: (r) => r.status === "CANCELLED",
        // Status only — PATCH leaves omitted fields alone, so there's no need to read the current
        // limit and send it back (which would race with anyone else editing it).
        run: (r) =>
          request(`/api/cards/${r.id}`, {
            method: "PATCH",
            body: { status: r.status === "FROZEN" ? "ACTIVE" : "FROZEN" },
          }),
      },
    ],
  },

  transactions: {
    label: "Transactions",
    path: "/api/transactions",
    title: "Transactions",
    subtitle: "Every authorization attempt, approved or declined.",
    paged: true,
    // Money-moving POST: send an Idempotency-Key so a retried submission can't double-charge.
    idempotent: true,
    columns: [
      { key: "id", label: "ID" },
      { key: "cardId", label: "Card" },
      { key: "merchant", label: "Merchant" },
      { key: "amountMinor", label: "Amount", render: (v, r) => money(v, r.currency), align: "right" },
      { key: "status", label: "Status", render: (v) => pill(v) },
      { key: "declineReason", label: "Reason", render: (v) => (v ? `<span class="muted">${v}</span>` : "—") },
      { key: "createdAt", label: "When", render: (v) => timestamp(v) },
    ],
    filters: [{ name: "cardId", label: "Card", from: "cards", optionLabel: (c) => `#${c.id} ${c.cardholderName}` }],
    form: {
      title: "Authorize a spend",
      submit: "Authorize",
      fields: [
        { name: "cardId", label: "Card", type: "select", from: "cards", optionLabel: (c) => `#${c.id} ${c.cardholderName} •••• ${c.last4}`, cast: Number },
        { name: "merchant", label: "Merchant", type: "text", required: true, placeholder: "AWS" },
        { name: "amountMinor", label: "Amount", type: "money", required: true, value: "25" },
        // USD-only starter: totals across currencies are meaningless without an FX rate, and
        // building that is not worth it until a problem actually asks for it.
        { name: "currency", label: "Currency", type: "select", options: ["USD"] },
      ],
    },
    // A declined charge is a successful API call with an unhappy answer. Say so.
    onCreated: (row) =>
      row.status === "DECLINED"
        ? { text: `Declined: ${row.declineReason}`, type: "error" }
        : { text: `Approved ${money(row.amountMinor, row.currency)} at ${row.merchant}`, type: "success" },
  },
};

// ---- formatting -----------------------------------------------------------

const money = (minor, currency = "USD") =>
  new Intl.NumberFormat(undefined, { style: "currency", currency }).format((minor ?? 0) / 100);

const timestamp = (iso) => (iso ? new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }) : "—");

const pill = (value) => `<span class="pill pill-${String(value).toLowerCase()}">${escapeHtml(value)}</span>`;

const sum = (rows, key) => rows.reduce((total, row) => total + (row[key] ?? 0), 0);

/** The currency shared by every row, or null if they disagree — never guess from row[0]. */
const soleCurrency = (rows) => {
  const codes = new Set(rows.map((r) => r.currency));
  return codes.size === 1 ? [...codes][0] : null;
};

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

// ---- API ------------------------------------------------------------------

/** Every failure funnels through here so the backend's ApiError shape is parsed in one place. */
async function request(path, { method = "GET", body, headers } = {}) {
  const response = await fetch(path, {
    method,
    headers: { ...(body ? { "Content-Type": "application/json" } : {}), ...headers },
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

// ---- state ----------------------------------------------------------------

const el = (id) => document.getElementById(id);
let active = location.hash.slice(1) in RESOURCES ? location.hash.slice(1) : "cards";
let rows = [];
let pageMeta = { page: 0, totalPages: 1, totalElements: 0 };
let filters = {};

// ---- rendering ------------------------------------------------------------

function renderTabs() {
  el("tabs").innerHTML = Object.entries(RESOURCES)
    .map(([key, r]) => `<button class="tab ${key === active ? "active" : ""}" data-key="${key}">${r.label}</button>`)
    .join("");
  el("tabs").querySelectorAll(".tab").forEach((tab) =>
    tab.addEventListener("click", () => select(tab.dataset.key)));
}

function renderSummary(config) {
  const container = el("summary");
  if (!config.summary || !rows.length) {
    container.innerHTML = "";
    return;
  }
  container.innerHTML = config.summary(rows)
    .map((s) => `<div class="stat"><span class="stat-label">${s.label}</span><span class="stat-value">${s.value}</span></div>`)
    .join("");
}

async function renderForm(config) {
  const panel = el("form-panel");
  if (!config.form) {
    panel.hidden = true;
    return;
  }
  panel.hidden = false;

  const fields = await Promise.all(config.form.fields.map(async (field) => {
    const input = await renderField(field);
    return `<label>${field.label}${input}</label>`;
  }));

  panel.innerHTML = `
    <h2>${config.form.title}</h2>
    <form id="create-form" class="form-row">
      ${fields.join("")}
      <button type="submit" class="btn btn-primary">${config.form.submit}</button>
    </form>
    <p id="form-error" class="error" role="alert" hidden></p>`;

  el("create-form").addEventListener("submit", (event) => submitForm(event, config));
}

async function renderField(field) {
  const required = field.required ? "required" : "";
  if (field.type === "select") {
    const options = field.from
      ? (await request(RESOURCES[field.from].path)).map((row) => ({ value: row.id, label: field.optionLabel(row) }))
      : field.options.map((value) => ({ value, label: value }));
    return `<select name="${field.name}">${options
      .map((o) => `<option value="${escapeHtml(o.value)}">${escapeHtml(o.label)}</option>`).join("")}</select>`;
  }
  if (field.type === "money") {
    // Typed in dollars, sent in cents. The API only ever speaks minor units.
    return `<input type="number" name="${field.name}" step="0.01" min="0" value="${field.value ?? ""}" ${required}>`;
  }
  return `<input type="${field.type}" name="${field.name}" placeholder="${field.placeholder ?? ""}"
            ${field.maxlength ? `maxlength="${field.maxlength}"` : ""} value="${field.value ?? ""}" ${required}>`;
}

function renderTable(config) {
  el("table-head").innerHTML = `<tr>${config.columns
    .map((c) => `<th class="${c.align === "right" ? "right" : ""}">${c.label}</th>`).join("")
    }${config.rowActions ? "<th></th>" : ""}</tr>`;

  const body = el("table-body");
  const span = config.columns.length + (config.rowActions ? 1 : 0);
  if (!rows.length) {
    body.innerHTML = `<tr><td colspan="${span}" class="empty">Nothing here yet</td></tr>`;
    return;
  }

  body.replaceChildren(...rows.map((row) => {
    const tr = document.createElement("tr");
    const cells = config.columns.map((c) => {
      const raw = row[c.key];
      const content = c.render ? c.render(raw, row) : escapeHtml(raw ?? "—");
      return `<td class="${c.align === "right" ? "right" : ""}">${content}</td>`;
    });

    if (config.rowActions) {
      // Index against the original array, not the filtered one, so hiding an action
      // doesn't shift what the remaining buttons point at.
      const buttons = config.rowActions
        .map((action, index) => ({ action, index }))
        .filter(({ action }) => !action.hidden?.(row))
        .map(({ action, index }) =>
          `<button class="btn btn-secondary btn-small" data-action="${index}">${action.label(row)}</button>`);
      cells.push(`<td class="row-actions">${buttons.join("")}</td>`);
    }
    tr.innerHTML = cells.join("");

    tr.querySelectorAll("[data-action]").forEach((button) => {
      button.addEventListener("click", async () => {
        button.disabled = true;
        try {
          await config.rowActions[Number(button.dataset.action)].run(row);
          await load();
          toast("Updated");
        } catch (err) {
          toast(err.message, "error");
          button.disabled = false;
        }
      });
    });
    return tr;
  }));
}

function renderPagination(config) {
  const bar = el("pagination");
  bar.hidden = !config.paged;
  if (!config.paged) return;

  el("page-info").textContent =
    `Page ${pageMeta.page + 1} of ${Math.max(pageMeta.totalPages, 1)} · ${pageMeta.totalElements} total`;
  el("prev-page").disabled = pageMeta.page <= 0;
  el("next-page").disabled = pageMeta.page >= pageMeta.totalPages - 1;
}

async function renderFilters(config) {
  const container = el("filters");
  container.innerHTML = "";
  for (const filter of config.filters ?? []) {
    const options = await request(RESOURCES[filter.from].path);
    const select = document.createElement("select");
    select.innerHTML = `<option value="">All ${filter.label.toLowerCase()}s</option>` +
      options.map((o) => `<option value="${o.id}">${escapeHtml(filter.optionLabel(o))}</option>`).join("");
    select.value = filters[filter.name] ?? "";
    select.addEventListener("change", () => {
      filters[filter.name] = select.value;
      pageMeta.page = 0;
      load();
    });
    container.appendChild(select);
  }
}

// ---- actions --------------------------------------------------------------

async function submitForm(event, config) {
  event.preventDefault();
  const error = el("form-error");
  error.hidden = true;

  // An impatient double-click on a money-moving form is two charges. Hold the button down for
  // the duration of the request; re-enable in `finally` so a failure doesn't strand the form.
  const submitButton = event.target.querySelector('button[type="submit"]');
  submitButton.disabled = true;

  const data = new FormData(event.target);
  const payload = {};
  for (const field of config.form.fields) {
    const raw = data.get(field.name);
    if (field.type === "money") payload[field.name] = Math.round(Number(raw) * 100);
    else if (field.cast) payload[field.name] = field.cast(raw);
    else payload[field.name] = raw;
  }

  // One key per submission: retrying THIS request is the same charge, a fresh click is a new one.
  // crypto.randomUUID needs a secure context — fine on localhost, which is how this runs.
  const headers = config.idempotent ? { "Idempotency-Key": crypto.randomUUID() } : undefined;

  try {
    const created = await request(config.path, { method: "POST", body: payload, headers });
    const result = config.onCreated?.(created) ?? { text: "Created", type: "success" };
    toast(result.text, result.type);
    await load();
  } catch (err) {
    error.textContent = err.message;
    error.hidden = false;
  } finally {
    submitButton.disabled = false;
  }
}

async function load() {
  const config = RESOURCES[active];
  const error = el("list-error");
  error.hidden = true;

  const params = new URLSearchParams();
  Object.entries(filters).forEach(([k, v]) => v && params.set(k, v));
  if (config.paged) params.set("page", pageMeta.page);

  try {
    const data = await request(`${config.path}${params.toString() ? `?${params}` : ""}`);
    if (config.paged) {
      rows = data.content;
      pageMeta = { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements };
    } else {
      rows = data;
    }
  } catch (err) {
    rows = [];
    error.textContent = err.message;
    error.hidden = false;
  }

  renderSummary(config);
  renderTable(config);
  renderPagination(config);
}

async function select(key) {
  active = key;
  location.hash = key;
  filters = {};
  pageMeta = { page: 0, totalPages: 1, totalElements: 0 };

  const config = RESOURCES[key];
  el("view-title").textContent = config.title;
  el("view-subtitle").textContent = config.subtitle ?? "";
  el("list-title").textContent = `All ${config.label.toLowerCase()}`;

  renderTabs();
  await renderFilters(config);
  await renderForm(config);
  await load();
}

function toast(text, type = "success") {
  const node = document.createElement("div");
  node.className = `toast toast-${type}`;
  node.textContent = text;
  el("toasts").appendChild(node);
  setTimeout(() => node.remove(), 4000);
}

// ---- wiring ---------------------------------------------------------------

el("refresh").addEventListener("click", load);
el("prev-page").addEventListener("click", () => { pageMeta.page -= 1; load(); });
el("next-page").addEventListener("click", () => { pageMeta.page += 1; load(); });
window.addEventListener("hashchange", () => {
  const key = location.hash.slice(1);
  if (key in RESOURCES && key !== active) select(key);
});

select(active);
