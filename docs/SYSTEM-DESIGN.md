# Fintech system design — the first 50 minutes

For the non-AI round. The three named failure modes are: **solutioning before clarifying**,
**not asking about scope and scale**, and **over-engineering**. All three are avoidable by
spending the first ten minutes not drawing boxes.

---

## The clock

| Minutes | What |
|---|---|
| 0–8 | Clarify. Requirements, actors, scope boundaries. Write the agreed list where they can see it. |
| 8–12 | Scale. Ask for the numbers, do the arithmetic out loud, state what the numbers imply. |
| 12–22 | Data model. Entities, relationships, keys, the money representation. This is the core. |
| 22–32 | API + the critical path end to end. One request, all the way through. |
| 32–42 | Failure modes and edge cases. Where it breaks and what happens when it does. |
| 42–50 | Scale it *only now*, and only where the numbers demanded it. Then trade-offs. |

Scaling before the model is correct is the over-engineering trap. Sequence protects you.

---

## 1. Clarify before you draw

Ask these; do not assume any of them.

**Product boundary**
- Who are the actors? (cardholder, admin, finance/approver, merchant, the network)
- Is this issuing (we give out cards) or acquiring (we process merchants' payments)? Very different.
- Real money or a closed-loop / stored-value system?
- Do we hold funds, or does a bank partner? Are we the ledger of record?
- Single currency or multi? Multi-currency is a 3x complexity multiplier — pin it down early.
- One business per account, or a hierarchy (org → department → employee → card)?

**Scope**
- Are we building the authorization path, the money movement, or the reporting on top?
- Is fraud/risk in scope, or a black box we call?
- Which of this is v1 versus later?

**Non-functional**
- What's the latency budget on the critical path? (For card auth this is the binding constraint.)
- What's the consistency requirement? Where is eventual consistency acceptable?
- Compliance posture — PCI scope, audit retention, data residency?

> Say explicitly: *"I'm going to spend a few minutes on requirements before I draw anything."*
> Then do it. Interviewers are grading that you did.

---

## 2. Get the numbers, then do the arithmetic

Ask for: businesses, cards per business, transactions per card per month, peak-to-average ratio,
data retention.

A plausible mid-size issuer: **100k businesses × 10 cards × 50 txns/month ≈ 50M txns/month**.

```
50M / month ÷ 30 ÷ 86,400  ≈  19 authorizations/second average
peak (10x, lunchtime + month-end)  ≈  200/second
50M txns × ~500 bytes      ≈  25 GB/month of transaction rows
```

**The conclusion you should reach out loud:** 200 writes/second is nothing. A single well-indexed
Postgres primary handles this with room to spare. Card authorization is **not a throughput
problem — it is a latency and correctness problem.** The card network expects an answer in
single-digit hundreds of milliseconds, and a wrong answer moves real money.

That reframe is the highest-value sentence in the whole round. It also earns you the right to *not*
draw Kafka, sharding, and a cache tier — because you showed the math that says you don't need them.

---

## 3. Data model

### Money, non-negotiable

- Integer **minor units** (`long amount_minor`), never float/double. `0.1 + 0.2 != 0.3` moves money.
- Store the **ISO-4217 currency next to every amount**. A bare number is a bug waiting to happen.
- Minor-unit scale is not always 2: JPY has 0, KWD/BHD/TND have 3. Don't hardcode `/100` if
  multi-currency is in scope.
- For FX, store the rate and the timestamp you used. The rate at authorization differs from the
  rate at settlement, and that delta is a real line item.

### The ledger question — and the answer that shows judgment

Two ways to know a balance:

| | Derived (`SUM` the entries) | Maintained (a `balance` column) |
|---|---|---|
| Correctness | Cannot drift | Drifts on any missed update |
| Read cost | Grows with history | O(1) |
| Concurrency | Needs care on the write check | Needs a lock or atomic update |

The senior answer is *both, in sequence*: **derive it until the numbers say otherwise, then add a
maintained balance plus a periodic reconciliation job that re-derives and alerts on mismatch.**
Snapshot balances at period boundaries so the `SUM` never scans all of history.

For real money movement, **double-entry**: every event writes ≥2 entries that sum to zero. Entries
are **append-only and immutable** — a correction is a new reversing entry, never an `UPDATE`.
That property is what makes the system auditable, and auditability is the actual product
requirement behind "it's a ledger."

### Corporate card core

```
business(id, name, ...)
card(id, business_id, user_id, last4, status, spend_limit_minor, currency, created_at)
authorization(id, card_id, amount_minor, currency, merchant, mcc, status,
              decline_reason, network_ref, idempotency_key UNIQUE, expires_at, created_at)
transaction(id, authorization_id, captured_amount_minor, posted_at)   -- clearing
ledger_entry(id, account_id, transaction_id, direction, amount_minor, currency, created_at)
```

Index for the queries you actually run: `(card_id, created_at DESC)` for a card's activity,
`(business_id, posted_at)` for statements, unique on `idempotency_key`, unique on `network_ref`
for replay protection from the network.

### The lifecycle you should name unprompted

**Authorization → Capture → Settlement**, and they are *not* the same event:

- **Authorization** places a hold. It does not move money. It expires (commonly ~7 days, longer for
  travel/hotel).
- **Capture** (clearing) is the merchant claiming it — and it can be for a **different amount**
  (tip added, partial shipment) or never happen at all.
- **Settlement** is the actual funds movement, in a batch, typically T+1 or T+2.

Modelling capture as "update the auth row" is the mistake. They are separate records with a
one-to-many relationship, because one authorization can be captured in parts.

Also worth naming: **reversals** (merchant cancels an auth), **incremental authorizations**
(hotel raising the hold), and **force posts** — a capture with no matching authorization, which the
networks permit via offline/stand-in approval. Force posts are why **a real card balance can go
negative regardless of your limit checks**. Raising that unprompted is a strong domain signal.

---

## 4. API design

- **Idempotency on every money-moving POST.** Client supplies `Idempotency-Key`; you store it with
  a unique constraint and return the original response on replay. Without it, a client timeout plus
  a retry is a double charge. Scope keys per endpoint, expire them (Stripe: 24h), and decide what
  happens when the same key arrives with a *different* body — reject with 409, don't silently serve
  the old answer.
- **A declined charge is a successful API call.** 201 with `status: DECLINED` and a machine-readable
  `decline_reason`. Reserve 4xx for requests you could not process at all. Clients branch on codes;
  humans read messages.
- **Cursor pagination** (`created_at, id` as the cursor), not `OFFSET`. Offset pagination skips and
  duplicates rows when the underlying data is being written to, which for transactions it always is.
- **Never expose the PAN.** `last4` plus a token. Full PAN in your database or logs drags everything
  it touches into PCI scope — the design goal is *scope reduction*.
- **Webhooks for anything asynchronous**, with signed payloads, at-least-once delivery, and an
  explicitly documented expectation that consumers are idempotent.
- **Immutability**: financial records are corrected by appending a reversal, never by `UPDATE` or
  `DELETE`. It's why the card API in this repo has no `DELETE` — cards get cancelled.

---

## 5. Failure modes and edge cases

This section is where the round is usually won. Have these ready:

**Concurrency — two charges racing the same limit.** Name a mechanism and its cost:
- `SELECT ... FOR UPDATE` on the card/account row: correct, serializes spend on one card. Fine —
  one card's concurrent authorizations are inherently rare.
- Optimistic locking (`@Version`): cheaper, but you must handle the retry.
- **A conditional update is often best:**
  ```sql
  UPDATE card SET spent_minor = spent_minor + :amt
   WHERE id = :id AND spent_minor + :amt <= spend_limit_minor
  ```
  Atomic, one round trip, no explicit lock; zero rows affected means declined. Needs a maintained
  `spent_minor` column though — so it trades away the derived balance above.

This repo implements the first option (`CardRepository.findWithLockById`), because it keeps the
balance derived. If asked why not the conditional update: it's faster, but it requires the
denormalized column and the drift risk that comes with it.

**Watch the arithmetic too.** `spent + amount > limit` overflows for a large enough amount, wraps
negative, and reads as "under the limit". Compare as `amount > limit - spent`, where both operands
are non-negative and the subtraction cannot overflow.

**Partial failure.** Committed to the database, then the process died before responding. The client
retries — idempotency key makes that safe. This is *the* reason idempotency exists; say it that way.

**Dual writes.** Writing to the database and publishing an event are not one transaction. Use the
**transactional outbox**: write the event to a table in the same transaction, relay it separately.

**Exactly-once is a myth.** You get at-least-once delivery plus idempotent processing. Saying this
plainly is a senior signal.

**Reconciliation.** The network's daily settlement file is the source of truth for what actually
happened. Diff it against your ledger, quarantine mismatches into an exceptions queue for a human.
Any answer that assumes your own database is authoritative about external money movement is wrong.

**Other edge cases worth having in your pocket:** authorization expiry sweeping stale holds ·
capture amount ≠ authorized amount · refund exceeding the original · currency mismatch ·
clock skew and why timestamps come from one source · a cancelled card with in-flight
authorizations · timezone and cutoff for "which billing cycle does this belong to."

---

## 6. Which database, and why

**Postgres, for the money.** Be able to defend it in one breath: ACID transactions across the rows
that must agree, constraints that make invalid states unrepresentable (unique idempotency keys,
`CHECK (amount_minor > 0)`), `SELECT FOR UPDATE` for contended balances, JSONB for the metadata
blobs that always appear, partial indexes, and mature operational tooling. Money wants
strong consistency and referential integrity; that's the whole argument.

**Why not a document store here:** the model is relational — accounts, entries, and transactions
are joins — and the constraints you need most are cross-document. Multi-document transactions exist
but you're paying for a flexibility you don't want on a ledger.

**Scale it in this order, and only when the numbers say so:**
1. Index correctly. Most "we need to shard" is a missing composite index.
2. Read replicas for reporting — and name the catch: replication lag, so never read-your-own-writes
   off a replica.
3. Partition transactions by time (monthly). Makes retention a `DROP PARTITION` instead of a
   `DELETE` that fights vacuum.
4. Shard by `business_id` if a single primary genuinely runs out. Tenant-keyed sharding keeps every
   query inside one shard.
5. Move analytics off OLTP entirely (ClickHouse/Snowflake/BigQuery). Never run finance's reporting
   queries against the authorization path.

**Supporting pieces, each with a reason:** Redis for velocity/fraud counters and rate limits (not as
a source of truth); Kafka when you have genuine multiple consumers of the event stream, not
because it's on the diagram; S3 + Parquet for settlement files and cold storage.

---

## 7. If they go deep

- **Isolation levels.** Postgres defaults to READ COMMITTED. `SERIALIZABLE` gives you correctness at
  the cost of serialization failures you must retry. Know that a balance check under READ COMMITTED
  is a read-modify-write and needs a lock or a conditional update to be safe.
- **Velocity limits / rate limiting.** Token bucket for smoothness, sliding-window counter for
  accuracy at low memory; know why a fixed window lets 2x through at the boundary.
- **Idempotency at scale.** Unique index is the correctness mechanism; a Redis lookup in front is
  the latency optimization. Never the reverse.
- **Statement/billing cycles.** Date arithmetic in the business's timezone, cutoffs, proration, and
  what happens to a transaction that posts after the cycle closed (it lands in the next one).
- **Reconciliation as an algorithm.** Sort both sides by `(network_ref, amount)` and merge-join;
  it's a set difference over sorted streams, O(n log n), streamable without loading either side.
- **Fraud/velocity checks** on the auth path have a hard latency budget — precomputed counters, not
  aggregate queries.

---

## 8. Lines that land

- "Before I design anything, let me make sure I understand what we're building."
- "Let me do the arithmetic — 200 writes per second means one Postgres primary. I'm not going to
  shard this, and here's the number that says I don't have to."
- "That's a latency problem, not a throughput problem."
- "I'll derive the balance now and add a maintained one plus reconciliation when the reads justify
  it."
- "Exactly-once delivery doesn't exist. At-least-once plus an idempotent consumer does."
- "I'd cut that from v1. Here's what it costs us and when I'd revisit."

And the one that defuses the over-engineering trap outright:

> "I could add a queue and a cache here, but at this volume it's complexity without benefit. I'd
> rather keep it simple and revisit when we see the load. Let me tell you the metric that would
> change my mind."
