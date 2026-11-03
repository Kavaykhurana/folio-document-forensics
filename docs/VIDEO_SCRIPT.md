# Folio video walkthrough narration

1. Folio turns document noise into an evidence trail. The prototype helps investigators compare related records and see why a result needs review.
2. OCR can read a field. By itself, it cannot establish whether a document is authentic or whether another record disagrees.
3. Folio accepts PDFs and images, extracts supported fields, and keeps each value connected to its source.
4. For text PDFs, PDFBox reads the text layer. Scanned pages and images can use local English Tesseract OCR.
5. The engine compares supported fields across documents. Here, two synthetic records disagree on declared annual income.
6. The application lists one million two hundred thousand rupees. The income proof lists two million one hundred thousand.
7. That is a review signal, not a fraud verdict. A reviewer can inspect the evidence IDs and original source.
8. A basic font-family check flags a line whose font differs from the page baseline. Formatting alone proves nothing.
9. The dossier records document overview, extracted fields, contextual signals, source references, hashes, and limitations.
10. Folio uses deterministic rules today. Handwriting, signature validation, and pixel-level tamper localization remain unsupported. Human review stays in control.
