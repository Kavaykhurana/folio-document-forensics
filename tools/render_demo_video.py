#!/usr/bin/env python3
"""Render a narrated slide-based Folio demo on macOS (requires soffice, pdftoppm, say and ffmpeg)."""
from pathlib import Path
import subprocess, tempfile
ROOT = Path(__file__).resolve().parents[1]
PPTX = ROOT / "presentation" / "Folio-Document-Forensics.pptx"
OUTPUT = ROOT / "media" / "folio-demo.mp4"
SCRIPT = ROOT / "docs" / "VIDEO_SCRIPT.md"
lines = [line.split(". ", 1)[1] for line in SCRIPT.read_text().splitlines() if line[:1].isdigit() and ". " in line]
with tempfile.TemporaryDirectory(prefix="folio-video-") as tmp:
    tmp = Path(tmp)
    pdf_dir, audio_dir, clips = tmp/"pdf", tmp/"audio", tmp/"clips"
    for p in (pdf_dir, audio_dir, clips): p.mkdir()
    subprocess.run(["soffice", "--headless", "--convert-to", "pdf", "--outdir", str(pdf_dir), str(PPTX)], check=True, stdout=subprocess.DEVNULL)
    pdf = pdf_dir / f"{PPTX.stem}.pdf"
    subprocess.run(["pdftoppm", "-png", "-r", "100", str(pdf), str(pdf_dir/"slide")], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    images = sorted(pdf_dir.glob("slide-*.png"))
    if len(images) != len(lines): raise SystemExit(f"Expected {len(lines)} slides and narration lines; found {len(images)} slides")
    concat = []
    for i, (image, narration) in enumerate(zip(images, lines), 1):
        audio = audio_dir/f"voice-{i:02}.aiff"
        wav = audio_dir/f"voice-{i:02}.wav"
        clip = clips/f"scene-{i:02}.mp4"
        subprocess.run(["say", "-v", "Karen", "-r", "175", "-o", str(audio), narration], check=True)
        subprocess.run(["ffmpeg", "-y", "-i", str(audio), str(wav)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        duration = float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", str(wav)], text=True))
        fade = max(0, duration - 0.25)
        vf = f"scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,fade=t=in:st=0:d=0.2,fade=t=out:st={fade:.3f}:d=0.25,format=yuv420p"
        subprocess.run(["ffmpeg", "-y", "-loop", "1", "-framerate", "30", "-i", str(image), "-i", str(wav), "-t", f"{duration:.3f}", "-vf", vf, "-c:v", "libx264", "-preset", "medium", "-crf", "22", "-c:a", "aac", "-b:a", "128k", "-shortest", str(clip)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        concat.append(f"file '{clip}'")
    manifest = tmp/"concat.txt"
    manifest.write_text("\n".join(concat)+"\n")
    subprocess.run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", str(manifest), "-c", "copy", "-movflags", "+faststart", str(OUTPUT)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
print(OUTPUT)
