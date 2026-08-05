# Prompt library

Ordered by when you use them, not by what they generate. Each file is one copy-paste block with
`<>` placeholders to fill. `CLAUDE.md` is already loaded in every session, so none of these need to
re-explain the stack, the layout, or the no-over-engineering rules — keep it that way.

| When | File | Costs you |
|---|---|---|
| First 5 min, before any code | [`00-kickoff.md`](00-kickoff.md) | ~2 min, saves 15 |
| Each feature, end to end | [`01-feature.md`](01-feature.md) | one prompt per feature |
| After the backend works | [`02-frontend.md`](02-frontend.md) | ~1 min per screen |
| The moment something breaks | [`03-debug.md`](03-debug.md) | ~30s |
| Once features are agreed-done | [`04-tests.md`](04-tests.md) | ~2 min |
| Last 10 minutes | [`05-endgame.md`](05-endgame.md) | ~3 min, buys the ending |

## How to run the session

1. **Never prompt without a plan.** `00-kickoff.md` first, always. Approve the data model before a
   line of code exists — a wrong model costs 20 minutes, a wrong endpoint costs 2.
2. **One feature per prompt, vertical.** Entity → repo → service → controller → UI, then run it.
   Never queue two features in one prompt; you cannot review what you cannot read.
3. **Run after every feature.** `./mvnw -o test` and one curl. Broken-and-unnoticed is the only
   unrecoverable state in a 50-minute interview.
4. **Narrate the tradeoff, not the typing.** While the agent works, tell the interviewer what you
   asked for and what you deliberately excluded. That is the part being graded.
5. **Timebox debugging to 3 minutes.** Past that, `03-debug.md`, and if it's still broken, revert
   to the last green state and take the simpler path.
