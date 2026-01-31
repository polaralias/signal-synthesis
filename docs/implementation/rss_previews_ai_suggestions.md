## Proposed Approach
### 1) Add previews to curated RSS catalog
  - Add a **provider-level RSS preview** modal: open from each provider header, with a toggle/selector to swipe through that provider’s topics/feeds inside the preview (so users don’t have to open 20+ previews).

### 2) AI Suggestions Consolidation (settings-wide)
Goal: replace individual per-setting suggestion buttons with a single **Suggest all settings** flow that can apply to a subset of areas.

**Proposed UX**
- Entry point: “Suggest all settings” (single button in Settings).
- Area selection: checklist with per-area toggles (e.g., **RSS**, **Risk**, **Financial markers**, etc.).
- Prompt input: one text box for the user’s context (risk tolerance, trading style, regions, themes, etc.).
- Run once: AI analysis spans all settings, but outputs are **only applied to selected areas**.
- Review/apply: show suggested diffs per area; user can apply all or tweak.
- History: show last prompt + last AI outputs; allow “Apply last suggestion” per area or re-run with a new prompt.

**Notes for RSS**
- RSS suggestions choose provider/topic toggles (not URLs).
- Preview remains available at provider level to validate the suggestions.


## Contextual Agent discussion (pre implementation plan)

### Human
Looks pretty good! Can we add a small RSS preview so the user can investigate each of the feeds before or after enabling them? 

In addition, there are quite a few feeds now!! Can we introduce an AI suggestion like we have for other settings, where the user enters risk, trading profile, etc. to identify what feeds to follow (e.g wallstreetbets vs daytrading vs uk vs ai) and so on

Requesting that did make me realise we have a number of ai suggestion settings now and I'm wondering if some consolidation is in order. Where the user can:

Request AI settings by selecting the areas to configure
RSS [X]
Risk []
Financial markers [X]
Enter key information for that in a text box
AI analysis is done *across all settings* but *only applied to the selected ones*
The user can then configure from there
The user can also update settings to match the last AI provided outputs (essentially, storing what *would have been selected* for Risk in the above example, then applying)
Or being given the option to request again by entering new information, whilst being shown what their previous prompt was

Can you replay that to me? to make sure we are on the same page before implementing

### Agent
Add a small RSS preview UI so users can inspect a feed before or after enabling it (likely per provider/topic toggle).
Add an “AI feed suggestion” flow similar to other settings: user inputs trading profile/risk/region/themes (e.g., WSB vs day‑trading vs UK vs AI) and the app suggests which RSS sources/topics to follow.
Consolidate AI suggestion settings into one entry point where users:
select which areas to configure (e.g., RSS / Risk / Financial markers),
provide a single text input,
run AI analysis once across all settings,
apply only the selected areas, and
optionally re‑apply the last AI outputs or re‑run with a new prompt while showing the previous prompt.
Questions to confirm scope/details:

Where should RSS preview live: inline under each topic toggle, or a modal/drawer “Preview feed” per source/topic?
For the consolidated AI settings, do you want a single screen that writes to existing per‑area settings, or a new “AI assistant” module that then branches into the existing screens?
Should “apply last AI outputs” be a one‑click action or a per‑area toggle?
If this matches your intent, I’ll propose a concrete UI/flow and update the plan accordingly.

### Human
Good questions
1) maybe at a provider level, with the option to swipe through different feeds with a toggle in the preview modal. Saves tapping preview on 20+ individual feeds
2) I think replace the individual suggestion buttons with a single "suggest all settings" kind of flow
3) I think consolidated in the suggest all settings area, but still with a per area toggle

Make sense?

### Agent
Yes, that makes sense. I’ve updated the plan to capture the provider‑level preview modal with topic/feeds toggle, and the single “Suggest all settings” flow with per‑area toggles and history/apply‑last behavior.

### Human
Make sure the settings screen doesnt overlap (the last PR was great, no sign of this bug) and check the app builds before sign off with .\gradlew assembleDebug. Oh also make sure the drop down expand buttons for the RSS providers work, a previous version had them broken.