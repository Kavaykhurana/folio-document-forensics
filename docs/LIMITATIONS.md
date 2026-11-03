# Limitations, assumptions and unsupported scenarios

## Implemented

- PDF and common raster image ingestion; one investigation can include up to 10 files.
- Selectable PDF text extraction and local Tesseract fallback for scanned PDFs/images.
- Narrow English label/value extraction for applicant name, DOB, national ID, address, account number, declared income, document date and signature marker.
- Heuristic document type labels, field-level extraction confidence, source evidence references, SHA-256 at ingest, and cross-document exact-normalized value discrepancy checks.
- A structured dossier, local JSONL completion event, and a source review screen.

## Partial

- OCR on clear, printed English is supported when Tesseract and its English language data are installed. Handwriting, multilingual text, low resolution, skew, glare and complex tables may fail or be wrong.
- For OCR evidence, page number and normalized bounding boxes are recorded. The interface shows extracted lines beside the source but does not draw region overlays.
- For PDFs with a text layer, evidence points to page and line. A basic font-family outlier check compares line fonts to the page baseline; exact glyph coordinates, font metrics, layout consistency, overlays and robust alteration detection are not implemented.
- Classification is based on labels and text cues; it is not a trained classifier.
- Cross-document relationship is assumed from documents submitted together. There is no entity resolution or discovery across unrelated cases.

## Unsupported

- Determining document authenticity or fraud, digital signature validation, handwritten signature verification, image splice / copy-move detection, reliable font-change detection, metadata provenance, PDF object-level forensic analysis, pixel-level tamper localization, handwriting recognition, translation, external identity checks, and decision automation.
- Authentication, role-based access, encryption at rest, remote deployment hardening, production retention controls and tamper-evident audit storage.
- A hosted AI agent or LLM. Explanations are generated from deterministic rules, and should not be described as model reasoning.

## Confidence and false positives

OCR confidence measures recognition quality, not whether a value is true. Rule-based mismatch confidence is a fixed review-signal score, not a calibrated probability. Different dates, name transliterations, OCR substitutions, legitimate address changes, formatting, duplicate labels and data entry errors can all create false positives or missed discrepancies. Every finding must be checked against the source and an authoritative record.

## Assumptions

- Submit only documents that the team is authorized to process.
- Documents in one upload belong to the same case unless a reviewer says otherwise.
- The backend host is trusted; files remain in local application storage until manually removed.
- Tesseract is installed and its `eng` traineddata is available for scanned inputs.

## Standards

The implementation uses integrity hashes and source references as basic evidence-handling practices. It is not independently validated and does not claim compliance or certification against ISO/IEC 27037, 27041, 27042, NIST SP 800-86 or OWASP ML Security guidance.
