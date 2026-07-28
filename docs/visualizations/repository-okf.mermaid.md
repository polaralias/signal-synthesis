# signal-synthesis

> Generated from repository-local OKF records. The Markdown/YAML bundle remains canonical.

Source: `signal-synthesis`

The report separates the connected repository map from detailed component and key-concept views so large bundles remain reviewable.

## Connected-area overview

```mermaid
flowchart LR
    a0["docs · 35 concepts"]
    a1["repository root · 2 concepts"]
    a2["tasks · 1 concepts"]
    a0 -->|links| a1
    a0 -->|links| a2
    a1 -->|links| a0
    a2 -->|links| a0
    classDef default fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
```

## Connected component 1

### docs

```mermaid
flowchart LR
    n0["AnalysisViewModel And Pipeline Map"]:::knowledge
    n1["Anthropic Stage-Route Verification 2026-05-24"]:::knowledge
    n2["API Usage Tracking & Logging Enhancements"]:::knowledge
    n3["OpenAI Responses API Alignment Bugs"]:::knowledge
    n4["Implementation Guide: Signal Synthesis Android App"]:::knowledge
    n5["Phased Implementation Plan (Agent Guide)"]:::knowledge
    n6["Implementation Phase 2"]:::knowledge
    n7["Next Steps for Improving the Trading Companion App"]:::knowledge
    n8["Implementation Phase 4"]:::knowledge
    n9["Mock Mode Banner Feature"]:::knowledge
    n10["Implementation Plan: Converting MCP Server to an Android App"]:::knowledge
    n11["QA Checklist: Signal Synthesis Android App"]:::knowledge
    n12["Archive"]:::knowledge
    n13["Refactor + staged pipeline implementation reference (authoritative)"]:::knowledge
    n14["Rss Previews Ai Suggestions"]:::knowledge
    n15["Signal Synthesis Codebase Map"]:::knowledge
    n16["Decision 0001: Single Staged Pipeline Product Contract"]:::knowledge
    n17["Decision 0002: Evidence And Status Policy"]:::knowledge
    n18["Decision 0003: Canonical Documentation Hierarchy"]:::knowledge
    n19["Decision 0004: Archive Policy"]:::knowledge
    n20["Decision 0005: First TDD Proof Slice"]:::knowledge
    n21["Decision 0006: Provider Model Discovery For OpenAI, Gemini, And Anthropic"]:::knowledge
    n22["Decisions"]:::knowledge
    n23["signal-synthesis complete Markdown inventory"]:::knowledge
    n24["signal-synthesis documentation map"]:::knowledge
    n25["signal-synthesis repository OKF visualization"]:::knowledge
    n26["LLM provider documentation extraction"]:::knowledge
    n27["Product Contract"]:::knowledge
    n28["Public Readiness Verification 2026-05-24"]:::knowledge
    n29["Repository Knowledge Base"]:::knowledge
    n30["V2 Convergence Plan"]:::knowledge
    n31["V2 Manual Real-Provider Verification 2026-05-23"]:::knowledge
    n32["V2 Manual Real-Provider Verification Template"]:::knowledge
    n33["V2 Proof Gap"]:::knowledge
    n34["V2 Verification Matrix"]:::knowledge
    n35["Glossary"]:::boundary
    n36["Signal Synthesis"]:::boundary
    n37["Adopt RKE OKF knowledge format · done"]:::boundary
    n0 -->|links| n24
    n1 -->|links| n24
    n2 -->|links| n24
    n3 -->|links| n24
    n4 -->|links| n24
    n5 -->|links| n24
    n6 -->|links| n24
    n7 -->|links| n24
    n8 -->|links| n24
    n9 -->|links| n24
    n10 -->|links| n24
    n11 -->|links| n24
    n12 -->|links| n24
    n13 -->|links| n24
    n14 -->|links| n24
    n15 -->|links| n24
    n16 -->|links| n24
    n17 -->|links| n24
    n18 -->|links| n24
    n19 -->|links| n24
    n20 -->|links| n24
    n21 -->|links| n24
    n22 -->|links| n24
    n23 -->|links| n0
    n23 -->|links| n1
    n23 -->|links| n2
    n23 -->|links| n3
    n23 -->|links| n4
    n23 -->|links| n5
    n23 -->|links| n6
    n23 -->|links| n7
    n23 -->|links| n8
    n23 -->|links| n9
    n23 -->|links| n10
    n23 -->|links| n11
    n23 -->|links| n12
    n23 -->|links| n13
    n23 -->|links| n14
    n23 -->|links| n15
    n23 -->|links| n16
    n23 -->|links| n17
    n23 -->|links| n18
    n23 -->|links| n19
    n23 -->|links| n20
    n23 -->|links| n21
    n23 -->|links| n22
    n23 -->|links| n24
    n23 -->|links| n25
    n23 -->|links| n26
    n23 -->|links| n27
    n23 -->|links| n28
    n23 -->|links| n29
    n23 -->|links| n30
    n23 -->|links| n31
    n23 -->|links| n32
    n23 -->|links| n33
    n23 -->|links| n34
    n23 -->|links| n35
    n23 -->|links| n36
    n23 -->|links| n37
    n24 -->|links| n36
    n24 -->|links| n23
    n24 -->|links| n15
    n24 -->|links| n12
    n24 -->|links| n16
    n24 -->|links| n17
    n24 -->|links| n18
    n24 -->|links| n19
    n24 -->|links| n20
    n24 -->|links| n21
    n24 -->|links| n30
    n24 -->|links| n35
    n24 -->|links| n2
    n24 -->|links| n3
    n24 -->|links| n4
    n24 -->|links| n5
    n24 -->|links| n6
    n24 -->|links| n7
    n24 -->|links| n8
    n24 -->|links| n9
    n24 -->|links| n10
    n24 -->|links| n11
    n24 -->|links| n13
    n24 -->|links| n14
    n24 -->|links| n22
    n24 -->|links| n29
    n24 -->|links| n27
    n24 -->|links| n0
    n24 -->|links| n26
    n24 -->|links| n33
    n24 -->|links| n1
    n24 -->|links| n28
    n24 -->|links| n31
    n24 -->|links| n32
    n24 -->|links| n34
    n24 -->|links| n37
    n24 -->|links| n25
    n25 -->|links| n24
    n25 -->|links| n23
    n25 -->|links| n37
    n26 -->|links| n24
    n27 -->|links| n24
    n28 -->|links| n24
    n29 -->|links| n27
    n29 -->|links| n36
    n29 -->|links| n35
    n29 -->|links| n34
    n29 -->|links| n33
    n29 -->|links| n15
    n29 -->|links| n0
    n29 -->|links| n30
    n29 -->|links| n31
    n29 -->|links| n28
    n29 -->|links| n1
    n29 -->|links| n26
    n29 -->|links| n20
    n29 -->|links| n24
    n30 -->|links| n24
    n31 -->|links| n24
    n32 -->|links| n24
    n33 -->|links| n24
    n34 -->|links| n15
    n34 -->|links| n0
    n34 -->|links| n24
    n35 -->|links| n24
    n36 -->|links| n27
    n36 -->|links| n34
    n36 -->|links| n15
    n36 -->|links| n24
    n37 -->|links| n24
    n37 -->|links| n25
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### repository root

```mermaid
flowchart LR
    n0["Signal Synthesis Codebase Map"]:::boundary
    n1["signal-synthesis complete Markdown inventory"]:::boundary
    n2["signal-synthesis documentation map"]:::boundary
    n3["Product Contract"]:::boundary
    n4["Repository Knowledge Base"]:::boundary
    n5["V2 Verification Matrix"]:::boundary
    n6["Glossary"]:::knowledge
    n7["Signal Synthesis"]:::knowledge
    n0 -->|links| n2
    n1 -->|links| n0
    n1 -->|links| n2
    n1 -->|links| n3
    n1 -->|links| n4
    n1 -->|links| n5
    n1 -->|links| n6
    n1 -->|links| n7
    n2 -->|links| n7
    n2 -->|links| n1
    n2 -->|links| n0
    n2 -->|links| n6
    n2 -->|links| n4
    n2 -->|links| n3
    n2 -->|links| n5
    n3 -->|links| n2
    n4 -->|links| n3
    n4 -->|links| n7
    n4 -->|links| n6
    n4 -->|links| n5
    n4 -->|links| n0
    n4 -->|links| n2
    n5 -->|links| n0
    n5 -->|links| n2
    n6 -->|links| n2
    n7 -->|links| n3
    n7 -->|links| n5
    n7 -->|links| n0
    n7 -->|links| n2
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### tasks

