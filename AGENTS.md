# Project Working Rules

## 1. Core Principles

- Keep changes focused on the user's request.
- Inspect the relevant code and repository state before editing.
- Follow the existing architecture, naming, and coding conventions.
- Do not overwrite, revert, or remove unrelated user changes.
- Prefer a complete implementation with verification over a partial proposal.
- If a safe assumption can be made from the repository, proceed without unnecessary questions.

## 2. Encoding and Shell

- All text files must use UTF-8.
- Prefer UTF-8 without BOM unless the existing file requires BOM.
- If a file's encoding is uncertain, stop and report the risk before editing it.
- The default shell is PowerShell 7 (`pwsh`).
- Do not add redundant encoding setup to every PowerShell command.

## 3. Task Levels

Classify each request by its actual impact.

### Level 0: Advice and Investigation

Examples:

- Technical explanations
- Repository inspection
- Code review
- GitHub or documentation research
- Architecture recommendations without file changes

Workflow:

- Investigate and answer directly.
- Do not create requirement, solution, version, or task-log documents.

### Level 1: Routine Change

Examples:

- Bug fixes
- Small features
- UI adjustments
- Test updates
- Local refactoring
- Documentation and configuration changes

Workflow:

1. Briefly state the goal, expected files, risks, and verification.
2. Implement without waiting for an extra confirmation unless the operation is destructive or ambiguous.
3. Run checks appropriate to the changed area.
4. Report the result, verification, and remaining risks.

Routine changes do not require a requirement document, solution document, or daily task log.

### Level 2: Major Change

A task is a major change when it includes one or more of the following:

- Public API or compatibility changes
- Database schema changes or data migrations
- Architecture or module-boundary changes
- Security, authorization, payment, or irreversible data operations
- A large feature spanning multiple modules
- A risky change with difficult rollback
- A milestone the user explicitly wants documented

Workflow:

1. Create one design document before implementation.
2. Show the document to the user and wait for confirmation.
3. Implement and verify the approved design.
4. Update long-term architecture or version documentation only when applicable.

The user may explicitly request a lighter or stricter workflow. Follow that request unless it would make the operation unsafe.

## 4. Major Change Document

Use one document instead of separate requirement and solution documents.

File name:

```text
docs/YYYY-MM-DD_<task-name>.md
```

Required sections:

- Goal
- Scope
- Non-goals
- Impact
- Implementation plan
- Files and modules involved
- Risks
- Rollback plan
- Verification
- Acceptance criteria
- Implementation result, completed after delivery

Use Chinese by default for project documents unless the user requests another language.

## 5. Long-Term Documentation

Update long-term documentation only when the accepted change affects its subject.

Update `docs/version-management.md` for:

- Public API changes
- Architecture changes
- Schema changes
- Explicitly tracked milestones

Update `docs/architecture-interface-requirements.md` for:

- Stable architecture rules
- Module boundaries
- Public interface contracts
- Persistent data-model constraints

When an architecture or interface change is recorded in both files, keep their version entries consistent.

Do not update long-term documents for routine fixes that do not change stable contracts.

`docs/daily-task-log.md` is optional. Update it only when the user requests it or when the task is an explicitly tracked project milestone.

## 6. Editing Rules

- Check `git status` before making changes.
- Read the relevant files before editing.
- Preserve unrelated modifications in a dirty worktree.
- Keep the change set as small as reasonably possible.
- Avoid unrelated refactoring, formatting churn, and dependency upgrades.
- Prefer existing helpers and patterns over new abstractions.
- Add comments only when the intent is not clear from the code.
- Never expose or modify secrets unless the task explicitly requires it.
- Do not commit `.env`, credentials, access tokens, private keys, or runtime uploads.
- Do not use destructive Git or filesystem commands without explicit user approval.

## 7. File and Module Size

- Avoid creating or expanding oversized files.
- Keep long prompts, system instructions, and prompt templates under `prompts/`.
- If a target file is already oversized, assess whether a focused split is needed before adding substantial logic.
- Do not combine a risky legacy-file split with an unrelated feature unless necessary.
- Generated files are exempt from manual file-size rules.

## 8. Verification

Verification must match the scope and risk of the change.

At minimum:

- Run the most relevant tests, checks, or build commands.
- Run `git diff --check`.
- Inspect the final diff for accidental or unrelated changes.

For higher-risk changes, also consider:

- Integration tests
- Database migration checks
- Browser or end-to-end tests
- Concurrency tests
- Security and authorization tests
- Rollback verification

If a check cannot be run, clearly state what was not verified and why.

## 9. Response Style

- Keep responses concise and structured.
- Present the conclusion first.
- During implementation, provide short progress updates when useful.
- The final response should include:
  - What changed
  - What was verified
  - Any remaining risk or limitation
- Do not create extra documents or logs unless required by these rules or requested by the user.

