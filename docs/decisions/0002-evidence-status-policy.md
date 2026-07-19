# Decision 0002: Evidence And Status Policy

Date: 2026-05-23

## Status

Accepted

## Decision

Repository status claims must use the existing labels:

- `verified working`
- `verified limited`
- `known broken`
- `untested`

A capability may be upgraded from `verified limited` to `verified working` only when both conditions are met:

1. The claimed path is covered by meaningful deterministic automated tests.
2. A documented manual real-provider scenario exists for any stage that depends on external providers or live integrations.

## Meaning

- Code presence alone is not enough for `verified working`.
- Unit tests alone are not enough for externally dependent flows.
- Agents should avoid inflating support claims from partial evidence.

## Why

This repository depends on live market-data and LLM providers. Automated tests can prove orchestration and deterministic behaviour, but they do not fully prove live integration behaviour on their own.

This policy creates a repeatable threshold for trustworthy support claims.