```mermaid
flowchart LR
    n0["signal-synthesis complete Markdown inventory"]:::boundary
    n1["signal-synthesis documentation map"]:::boundary
    n2["signal-synthesis repository OKF visualization"]:::boundary
    n3["Adopt RKE OKF knowledge format · done"]:::task
    n0 -->|links| n1
    n0 -->|links| n2
    n0 -->|links| n3
    n1 -->|links| n0
    n1 -->|links| n3
    n1 -->|links| n2
    n2 -->|links| n1
    n2 -->|links| n0
    n2 -->|links| n3
    n3 -->|links| n1
    n3 -->|links| n2
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

## Key concept neighbourhoods

### signal-synthesis documentation map

```mermaid
flowchart LR
    n0["AnalysisViewModel And Pipeline Map"]:::boundary
    n1["Anthropic Stage-Route Verification 2026-05-24"]:::boundary
    n2["API Usage Tracking & Logging Enhancements"]:::boundary
    n3["OpenAI Responses API Alignment Bugs"]:::boundary
    n4["Implementation Guide: Signal Synthesis Android App"]:::boundary
    n5["Phased Implementation Plan (Agent Guide)"]:::boundary
    n6["Implementation Phase 2"]:::boundary
    n7["Next Steps for Improving the Trading Companion App"]:::boundary
    n8["Implementation Phase 4"]:::boundary
    n9["Mock Mode Banner Feature"]:::boundary
    n10["Implementation Plan: Converting MCP Server to an Android App"]:::boundary
    n11["QA Checklist: Signal Synthesis Android App"]:::boundary
    n12["Archive"]:::boundary
    n13["Refactor + staged pipeline implementation reference (authoritative)"]:::boundary
    n14["Rss Previews Ai Suggestions"]:::boundary
    n15["Signal Synthesis Codebase Map"]:::boundary
    n16["Decision 0001: Single Staged Pipeline Product Contract"]:::boundary
    n17["Decision 0002: Evidence And Status Policy"]:::boundary
    n18["Decision 0003: Canonical Documentation Hierarchy"]:::boundary
    n19["Decision 0004: Archive Policy"]:::boundary
    n20["Decision 0005: First TDD Proof Slice"]:::boundary
    n21["Decision 0006: Provider Model Discovery For OpenAI, Gemini, And Anthropic"]:::boundary
    n22["Decisions"]:::boundary
    n23["signal-synthesis complete Markdown inventory"]:::boundary
    n24["signal-synthesis documentation map"]:::knowledge
    n25["signal-synthesis repository OKF visualization"]:::boundary
    n26["LLM provider documentation extraction"]:::boundary
    n27["Product Contract"]:::boundary
    n28["Public Readiness Verification 2026-05-24"]:::boundary
    n29["Repository Knowledge Base"]:::boundary
    n30["V2 Convergence Plan"]:::boundary
    n31["V2 Manual Real-Provider Verification 2026-05-23"]:::boundary
    n32["V2 Manual Real-Provider Verification Template"]:::boundary
    n33["V2 Proof Gap"]:::boundary
    n34["V2 Verification Matrix"]:::boundary
    n35["Glossary"]:::boundary
    n36["Signal Synthesis"]:::boundary
    n37["Adopt RKE OKF knowledge format · done"]:::boundary
    n0 -->|links| n24
    n1 -->|links| n24
    n2 -->|links| n24
    n3 -->|links| n24
    n4 -->|links| n24
    n5 -->|links| n24
    n6 -->|links| n24
    n7 -->|links| n24
    n8 -->|links| n24
    n9 -->|links| n24
    n10 -->|links| n24
    n11 -->|links| n24
    n12 -->|links| n24
    n13 -->|links| n24
    n14 -->|links| n24
    n15 -->|links| n24
    n16 -->|links| n24
    n17 -->|links| n24
    n18 -->|links| n24
    n19 -->|links| n24
    n20 -->|links| n24
    n21 -->|links| n24
    n22 -->|links| n24
    n23 -->|links| n0
    n23 -->|links| n1
    n23 -->|links| n2
    n23 -->|links| n3
    n23 -->|links| n4
    n23 -->|links| n5
    n23 -->|links| n6
    n23 -->|links| n7
    n23 -->|links| n8
    n23 -->|links| n9
    n23 -->|links| n10
    n23 -->|links| n11
    n23 -->|links| n12
    n23 -->|links| n13
    n23 -->|links| n14
    n23 -->|links| n15
    n23 -->|links| n16
    n23 -->|links| n17
    n23 -->|links| n18
    n23 -->|links| n19
    n23 -->|links| n20
    n23 -->|links| n21
    n23 -->|links| n22
    n23 -->|links| n24
    n23 -->|links| n25
    n23 -->|links| n26
    n23 -->|links| n27
    n23 -->|links| n28
    n23 -->|links| n29
    n23 -->|links| n30
    n23 -->|links| n31
    n23 -->|links| n32
    n23 -->|links| n33
    n23 -->|links| n34
    n23 -->|links| n35
    n23 -->|links| n36
    n23 -->|links| n37
    n24 -->|links| n36
    n24 -->|links| n23
    n24 -->|links| n15
    n24 -->|links| n12
    n24 -->|links| n16
    n24 -->|links| n17
    n24 -->|links| n18
    n24 -->|links| n19
    n24 -->|links| n20
    n24 -->|links| n21
    n24 -->|links| n30
    n24 -->|links| n35
    n24 -->|links| n2
    n24 -->|links| n3
    n24 -->|links| n4
    n24 -->|links| n5
    n24 -->|links| n6
    n24 -->|links| n7
    n24 -->|links| n8
    n24 -->|links| n9
    n24 -->|links| n10
    n24 -->|links| n11
    n24 -->|links| n13
    n24 -->|links| n14
    n24 -->|links| n22
    n24 -->|links| n29
    n24 -->|links| n27
    n24 -->|links| n0
    n24 -->|links| n26
    n24 -->|links| n33
    n24 -->|links| n1
    n24 -->|links| n28
    n24 -->|links| n31
    n24 -->|links| n32
    n24 -->|links| n34
    n24 -->|links| n37
    n24 -->|links| n25
    n25 -->|links| n24
    n25 -->|links| n23
    n25 -->|links| n37
    n26 -->|links| n24
    n27 -->|links| n24
    n28 -->|links| n24
    n29 -->|links| n27
    n29 -->|links| n36
    n29 -->|links| n35
    n29 -->|links| n34
    n29 -->|links| n33
    n29 -->|links| n15
    n29 -->|links| n0
    n29 -->|links| n30
    n29 -->|links| n31
    n29 -->|links| n28
    n29 -->|links| n1
    n29 -->|links| n26
    n29 -->|links| n20
    n29 -->|links| n24
    n30 -->|links| n24
    n31 -->|links| n24
    n32 -->|links| n24
    n33 -->|links| n24
    n34 -->|links| n15
    n34 -->|links| n0
    n34 -->|links| n24
    n35 -->|links| n24
    n36 -->|links| n27
    n36 -->|links| n34
    n36 -->|links| n15
    n36 -->|links| n24
    n37 -->|links| n24
    n37 -->|links| n25
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### signal-synthesis complete Markdown inventory

