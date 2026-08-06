/*
 * The whole UI: search events, drill into one, book an available ticket. Written directly against
 * the DOM — deliberately not a config-driven renderer (see CLAUDE.md on app.js).
 */

const EVENTS_API = "/api/events";
const TICKETS_API = "/api/tickets";
const USERS_API = "/api/users";
const CURRENT_USER_KEY = "currentUserId";

const el = (id) => document.getElementById(id);

let events = [];
let currentEventId = null;
let users = [];
let currentUserId = localStorage.getItem(CURRENT_USER_KEY);

// ---- helpers --------------------------------------------------------------

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

const timestamp = (iso) =>
  iso ? new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }) : "—";

const money = (cents) => `$${(cents / 100).toFixed(2)}`;

function toast(text, type = "success") {
  const node = document.createElement("div");
  node.className = `toast toast-${type}`;
  node.textContent = text;
  el("toasts").appendChild(node);
  setTimeout(() => node.remove(), 4000);
}

/** Every call goes through here, so the backend's ApiError shape is parsed in exactly one place. */
async function request(path, { method = "GET", body } = {}) {
  const headers = {};
  if (body) headers["Content-Type"] = "application/json";
  if (currentUserId) headers["X-User-Id"] = currentUserId;

  const response = await fetch(path, {
    method,
    headers,
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

// ---- profile / current user -------------------------------------------------

function currentUser() {
  return users.find((u) => String(u.id) === String(currentUserId));
}

function selectUser(id) {
  currentUserId = String(id);
  localStorage.setItem(CURRENT_USER_KEY, currentUserId);
  el("user-picker").hidden = true;
  el("profile-menu").hidden = true;
  renderProfile();
  loadMine();
}

function logout() {
  currentUserId = null;
  localStorage.removeItem(CURRENT_USER_KEY);
  el("profile-menu").hidden = true;
  renderProfile();
  openUserPicker();
}

function userButton(user, onClick) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "profile-user-btn" + (String(user.id) === String(currentUserId) ? " active" : "");
  button.textContent = user.name;
  button.addEventListener("click", onClick);
  return button;
}

function openUserPicker() {
  const list = el("user-picker-list");
  list.replaceChildren(...users.map((u) => userButton(u, () => selectUser(u.id))));
  el("user-picker").hidden = false;
}

function renderProfile() {
  const user = currentUser();
  el("profile-toggle").hidden = !user;
  if (!user) return;

  el("profile-toggle-name").textContent = user.name;
  el("profile-name").textContent = user.name;
  el("profile-email").textContent = user.email;

  const list = el("profile-user-list");
  list.replaceChildren(...users.map((u) => userButton(u, () => selectUser(u.id))));
}

async function loadUsers() {
  users = await request(USERS_API);
  renderProfile();
  if (currentUserId && !currentUser()) {
    currentUserId = null;
    localStorage.removeItem(CURRENT_USER_KEY);
  }
  if (!currentUserId) {
    openUserPicker();
  } else {
    loadMine();
  }
}

el("profile-toggle").addEventListener("click", () => {
  el("profile-menu").hidden = !el("profile-menu").hidden;
});
el("profile-logout").addEventListener("click", logout);

// ---- my tickets ---------------------------------------------------------------

async function loadMine() {
  if (!currentUserId) return;
  const error = el("mine-error");
  error.hidden = true;
  try {
    renderMine(await request(`${TICKETS_API}/mine`));
  } catch (err) {
    error.textContent = err.message;
    error.hidden = false;
  }
}

function renderMine(tickets) {
  const body = el("mine-rows");
  if (!tickets.length) {
    body.innerHTML = `<tr><td colspan="5" class="empty">No tickets booked yet</td></tr>`;
    return;
  }
  body.replaceChildren(...tickets.map((t) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(t.eventTitle)}</td>
      <td>${escapeHtml(t.section)}</td>
      <td>${escapeHtml(t.seatLabel)}</td>
      <td>${money(t.priceCents)}</td>
      <td class="muted">${timestamp(t.bookedAt)}</td>`;
    return tr;
  }));
}

// ---- event list -------------------------------------------------------------

function renderEvents() {
  const body = el("event-rows");
  if (!events.length) {
    body.innerHTML = `<tr><td colspan="6" class="empty">No events found</td></tr>`;
    return;
  }
  body.replaceChildren(...events.map(eventRow));
}

function eventRow(event) {
  const tr = document.createElement("tr");
  tr.innerHTML = `
    <td>${escapeHtml(event.title)}</td>
    <td>${escapeHtml(event.artistName)}</td>
    <td>${escapeHtml(event.venueName)}</td>
    <td class="muted">${escapeHtml(event.location)}</td>
    <td class="muted">${timestamp(event.startTime)}</td>
    <td class="row-actions"><button type="button" class="btn btn-secondary btn-small">View tickets</button></td>`;

  tr.querySelector("button").addEventListener("click", () => openEvent(event.id, event.title));
  return tr;
}

async function loadEvents() {
  const error = el("list-error");
  error.hidden = true;
  const q = el("q").value.trim();
  const location = el("location").value.trim();
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (location) params.set("location", location);

  try {
    events = await request(params.toString() ? `${EVENTS_API}?${params}` : EVENTS_API);
  } catch (err) {
    events = [];
    error.textContent = err.message;
    error.hidden = false;
  }
  renderEvents();
}

// ---- event detail / tickets ------------------------------------------------

async function openEvent(id, title) {
  currentEventId = id;
  el("detail-panel").hidden = false;
  el("detail-title").textContent = title;
  el("detail-panel").scrollIntoView({ behavior: "smooth", block: "start" });
  await loadTickets();
}

el("close-detail").addEventListener("click", () => {
  currentEventId = null;
  el("detail-panel").hidden = true;
});

async function loadTickets() {
  const error = el("detail-error");
  error.hidden = true;
  try {
    const [detail, tickets] = await Promise.all([
      request(`${EVENTS_API}/${currentEventId}`),
      request(`${EVENTS_API}/${currentEventId}/tickets`),
    ]);
    el("available-count").textContent = `${detail.availableTicketCount} available`;
    renderTickets(tickets);
  } catch (err) {
    error.textContent = err.message;
    error.hidden = false;
  }
}

function renderTickets(tickets) {
  const body = el("ticket-rows");
  if (!tickets.length) {
    body.innerHTML = `<tr><td colspan="5" class="empty">No tickets for this event</td></tr>`;
    return;
  }
  body.replaceChildren(...tickets.map(ticketRow));
}

function ticketRow(ticket) {
  const tr = document.createElement("tr");
  const isAvailable = ticket.status === "AVAILABLE";
  tr.innerHTML = `
    <td>${escapeHtml(ticket.section)}</td>
    <td>${escapeHtml(ticket.seatLabel)}</td>
    <td>${money(ticket.priceCents)}</td>
    <td>${ticket.status === "BOOKED" ? `Booked${ticket.buyerName ? ` — ${escapeHtml(ticket.buyerName)}` : ""}` : "Available"}</td>
    <td class="row-actions">
      ${isAvailable ? `
        <form class="book-form">
          <input type="text" name="buyerName" placeholder="Name" required maxlength="200">
          <input type="email" name="buyerEmail" placeholder="Email" required maxlength="200">
          <button type="submit" class="btn btn-primary btn-small">Book</button>
        </form>` : ""}
    </td>`;

  const form = tr.querySelector("form");
  if (form) {
    form.addEventListener("submit", (event) => bookTicket(event, ticket.id));
  }
  return tr;
}

async function bookTicket(event, ticketId) {
  event.preventDefault();
  const form = event.target;
  const button = form.querySelector('button[type="submit"]');
  const data = new FormData(form);

  button.disabled = true;
  try {
    await request(`${TICKETS_API}/${ticketId}/book`, {
      method: "POST",
      body: { buyerName: data.get("buyerName"), buyerEmail: data.get("buyerEmail") },
    });
    toast("Ticket booked");
    await Promise.all([loadTickets(), loadMine()]);
  } catch (err) {
    toast(err.message, "error");
    button.disabled = false;
  }
}

// ---- wiring ---------------------------------------------------------------

el("refresh").addEventListener("click", loadEvents);
el("q").addEventListener("keydown", (e) => e.key === "Enter" && loadEvents());
el("location").addEventListener("keydown", (e) => e.key === "Enter" && loadEvents());

loadEvents();
loadUsers();
