# Investigation dossier

**Investigation:** `7beaecc3-0326-4e10-8540-f9fb55c22ef6`  
**Generated:** 2026-11-03T12:04:22.475619Z  
**Status:** Complete for automated prototype analysis  
**Decision boundary:** Findings are indicators for human review. No fraud determination is made.

## 7.1 Document overview

| Document | Type | Source | Status | SHA-256 |
|---|---|---|---|---|
| 01_application.pdf | Application form | local upload | processed | `40bac9e0aa0a841796e592bf3107ce4173c1a1c4315fd2d1394642a416b598e4` |
| 02_identity.pdf | Identity document | local upload | processed | `1b19ead300a0154eb6ddcb01cae745a5e8c90ca81ef33cc5501bd738e5438d23` |
| 03_income_proof_altered.pdf | Income proof | local upload | processed | `98c875f0ff4ce609073fd2eecb2f7f58faea814a2b5a6d04d57213f7ec96563c` |

## 7.2 Extracted information

### 01_application.pdf

| Field | Value | Confidence | Evidence location |
|---|---|---:|---|
| applicant_name | Asha Mehta | 94% | [E-6f76e0c8, E-b66a469d] |
| date_of_birth | 14 February 1992 | 94% | [E-e9536cd3] |
| national_id | DEMO 4821 6703 | 94% | [E-16443735] |
| address | 18 Sample Lane, Pune, Maharashtra | 94% | [E-7eb76aaa] |
| declared_income | INR 1,200,000 per annum | 94% | [E-94433064] |
| document_date | 02 November 2026 | 94% | [E-9fe9508a] |
| signature | Signature: Asha Mehta | 94% | [E-b66a469d] |

### 02_identity.pdf

| Field | Value | Confidence | Evidence location |
|---|---|---:|---|
| applicant_name | Asha Mehta | 94% | [E-6031dfab] |
| date_of_birth | 14 February 1992 | 94% | [E-9e2233c7] |
| national_id | DEMO 4821 6703 | 94% | [E-6dea5882] |
| address | 18 Sample Lane, Pune, Maharashtra | 94% | [E-86e8c325] |
| document_date | 12 August 2024 | 94% | [E-920d3bbb] |

### 03_income_proof_altered.pdf

| Field | Value | Confidence | Evidence location |
|---|---|---:|---|
| applicant_name | Asha Mehta | 94% | [E-4f02511d] |
| account_number | DEMO-AC-1938 | 94% | [E-270feb35] |
| declared_income | INR 2,100,000 per annum | 94% | [E-f0b40f75, E-dfa03217] |
| document_date | 01 November 2026 | 94% | [E-426cec47] |

## 7.3 Contextual analysis

- **Conflicting declared income** — 01_application.pdf = “INR 1,200,000 per annum”; 03_income_proof_altered.pdf = “INR 2,100,000 per annum”. The values may reflect a legitimate change, extraction error, or inconsistent source record; verify against authoritative evidence.
- **Font style differs from page baseline** — Potential visual style outlier at page 1, line 5 (Helvetica-Bold vs predominant Helvetica). This is a visual observation only; formatting differences can be intentional.

## 7.4 Visual forensic findings

- **01_application.pdf** — PDF text layer; 1 page(s). PDF text positions are not rendered as overlays; the font check is a basic page-level style comparison only.
- **02_identity.pdf** — PDF text layer; 1 page(s). PDF text positions are not rendered as overlays; the font check is a basic page-level style comparison only.
- **03_income_proof_altered.pdf** — PDF text layer; 1 page(s). Potential visual style outlier at page 1, line 5 (Helvetica-Bold vs predominant Helvetica). This is a visual observation only; formatting differences can be intentional. PDF text positions are not rendered as overlays; the font check is a basic page-level style comparison only.

## 7.5 Evidence and traceability

### 01_application.pdf (`40bac9e0aa0a841796e592bf3107ce4173c1a1c4315fd2d1394642a416b598e4`)

