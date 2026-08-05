# Prompt library

**Start with [`QUICK.md`](QUICK.md).** Four prompts — kickoff, one vertical slice, debug, endgame —
covering the whole interview. That's the file to keep open.

The numbered files below are the long-form versions: reach for one when a step needs more structure
than the quick prompt gives it, not as a checklist to work through. Each is one copy-paste block
with `<>` placeholders to fill. `CLAUDE.md` is already loaded in every session, so none of them
re-explain the stack, the layout, or the no-over-engineering rules — keep it that way.

| When | File |
|---|---|
| First minutes, before any code | [`00-kickoff.md`](00-kickoff.md) |
| Once the requirements are on the table | [`01-scope-to-mvp.md`](01-scope-to-mvp.md) |
| Before the first line of code | [`02-data-model.md`](02-data-model.md) |
| Each feature, end to end | [`03-feature.md`](03-feature.md) |
| After a backend slice works | [`04-frontend.md`](04-frontend.md) |
| The moment something breaks | [`05-debug.md`](05-debug.md) |
| When it starts feeling heavy | [`06-scope-review.md`](06-scope-review.md) |
| Last 10 minutes | [`07-endgame.md`](07-endgame.md) |

## The rules these prompts all enforce

Every template below is written to make the agent:

1. **Inspect before changing.** Read the existing file, follow the pattern that's already there.
2. **State assumptions briefly**, then proceed — not ask five clarifying questions.
3. **Build the smallest thing that satisfies the requirement**, and stop.
4. **Reuse existing patterns** rather than inventing parallel ones.
5. **Work one vertical slice at a time**, never two features in one prompt and never a horizontal
   layer across all of them.
6. **Run the tests after each slice**, and report the real count and result — never an estimate.
7. **Leave working infrastructure alone** — no redesigning the error handler, the config, or the UI
   shell because it's adjacent to the change.
8. **Ask before adding a dependency.**
9. **Stop polishing once the agreed features work.**
10. **Separate MVP from production follow-up** instead of quietly building the follow-up.

If a template you're editing loses one of those, put it back.

## How to run the session

1. **Never prompt without a plan.** The kickoff prompt first, always — model and API reviewed on
   paper before any code. A wrong model costs 20 minutes; a wrong endpoint costs 2.
2. **One feature per prompt, vertical.** Persistence (if any) → service → endpoint → test → UI,
   then run it. Never all the entities first, and never two features in one prompt — you cannot
   review what you cannot read.
3. **Run after every feature.** `./mvnw -o test` and one curl. Broken-and-unnoticed is the only
   unrecoverable state in a 50-minute interview.
4. **Checkpoint every green feature.** `git add -A && git commit -q -m "feat: <feature>"`. Ten
   seconds, and it is what makes step 6 possible — an uncommitted green state cannot be returned to.
5. **Narrate the tradeoff, not the typing.** While the agent works, tell the interviewer what you
   asked for and what you deliberately excluded. That is the part being graded.
6. **Timebox debugging to 3 minutes.** Past that, `05-debug.md`. If it's still broken, throw the
   experiment away with `git reset --hard HEAD` (add `git clean -fd` if new files appeared), get
   back to your last checkpoint, and take the simpler path.
