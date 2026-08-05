# 06 · Scope review — cut what nobody asked for

Run this when the code starts feeling heavier than the problem, and once more before the endgame.
Over-engineering is the most common way this round is lost, and an agent will happily produce it if
you never push back.

```
Review everything we've built so far against the agreed feature list:
<paste the MVP list>

Find, specifically:
- abstractions with exactly one caller or one implementation
- code that exists for a requirement nobody stated
- fields, endpoints, or config we added "just in case"
- duplicated logic that is genuinely the same thing (not two things that merely look alike)
- anything in common/, the config, or the frontend shell you changed that didn't need changing

For each: name the file, say what it costs, and give me the one-line deletion. Rank by how much
simpler the code gets. If the honest answer is "nothing to cut", say that and stop — do not invent
work.

Do not fix anything yet. I'll pick.
```

Then apply the ones you agree with:

```
Apply <items 1, 3>. Behaviour must be identical — run ./mvnw -o test after and show me the result.
Nothing else changes.
```

## The specific asks that undo a bad turn

```
Delete the interface and put the logic directly in the <X>Service. Same behaviour.
```

```
Delete the mapper class. Use a static from(...) factory on the response record.
```

```
Collapse <AbstractX>/<BaseY> back into the concrete class. There is one subclass; there is no
abstraction to earn here.
```

```
That's a new dependency and I didn't approve one. Do it with what's already in pom.xml, or tell me
it genuinely can't be done and why.
```

## The self-check

If you're about to add an abstraction, ask: *is there a second caller today?* If no, inline it.

If you're about to add infrastructure — a cache, a queue, a scheduler, an event bus — say the
number that would justify it out loud. If you can't name the number, you don't need the
infrastructure, and saying so is worth more than building it.