- `E-0f68d05e` — page 1, line 1 — “HACKTHECORE DEMONSTRATION — SYNTHETIC DATA” (PDF text layer)
- `E-866e5348` — page 1, line 2 — “Document type: Application form” (PDF text layer)
- `E-420f5cdf` — page 1, line 3 — “Application reference: DEMO-2026-041” (PDF text layer)
- `E-6f76e0c8` — page 1, line 4 — “Applicant name: Asha Mehta” (PDF text layer)
- `E-e9536cd3` — page 1, line 5 — “Date of birth: 14 February 1992” (PDF text layer)
- `E-16443735` — page 1, line 6 — “National ID: DEMO 4821 6703” (PDF text layer)
- `E-7eb76aaa` — page 1, line 7 — “Address: 18 Sample Lane, Pune, Maharashtra” (PDF text layer)
- `E-94433064` — page 1, line 8 — “Declared income: INR 1,200,000 per annum” (PDF text layer)
- `E-9fe9508a` — page 1, line 9 — “Document date: 02 November 2026” (PDF text layer)
- `E-b66a469d` — page 1, line 10 — “Signature: Asha Mehta” (PDF text layer)
- `E-3d93e53f` — page 1, line 11 — “All identities and values on this page are fictional.” (PDF text layer)

### 02_identity.pdf (`1b19ead300a0154eb6ddcb01cae745a5e8c90ca81ef33cc5501bd738e5438d23`)

- `E-fa55cef4` — page 1, line 1 — “HACKTHECORE DEMONSTRATION — SYNTHETIC DATA” (PDF text layer)
- `E-43996aac` — page 1, line 2 — “Document type: Identity document” (PDF text layer)
- `E-6031dfab` — page 1, line 3 — “Applicant name: Asha Mehta” (PDF text layer)
- `E-9e2233c7` — page 1, line 4 — “Date of birth: 14 February 1992” (PDF text layer)
- `E-6dea5882` — page 1, line 5 — “National ID: DEMO 4821 6703” (PDF text layer)
- `E-86e8c325` — page 1, line 6 — “Address: 18 Sample Lane, Pune, Maharashtra” (PDF text layer)
- `E-920d3bbb` — page 1, line 7 — “Document date: 12 August 2024” (PDF text layer)
- `E-a8b864de` — page 1, line 8 — “All identities and values on this page are fictional.” (PDF text layer)

### 03_income_proof_altered.pdf (`98c875f0ff4ce609073fd2eecb2f7f58faea814a2b5a6d04d57213f7ec96563c`)

- `E-d2cc2299` — page 1, line 1 — “HACKTHECORE DEMONSTRATION — SYNTHETIC DATA” (PDF text layer)
- `E-5ade58fa` — page 1, line 2 — “Document type: Income proof” (PDF text layer)
- `E-4f02511d` — page 1, line 3 — “Applicant name: Asha Mehta” (PDF text layer)
- `E-270feb35` — page 1, line 4 — “Account number: DEMO-AC-1938” (PDF text layer)
- `E-f0b40f75` — page 1, line 5 — “Declared income: INR 2,100,000 per annum” (PDF text layer)
- `E-426cec47` — page 1, line 6 — “Document date: 01 November 2026” (PDF text layer)
- `E-dfa03217` — page 1, line 7 — “This synthetic fixture is intentionally altered: the declared income is inconsistent and uses a different font style. It is not a real customer document.” (PDF text layer)

## 7.6 Investigation summary

2 review item(s) require human examination. Findings are indicators, not a determination of fraud.

- **Conflicting declared income (confidence 78%)**: Compare the original source fields and confirm which record is current. Evidence IDs: [E-94433064, E-f0b40f75, E-dfa03217].
- **Font style differs from page baseline (confidence 72%)**: Compare this source line with surrounding lines and the issuing template; a style change alone does not establish alteration. Evidence IDs: [E-f0b40f75].

### Limitations

Rule-based extraction supports a narrow set of English labels. OCR confidence is a recognition score, not truth confidence. PDF text-layer line locations are approximate; OCR image evidence uses normalized page bounding boxes. The prototype does not verify signatures, detect font manipulation reliably, or determine authenticity. Human review is required.

### Chain of custody notes

Original uploads are stored unchanged for the life of the local application data directory; SHA-256 is recorded at ingestion. A local append-only JSONL event file records the analysis event. This prototype is not a certified evidence preservation system.
