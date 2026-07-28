---
type: "Decision"
title: "Decision 0004: Archive Policy"
description: "Documents Decision 0004: Archive Policy for the signal-synthesis repository."
timestamp: 2026-07-28T21:55:36Z
authority: canonical
verification: untested
owner: polaralias
tags:
  - signal-synthesis
  - decision
navigation:
  role: foundational
  order: 20
---
# Decision 0004: Archive Policy

Date: 2026-05-23

## Status

Accepted

## Decision

`docs/archive/` is historical context only.

Archive documents may contain:

- stale claims
- superseded plans
- resolved bugs
- earlier product framing

When archive content disagrees with code or canonical docs, agents must prefer current canonical docs plus implementation evidence.

## Meaning

- Archive content should not be treated as active support contract.
- Agents may use archive docs for context, but not as authority.
- If an archive document is still useful, that does not make it current.

## Why

This repository contains multiple generations of planning and repair material. An explicit archive boundary prevents old notes from re-entering the active contract by accident.

## Repository knowledge

- [Documentation map](../knowledge/documentation-map.md) — RKE-managed reading order and relationship hub.
