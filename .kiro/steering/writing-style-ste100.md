# Writing style: Simplified Technical English (ASD-STE100) — always on

Write all prose in this session with ASD-STE100 discipline. The full skill is at
`/Applications/Repos/asd-ste100-skill/` (`SKILL.md` + `references/writing-rules.md`); apply it
without being asked. Linter: `scripts/ste-lint.py`.

## Mode
- **Strict** for text a system or agent parses without a human: tool/error/status messages,
  commit messages, code comments, procedures.
- **STE-flavored** for explanatory prose: chat replies, PR text.

## Exception — generated artifacts (do NOT apply STE100)
When generating an artifact — PRD, TRD, SDD, TTP, design documents, or any spec deliverable —
**ignore this skill and write in full detail.** Spec artifacts need completeness, nuance, and
exact traceability language, not clarity-compression. STE100 applies to chat replies and
operational text, not to the specs themselves.

## Structural rules (apply everywhere)
- Active voice. Name the actor.
- One instruction per sentence. Split compound sentences.
- Sentence length: about 20 words for instructions, about 25 for descriptions.
- No phrasal verbs ("remove", not "take off"; "start", not "spin up").
- No semicolons. Use separate sentences.
- Noun clusters: at most 3 stacked words.
- Verb, not nominalization ("analyze the log", not "perform an analysis of the log").
- No marketing adjectives (seamless, robust, powerful, blazing-fast).
- Simple tenses, with one exception below.
- Lists for 3+ steps or conditions.

## Preserve meaning over brevity (the rule that matters most)
- Keep every hedge and modal exactly. "May have failed" stays "may have failed". Do not
  promote a hedge to a fact.
- Add no fact, cause, or number the source did not state.
- Keep a compound tense when it carries current relevance ("the job has completed" vs. "the
  job completed") or a hedge. Flag the departure.
- Keep a safety condition, exception, or scope qualifier even if it makes a sentence longer.
- Stop when the text is unambiguous, not when it is shortest.

## Scope
Does not apply to creative or persuasive copy. STE fixes the form of a text, not its substance;
it does not make weak content correct. This is a clarity discipline inspired by ASD-STE100, not
a certified STE authoring tool; the official ~900-word dictionary is not reproduced here.
