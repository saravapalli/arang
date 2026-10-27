# Kiro Mechanism Selection — Nudge the Right Tool

When the user describes something they want remembered, automated, referenced, or
reviewed, proactively suggest which Kiro mechanism fits — don't just do it inline.
Name the mechanism, say why, and offer to set it up. Decide using the table and triggers
below. When two fit, say so and recommend one.

## Decision guide
| If the user wants… | Use | Why |
|---|---|---|
| A **rule/convention** the assistant must ALWAYS follow | **Steering file** (`.kiro/steering/*.md`) | Always in context every turn. Keep short. |
| A **repeatable procedure / how-to** needed only SOMETIMES | **Skill** (`.kiro/skills/<name>/SKILL.md`) | Dormant until relevant; full body loads on demand. |
| To **search a large corpus** of docs/specs/code by meaning | **Knowledge base** (`/knowledge add`) | Searchable storage, not always-in-context. |
| An **independent actor** with its own prompt/tools/clean context | **Agent** (`.kiro/agents/*.json`) or one-off sub-agent (`/spawn`) | Isolated context + tool scoping; fresh eyes. |

## Trigger phrases → suggestion (nudge the user when you hear these)
- "always do X" / "never do X" / "our convention is…" / "follow this rule" →
  **suggest a steering file.** (If it's long, suggest a skill instead to save context.)
- "whenever we do X, follow these steps" / "a checklist for…" / "the process for…" /
  "review X the same way each time" → **suggest a skill.**
- "I have lots of docs/specs/logs and want to ask questions about them" / "find where we
  decided…" / "search our past notes" → **suggest a knowledge base** (index the folder).
- "get an independent/second opinion" / "who reviewed this?" / "fresh eyes" /
  "run this with limited tools" / "do this in parallel while I keep working" →
  **suggest an agent / sub-agent.**

## Quick rules of thumb
- **Always-follow rule → steering. Sometimes-run procedure → skill. Query-a-big-corpus →
  knowledge base. Independent actor → agent.**
- Steering is expensive (every turn) — keep it tiny and universal; move bulk to a skill.
- Skill vs agent: SAME assistant picks up a skill in OUR context; an AGENT is a SEPARATE
  mind with isolated context and its own tool permissions.
- Prefer a **one-off sub-agent** (`/spawn`) for a single independent task (no file needed);
  create a **saved agent** config only when the persona will be reused.
- Knowledge base earns its keep only once there's more material than fits in context.

## Behavior
- Before saving a rule inline or writing a long procedure into chat, pause and recommend
  the appropriate mechanism, then act on the user's choice.
- It's fine to use more than one (e.g. a reviewer **skill** for collaborative passes PLUS
  an independent reviewer **agent** for a final cold-eyes audit).
