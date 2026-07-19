# Decision 0006: Provider Model Discovery For OpenAI, Gemini, And Anthropic

Date: 2026-05-24

## Status

Accepted

## Decision

OpenAI, Gemini, and Anthropic model selection should no longer rely only on a static in-app catalogue during provider setup.

When a user saves an OpenAI, Gemini, or Anthropic API key, the app should call that provider's model-list endpoint, intersect the returned model IDs with the app's curated supported models, and use that discovered set for provider-specific defaults and settings choices.

The OpenAI default target is `gpt-5.4` when that key can access it. If the key cannot access `gpt-5.4`, the app should fall back to the best discovered curated OpenAI model instead of leaving a stale unavailable default selected.

Gemini should follow the same pattern with its own discovered available set and curated current defaults.

Anthropic should follow the same pattern, with `claude-sonnet-4-5` as the default current balanced tier when that key can access it, falling back to the best discovered curated Anthropic model instead of treating the first enum entry as the default.

## Meaning

- OpenAI, Gemini, and Anthropic now have feature parity for model discovery during setup.
- The app still owns curated tier labels and model ordering.
- Recommendation tiers are derived from model names, but generation and suffix must both be considered.
- Example: `gemini-3-flash-preview` can be a cheaper recommendation while `gemini-3.5-flash` is the default current-generation flash recommendation.
- The provider list-model endpoints decide availability, not pricing or support claims.
- Saved settings should be normalised away from stale defaults when discovery proves a better current default is available.

## Why

The repository had drift between static model enums, runtime provider reality, and the models actually available to a saved key.

That caused two problems:

- defaults aged out faster than the provider catalogues
- setup could present models the current key could not use
- Anthropic also defaulted too easily to the first curated model without discovery-backed balancing

Using provider discovery at key-save time keeps setup honest without turning the app into a free-form arbitrary model-ID surface.
