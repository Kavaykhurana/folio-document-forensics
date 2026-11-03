# 4-minute live demo

1. Start the app using the root README. Open `http://localhost:5173`.
2. Choose **Run the synthetic investigation**. Explain that all three records are fictional and the income-proof fixture is intentionally inconsistent.
3. Show document type, page count and extracted fields. Point to extraction confidence and clarify it is not truth confidence.
4. Open the “Conflicting declared income” signal. Compare the application and income proof values, review its evidence IDs and recommended verification action. Open the font-style observation and trace it to line 5 in the income proof. State explicitly that a typeface difference can be intentional and does not prove alteration.
5. Select the application and income-proof sources. Show the original PDF next to its evidence lines and trace the field to a page/line reference. The dossier also includes source SHA-256 values.
6. Download the dossier and open it. Walk through document overview, extracted info, contextual analysis, visual observations, traceability and limitations.
7. Close by stating that the signal asks for review; Folio does not decide fraud. Pixel-level alteration detection, handwriting and signature authentication are future work.

## Backup walkthrough

If the live OCR executable is missing, the supplied synthetic PDF fixtures have selectable text layers and can still be analyzed. OCR is only needed for scanned uploads and image fixtures.