```mermaid
flowchart LR
    n0["AnalysisViewModel And Pipeline Map"]:::boundary
    n1["Anthropic Stage-Route Verification 2026-05-24"]:::boundary
    n2["API Usage Tracking & Logging Enhancements"]:::boundary
    n3["OpenAI Responses API Alignment Bugs"]:::boundary
    n4["Implementation Guide: Signal Synthesis Android App"]:::boundary
    n5["Phased Implementation Plan (Agent Guide)"]:::boundary
    n6["Implementation Phase 2"]:::boundary
    n7["Next Steps for Improving the Trading Companion App"]:::boundary
    n8["Implementation Phase 4"]:::boundary
    n9["Mock Mode Banner Feature"]:::boundary
    n10["Implementation Plan: Converting MCP Server to an Android App"]:::boundary
    n11["QA Checklist: Signal Synthesis Android App"]:::boundary
    n12["Archive"]:::boundary
    n13["Refactor + staged pipeline implementation reference (authoritative)"]:::boundary
    n14["Rss Previews Ai Suggestions"]:::boundary
    n15["Signal Synthesis Codebase Map"]:::boundary
    n16["Decision 0001: Single Staged Pipeline Product Contract"]:::boundary
    n17["Decision 0002: Evidence And Status Policy"]:::boundary
    n18["Decision 0003: Canonical Documentation Hierarchy"]:::boundary
    n19["Decision 0004: Archive Policy"]:::boundary
    n20["Decision 0005: First TDD Proof Slice"]:::boundary
    n21["Decision 0006: Provider Model Discovery For OpenAI, Gemini, And Anthropic"]:::boundary
    n22["Decisions"]:::boundary
    n23["signal-synthesis complete Markdown inventory"]:::knowledge
    n24["signal-synthesis documentation map"]:::boundary
    n25["signal-synthesis repository OKF visualization"]:::boundary
    n26["LLM provider documentation extraction"]:::boundary
    n27["Product Contract"]:::boundary
    n28["Public Readiness Verification 2026-05-24"]:::boundary
    n29["Repository Knowledge Base"]:::boundary
    n30["V2 Convergence Plan"]:::boundary
    n31["V2 Manual Real-Provider Verification 2026-05-23"]:::boundary
    n32["V2 Manual Real-Provider Verification Template"]:::boundary
    n33["V2 Proof Gap"]:::boundary
    n34["V2 Verification Matrix"]:::boundary
    n35["Glossary"]:::boundary
    n36["Signal Synthesis"]:::boundary
    n37["Adopt RKE OKF knowledge format · done"]:::boundary
    n0 -->|links| n24
    n1 -->|links| n24
    n2 -->|links| n24
    n3 -->|links| n24
    n4 -->|links| n24
    n5 -->|links| n24
    n6 -->|links| n24
    n7 -->|links| n24
    n8 -->|links| n24
    n9 -->|links| n24
    n10 -->|links| n24
    n11 -->|links| n24
    n12 -->|links| n24
    n13 -->|links| n24
    n14 -->|links| n24
    n15 -->|links| n24
    n16 -->|links| n24
    n17 -->|links| n24
    n18 -->|links| n24
    n19 -->|links| n24
    n20 -->|links| n24
    n21 -->|links| n24
    n22 -->|links| n24
    n23 -->|links| n0
    n23 -->|links| n1
    n23 -->|links| n2
    n23 -->|links| n3
    n23 -->|links| n4
    n23 -->|links| n5
    n23 -->|links| n6
    n23 -->|links| n7
    n23 -->|links| n8
    n23 -->|links| n9
    n23 -->|links| n10
    n23 -->|links| n11
    n23 -->|links| n12
    n23 -->|links| n13
    n23 -->|links| n14
    n23 -->|links| n15
    n23 -->|links| n16
    n23 -->|links| n17
    n23 -->|links| n18
    n23 -->|links| n19
    n23 -->|links| n20
    n23 -->|links| n21
    n23 -->|links| n22
    n23 -->|links| n24
    n23 -->|links| n25
    n23 -->|links| n26
    n23 -->|links| n27
    n23 -->|links| n28
    n23 -->|links| n29
    n23 -->|links| n30
    n23 -->|links| n31
    n23 -->|links| n32
    n23 -->|links| n33
    n23 -->|links| n34
    n23 -->|links| n35
    n23 -->|links| n36
    n23 -->|links| n37
    n24 -->|links| n36
    n24 -->|links| n23
    n24 -->|links| n15
    n24 -->|links| n12
    n24 -->|links| n16
    n24 -->|links| n17
    n24 -->|links| n18
    n24 -->|links| n19
    n24 -->|links| n20
    n24 -->|links| n21
    n24 -->|links| n30
    n24 -->|links| n35
    n24 -->|links| n2
    n24 -->|links| n3
    n24 -->|links| n4
    n24 -->|links| n5
    n24 -->|links| n6
    n24 -->|links| n7
    n24 -->|links| n8
    n24 -->|links| n9
    n24 -->|links| n10
    n24 -->|links| n11
    n24 -->|links| n13
    n24 -->|links| n14
    n24 -->|links| n22
    n24 -->|links| n29
    n24 -->|links| n27
    n24 -->|links| n0
    n24 -->|links| n26
    n24 -->|links| n33
    n24 -->|links| n1
    n24 -->|links| n28
    n24 -->|links| n31
    n24 -->|links| n32
    n24 -->|links| n34
    n24 -->|links| n37
    n24 -->|links| n25
    n25 -->|links| n24
    n25 -->|links| n23
    n25 -->|links| n37
    n26 -->|links| n24
    n27 -->|links| n24
    n28 -->|links| n24
    n29 -->|links| n27
    n29 -->|links| n36
    n29 -->|links| n35
    n29 -->|links| n34
    n29 -->|links| n33
    n29 -->|links| n15
    n29 -->|links| n0
    n29 -->|links| n30
    n29 -->|links| n31
    n29 -->|links| n28
    n29 -->|links| n1
    n29 -->|links| n26
    n29 -->|links| n20
    n29 -->|links| n24
    n30 -->|links| n24
    n31 -->|links| n24
    n32 -->|links| n24
    n33 -->|links| n24
    n34 -->|links| n15
    n34 -->|links| n0
    n34 -->|links| n24
    n35 -->|links| n24
    n36 -->|links| n27
    n36 -->|links| n34
    n36 -->|links| n15
    n36 -->|links| n24
    n37 -->|links| n24
    n37 -->|links| n25
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### Repository Knowledge Base

```mermaid
flowchart LR
    n0["AnalysisViewModel And Pipeline Map"]:::boundary
    n1["Anthropic Stage-Route Verification 2026-05-24"]:::boundary
    n2["Signal Synthesis Codebase Map"]:::boundary
    n3["Decision 0005: First TDD Proof Slice"]:::boundary
    n4["signal-synthesis complete Markdown inventory"]:::boundary
    n5["signal-synthesis documentation map"]:::boundary
    n6["LLM provider documentation extraction"]:::boundary
    n7["Product Contract"]:::boundary
    n8["Public Readiness Verification 2026-05-24"]:::boundary
    n9["Repository Knowledge Base"]:::knowledge
    n10["V2 Convergence Plan"]:::boundary
    n11["V2 Manual Real-Provider Verification 2026-05-23"]:::boundary
    n12["V2 Proof Gap"]:::boundary
    n13["V2 Verification Matrix"]:::boundary
    n14["Glossary"]:::boundary
    n15["Signal Synthesis"]:::boundary
    n0 -->|links| n5
    n1 -->|links| n5
    n2 -->|links| n5
    n3 -->|links| n5
    n4 -->|links| n0
    n4 -->|links| n1
    n4 -->|links| n2
    n4 -->|links| n3
    n4 -->|links| n5
    n4 -->|links| n6
    n4 -->|links| n7
    n4 -->|links| n8
    n4 -->|links| n9
    n4 -->|links| n10
    n4 -->|links| n11
    n4 -->|links| n12
    n4 -->|links| n13
    n4 -->|links| n14
    n4 -->|links| n15
    n5 -->|links| n15
    n5 -->|links| n4
    n5 -->|links| n2
    n5 -->|links| n3
    n5 -->|links| n10
    n5 -->|links| n14
    n5 -->|links| n9
    n5 -->|links| n7
    n5 -->|links| n0
    n5 -->|links| n6
    n5 -->|links| n12
    n5 -->|links| n1
    n5 -->|links| n8
    n5 -->|links| n11
    n5 -->|links| n13
    n6 -->|links| n5
    n7 -->|links| n5
    n8 -->|links| n5
    n9 -->|links| n7
    n9 -->|links| n15
    n9 -->|links| n14
    n9 -->|links| n13
    n9 -->|links| n12
    n9 -->|links| n2
    n9 -->|links| n0
    n9 -->|links| n10
    n9 -->|links| n11
    n9 -->|links| n8
    n9 -->|links| n1
    n9 -->|links| n6
    n9 -->|links| n3
    n9 -->|links| n5
    n10 -->|links| n5
    n11 -->|links| n5
    n12 -->|links| n5
    n13 -->|links| n2
    n13 -->|links| n0
    n13 -->|links| n5
    n14 -->|links| n5
    n15 -->|links| n7
    n15 -->|links| n13
    n15 -->|links| n2
    n15 -->|links| n5
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### Signal Synthesis

