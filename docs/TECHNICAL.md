# Technical notes

## Runtime and dependencies

- Java 21, Spring Boot 3.5 and Spring Web for REST, multipart limits and JSON serialization.
- Apache PDFBox 3 for PDF loading, text extraction and rendering scanned pages.
- Tesseract command-line executable with `eng` data for optional image OCR. It runs locally with a timeout; no uploaded content is sent to a hosted OCR service.
- React 19, Vite, Tailwind CSS and Lucide React for the web workspace.

## Analysis details

### Ingestion and preservation

The API accepts PDF, PNG, JPG/JPEG, TIFF and BMP, up to 10 files / 15 MB each. Filenames are sanitized. Each original is written once under the investigation storage directory and SHA-256 is computed from the received bytes. A completed investigation writes a dossier and one JSON Lines event with hashes and finding identifiers.

### Text extraction

For PDFs, PDFBox reads page text. If total non-whitespace text is too small, pages are rasterized at 180 DPI and Tesseract TSV is read. Images go directly to Tesseract. OCR word boxes are grouped into line records; each OCR line retains normalized page coordinates and recognition score. PDF text lines retain page and line index; exact text coordinates are not retained. For text-layer PDFs, a lightweight line-level font-family count flags font names that differ from the page baseline (excluding the first line). This is an observation, not a tamper classifier.

### Structured fields

A small ordered set of English label patterns extracts applicant name, date of birth, national ID, address, account number, declared income, document date and a signature marker. Document type labels are keyword heuristics. Extracted values link back to up to three matching evidence IDs. Text-layer extraction uses a high fixed heuristic confidence; OCR uses Tesseract confidence from the best linked line. Both are extraction aids and are not calibrated probabilities.

### Reasoning and dossier

Cross-document fields are normalized by lowercasing and removing non-alphanumeric characters. Different normalized values produce a discrepancy signal with document names, source evidence IDs, a reason for review and a suggested action. Missing applicant names produce a low-severity review item. These are deterministic checks; there is no LLM, trained classifier, vector store or external source lookup. The dossier records source overview, field table, contextual findings, processing/visual observations, evidence locators, summary, limitations and a note on local audit/hash behavior.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | Spring Boot HTTP port |
| `APP_STORAGE` | `./data` | Local originals and generated investigation artifacts (relative to backend working directory) |
| `TESSERACT_CMD` | `tesseract` | OCR executable name/path |

Spring multipart limits are 15 MB/file and 40 MB/request. The app does not authenticate users or encrypt stored files. Run only on a trusted local machine for the prototype.

## Generated sample fixture

The `/api/demo` endpoint uses PDFBox to create three synthetic, selectable-text PDFs under `samples/generated/`, analyzes them and returns the investigation. `samples/dossiers/` contains an example result generated from those exact fixtures. They use fictional identity strings and explicitly disclose that the income proof is intentionally inconsistent. The generated dossier can be exported from the UI; copy that output to `samples/dossiers/sample-investigation.md` when preparing the submission bundle.
