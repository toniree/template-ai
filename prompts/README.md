# Prompt library

Eight templates, ordered by when you use them. Each file is one copy-paste block with `<>`
placeholders to fill. `CLAUDE.md` is already loaded in every session, so none of these re-explain
the stack, the layout, or the no-over-engineering rules — keep it that way.

| When | File | Costs you |
|---|---|---|
| First 5 min, before any code | [`00-kickoff.md`](00-kickoff.md) | ~1 min |
| Once the requirements are on the table | [`01-scope-to-mvp.md`](01-scope-to-mvp.md) | ~1 min, saves 15 |
| Before the first line of code | [`02-data-model.md`](02-data-model.md) | ~2 min |
| Each feature, end to end | [`03-feature.md`](03-feature.md) | one prompt per feature |
| After a backend slice works | [`04-frontend.md`](04-frontend.md) | ~1 min per screen |
| The moment something breaks | [`05-debug.md`](05-debug.md) | ~30s |
| When it starts feeling heavy | [`06-scope-review.md`](06-scope-review.md) | ~2 min |
| Last 10 minutes | [`07-endgame.md`](07-endgame.md) | ~3 min, buys the ending |

## The rules these prompts all enforce

Every template below is written to make the agent:

1. **Inspect before changing.** Read the existing file, follow the pattern that's already there.
2. **State assumptions briefly**, then proceed — not ask five clarifying questions.
3. **Build the smallest thing that satisfies the requirement**, and stop.
4. **Reuse existing patterns** rather than inventing parallel ones.
5. **Work one vertical slice at a time**, never two features in one prompt.
6. **Run the tests after each slice.**
7. **Leave working infrastructure alone** — no redesigning the error handler, the config, or the UI
   shell because it's adjacent to the change.
8. **Ask before adding a dependency.**
9. **Stop polishing once the agreed features work.**
10. **Separate MVP from production follow-up** instead of quietly building the follow-up.

If a template you're editing loses one of those, put it back.

## How to run the session

1. **Never prompt without a plan.** `00-kickoff.md` → `01-scope-to-mvp.md` → `02-data-model.md`,
   then code. A wrong model costs 20 minutes; a wrong endpoint costs 2.
2. **One feature per prompt, vertical.** Entity → repo → service → controller → UI, then run it.
   You cannot review what you cannot read.
3. **Run after every feature.** `./mvnw -o test` and one curl. Broken-and-unnoticed is the only
   unrecoverable state in a 50-minute interview.
4. **Checkpoint every green feature.** `git add -A && git commit -q -m "feat: <feature>"`. Ten
   seconds, and it is what makes step 6 possible — an uncommitted green state cannot be returned to.
5. **Narrate the tradeoff, not the typing.** While the agent works, tell the interviewer what you
   asked for and what you deliberately excluded. That is the part being graded.
6. **Timebox debugging to 3 minutes.** Past that, `05-debug.md`. If it's still broken, throw the
   experiment away with `git reset --hard HEAD` (add `git clean -fd` if new files appeared), get
   back to your last checkpoint, and take the simpler path.