```mermaid
flowchart LR
    n0["Signal Synthesis Codebase Map"]:::boundary
    n1["signal-synthesis complete Markdown inventory"]:::boundary
    n2["signal-synthesis documentation map"]:::boundary
    n3["Product Contract"]:::boundary
    n4["Repository Knowledge Base"]:::boundary
    n5["V2 Verification Matrix"]:::boundary
    n6["Signal Synthesis"]:::knowledge
    n0 -->|links| n2
    n1 -->|links| n0
    n1 -->|links| n2
    n1 -->|links| n3
    n1 -->|links| n4
    n1 -->|links| n5
    n1 -->|links| n6
    n2 -->|links| n6
    n2 -->|links| n1
    n2 -->|links| n0
    n2 -->|links| n4
    n2 -->|links| n3
    n2 -->|links| n5
    n3 -->|links| n2
    n4 -->|links| n3
    n4 -->|links| n6
    n4 -->|links| n5
    n4 -->|links| n0
    n4 -->|links| n2
    n5 -->|links| n0
    n5 -->|links| n2
    n6 -->|links| n3
    n6 -->|links| n5
    n6 -->|links| n0
    n6 -->|links| n2
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### V2 Verification Matrix

```mermaid
flowchart LR
    n0["AnalysisViewModel And Pipeline Map"]:::boundary
    n1["Signal Synthesis Codebase Map"]:::boundary
    n2["signal-synthesis complete Markdown inventory"]:::boundary
    n3["signal-synthesis documentation map"]:::boundary
    n4["Repository Knowledge Base"]:::boundary
    n5["V2 Verification Matrix"]:::knowledge
    n6["Signal Synthesis"]:::boundary
    n0 -->|links| n3
    n1 -->|links| n3
    n2 -->|links| n0
    n2 -->|links| n1
    n2 -->|links| n3
    n2 -->|links| n4
    n2 -->|links| n5
    n2 -->|links| n6
    n3 -->|links| n6
    n3 -->|links| n2
    n3 -->|links| n1
    n3 -->|links| n4
    n3 -->|links| n0
    n3 -->|links| n5
    n4 -->|links| n6
    n4 -->|links| n5
    n4 -->|links| n1
    n4 -->|links| n0
    n4 -->|links| n3
    n5 -->|links| n1
    n5 -->|links| n0
    n5 -->|links| n3
    n6 -->|links| n5
    n6 -->|links| n1
    n6 -->|links| n3
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

