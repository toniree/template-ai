# 00 · Kickoff — orient the agent and surface the questions worth asking

Run this **first**, right after you hear the problem. It costs a minute and it produces the
clarifying questions you ask the interviewer — which is itself graded. No code comes out of it.

```
Here is the problem I've been given, verbatim:

"<paste the prompt exactly as it was given to you>"

Context: ~50 minutes total, solo, this Spring Boot + H2 scaffold. A working demo matters more
than coverage. Do NOT write or plan any code yet.

First, look at the repo as it stands so you're answering about this codebase and not a generic one.

Then give me, in this order and nothing else:

1. WHAT I THINK THEY'RE ASKING FOR — three bullets, in plain language. If your reading could be
   wrong, say which bullet is the shaky one.

2. THE FIVE QUESTIONS WORTH ASKING — ranked by how much the answer changes what I build. For each,
   give the one-line question I should say out loud, and what I'd do differently for each likely
   answer. Skip anything I can safely just decide myself.

3. WHAT I'D ASSUME IF I GET NO ANSWERS — the default reading of every ambiguity, one line each.
   These are what I'll state out loud and build against.

4. WHAT IN THIS REPO ALREADY APPLIES — name the files I keep as-is, and whether the sample `task`
   package should be deleted, renamed, or extended for this problem.

Be concrete and terse. No preamble, no code.
```

## Then

Ask the interviewer the top two or three questions. Say the assumptions out loud for anything they
wave off — *"I'll assume one user, no auth, single tenant; stop me if that's wrong."*

Then go to [`01-scope-to-mvp.md`](01-scope-to-mvp.md).
