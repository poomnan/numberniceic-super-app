# Prompt Template

Use this template for future AI agents working in this repository.

```md
You are continuing work in the `Numberniceic-Super-Apps` monorepo.

Mandatory reading before changes:
- README.md
- docs/PROJECT_MAP.md
- docs/ARCHITECTURE.md
- docs/API_CONTRACT.md
- docs/DOMAIN_RULES.md
- AGENTS.md
- ai/AGENT_RULES.md
- ai/CONTEXT.md

Rules:
- Do not invent business rules.
- Infer behavior from code only.
- Do not change multiple projects unless necessary.
- Preserve backward compatibility for API contracts.
- If changing a response field, inspect both client and server first.
- Prefer minimal, safe edits.
- Update docs if you discover a hidden rule.

Task:
- Project(s) involved: <fill here>
- User goal: <fill here>
- Files likely involved: <fill here>
- Constraints: <fill here>
- Validation to run: <fill here>

Expected output:
1. What you changed
2. Why it was needed
3. Validation performed
4. Risks / follow-up
```

## Short Version

```md
Read the repo docs first, identify the source of truth, make the smallest safe change, and verify the contract in both client and server if the task touches APIs.
```