### Signal Synthesis Codebase Map

```mermaid
flowchart LR
    n0["Signal Synthesis Codebase Map"]:::knowledge
    n1["signal-synthesis complete Markdown inventory"]:::boundary
    n2["signal-synthesis documentation map"]:::boundary
    n3["Repository Knowledge Base"]:::boundary
    n4["V2 Verification Matrix"]:::boundary
    n5["Signal Synthesis"]:::boundary
    n0 -->|links| n2
    n1 -->|links| n0
    n1 -->|links| n2
    n1 -->|links| n3
    n1 -->|links| n4
    n1 -->|links| n5
    n2 -->|links| n5
    n2 -->|links| n1
    n2 -->|links| n0
    n2 -->|links| n3
    n2 -->|links| n4
    n3 -->|links| n5
    n3 -->|links| n4
    n3 -->|links| n0
    n3 -->|links| n2
    n4 -->|links| n0
    n4 -->|links| n2
    n5 -->|links| n4
    n5 -->|links| n0
    n5 -->|links| n2
    classDef task fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef workstream fill:#ede9fe,stroke:#7c3aed,color:#2e1065
    classDef tracker fill:#ffedd5,stroke:#ea580c,color:#431407
    classDef knowledge fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef boundary fill:#f8fafc,stroke:#64748b,color:#0f172a,stroke-dasharray:4 3
```

## Legend

- Blue: task
- Purple: workstream
- Orange: tracker profile
- Green: durable knowledge
- Dashed neutral nodes: neighbouring context repeated from another area or key-concept view
- Time references: edges to addressable `Task.time[]` fragments
- Arrows: structured relationships or repository-local Markdown links
