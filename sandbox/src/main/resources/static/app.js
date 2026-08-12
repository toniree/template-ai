/*
 * Card authorization demo: seeded cards/merchants, a purchase form, and a recent-transactions
 * table. Bespoke screen against the DOM, not a config-driven renderer — see CLAUDE.md.
 */

const el = (id) => document.getElementById(id);

const STATUS_LABELS = { SUCCESS: "Approved", FAIL: "Declined", PENDING: "Pending" };

// ---- helpers --------------------------------------------------------------

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

const dollars = (minor) => `$${(minor / 100).toFixed(2)}`;

const timestamp = (iso) =>
  iso ? new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }) : "—";

function toast(text, type = "success") {
  const node = document.createElement("div");
  node.className = `toast toast-${type}`;
  node.textContent = text;
  el("toasts").appendChild(node);
  setTimeout(() => node.remove(), 4000);
}

function newIdempotencyKey() {
  return crypto.randomUUID();
}

/** Every call goes through here so the backend's ApiError shape is parsed in exactly one place. */
async function request(path, { method = "GET", body, headers } = {}) {
  const response = await fetch(path, {
    method,
    headers: body ? { "Content-Type": "application/json", ...headers } : headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const parsed = await response.json().catch(() => null);
  if (!response.ok) {
    const message = parsed?.details?.length
      ? `${parsed.message}: ${parsed.details.join("; ")}`
      : parsed?.message || `Request failed (${response.status})`;
    throw new Error(message);
  }
  return parsed;
}

// ---- cards / merchants ------------------------------------------------------

async function loadCards() {
  const rows = el("card-rows");
  rows.innerHTML = `<tr><td colspan="3" class="empty">Loading…</td></tr>`;
  try {
    const cards = await request("/api/cards");
    rows.innerHTML = cards.length ? "" : `<tr><td colspan="3" class="empty">No cards</td></tr>`;
    rows.replaceChildren(...cards.map((card) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `<td>${card.id}</td><td class="mono">••${escapeHtml(card.last4)}</td><td>${dollars(card.balanceMinor)}</td>`;
      return tr;
    }));
  } catch (err) {
    rows.innerHTML = `<tr><td colspan="3" class="empty">${escapeHtml(err.message)}</td></tr>`;
  }
}

async function loadMerchants() {
  const rows = el("merchant-rows");
  const select = el("merchant-select");
  rows.innerHTML = `<tr><td colspan="3" class="empty">Loading…</td></tr>`;
  try {
    const merchants = await request("/api/merchants");
    rows.innerHTML = merchants.length ? "" : `<tr><td colspan="3" class="empty">No merchants</td></tr>`;
    rows.replaceChildren(...merchants.map((m) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `<td>${m.id}</td><td>${escapeHtml(m.name)}</td><td class="muted">${escapeHtml(m.location)}</td>`;
      return tr;
    }));
    select.replaceChildren(...merchants.map((m) => {
      const opt = document.createElement("option");
      opt.value = m.id;
      opt.textContent = `${m.name} (#${m.id})`;
      return opt;
    }));
  } catch (err) {
    rows.innerHTML = `<tr><td colspan="3" class="empty">${escapeHtml(err.message)}</td></tr>`;
  }
}

async function loadTransactions() {
  const rows = el("transaction-rows");
  rows.innerHTML = `<tr><td colspan="7" class="empty">Loading…</td></tr>`;
  try {
    const transactions = await request("/api/transactions");
    rows.innerHTML = transactions.length ? "" : `<tr><td colspan="7" class="empty">No transactions yet</td></tr>`;
    rows.replaceChildren(...transactions.map((t) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${t.id}</td>
        <td>${t.cardId}</td>
        <td>${t.merchantId}</td>
        <td>${dollars(t.amountMinor)}</td>
        <td class="${t.status === 'SUCCESS' ? 'status-ok' : 'status-fail'}">${STATUS_LABELS[t.status] ?? t.status}</td>
        <td class="muted">${escapeHtml(t.errorReason) || "—"}</td>
        <td class="muted">${timestamp(t.createdDate)}</td>`;
      return tr;
    }));
  } catch (err) {
    rows.innerHTML = `<tr><td colspan="7" class="empty">${escapeHtml(err.message)}</td></tr>`;
  }
}

// ---- purchase form ------------------------------------------------------

function renderResult(response, ok) {
  const panel = el("purchase-result");
  panel.hidden = false;
  panel.className = `result-panel ${ok ? "result-ok" : "result-fail"}`;
  panel.innerHTML = ok
    ? `<strong>Approved</strong> — transaction #${response.transactionId}, remaining balance ${dollars(response.remainingBalanceMinor)}`
    : `<strong>Declined</strong> — ${escapeHtml(response.message || response)}`;
}

el("purchase-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.target;
  const button = form.querySelector('button[type="submit"]');
  const error = el("form-error");
  error.hidden = true;
  el("purchase-result").hidden = true;

  button.disabled = true;
  const data = new FormData(form);
  const idempotencyKey = data.get("idempotencyKey") || newIdempotencyKey();
  el("idempotency-key").value = idempotencyKey;

  try {
    const response = await request("/api/purchase", {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey },
      body: {
        cardNum: data.get("cardNum"),
        expiryDate: data.get("expiryDate"),
        cvc: data.get("cvc"),
        amountMinor: Math.round(Number(data.get("amount")) * 100),
        merchantId: Number(data.get("merchantId")),
        item: data.get("item") || null,
      },
    });
    renderResult(response, true);
    toast("Purchase approved");
  } catch (err) {
    renderResult(err, false);
    toast(err.message, "error");
  } finally {
    button.disabled = false;
    await Promise.all([loadCards(), loadTransactions()]);
  }
});

el("new-key").addEventListener("click", () => {
  el("idempotency-key").value = newIdempotencyKey();
});

el("refresh-cards").addEventListener("click", loadCards);
el("refresh-transactions").addEventListener("click", loadTransactions);

// ---- startup ------------------------------------------------------------

el("idempotency-key").value = newIdempotencyKey();
loadCards();
loadMerchants();
loadTransactions();
