#!/usr/bin/env node
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, join, resolve } from 'node:path';

const require = createRequire(import.meta.url);
const ROOT = resolve(import.meta.dirname, '..');
const URL = process.env.FOLIO_DEMO_URL || 'http://127.0.0.1:5173/';
const PLAYWRIGHT = process.env.PLAYWRIGHT_MODULE || 'playwright';
const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const ffmpeg = process.env.FFMPEG || 'ffmpeg';
const ffprobe = process.env.FFPROBE || 'ffprobe';
const { chromium } = require(PLAYWRIGHT);
const screenshots = join(ROOT, 'docs', 'images');
const output = join(ROOT, 'media', 'folio-demo.mp4');
const captionsPath = join(ROOT, 'docs', 'VIDEO_SCRIPT.md');
const narration = readFileSync(captionsPath, 'utf8')
  .split('\n')
  .map(line => line.match(/^\d+\.\s+(.+)$/)?.[1])
  .filter(Boolean);

if (narration.length !== 6) {
  throw new Error(`Expected 6 narration lines in ${captionsPath}; found ${narration.length}.`);
}

const work = mkdtempSync(join(tmpdir(), 'folio-demo-'));
const PRE_ROLL = 1.5;
const PAUSE = 0.45;
mkdirSync(screenshots, { recursive: true });
mkdirSync(join(ROOT, 'media'), { recursive: true });
const videoDir = join(work, 'recorded');
mkdirSync(videoDir);

const run = (command, args, options = {}) => execFileSync(command, args, { stdio: 'ignore', ...options });
const durationOf = path => Number(execFileSync(ffprobe, [
  '-v', 'error', '-show_entries', 'format=duration', '-of', 'default=noprint_wrappers=1:nokey=1', path,
], { encoding: 'utf8' }).trim());
const stamp = seconds => {
  const ms = Math.round(seconds * 1000);
  const h = String(Math.floor(ms / 3600000)).padStart(2, '0');
  const m = String(Math.floor((ms % 3600000) / 60000)).padStart(2, '0');
  const s = String(Math.floor((ms % 60000) / 1000)).padStart(2, '0');
  return `${h}:${m}:${s},${String(ms % 1000).padStart(3, '0')}`;
};

async function main() {
  const voiceClips = [];
  const clipDurations = [];
  for (let i = 0; i < narration.length; i++) {
    const aiff = join(work, `voice-${String(i + 1).padStart(2, '0')}.aiff`);
    const wav = join(work, `voice-${String(i + 1).padStart(2, '0')}.wav`);
    run('say', ['-v', 'Karen', '-r', '175', '-o', aiff, narration[i]], { stdio: 'inherit' });
    run(ffmpeg, ['-y', '-i', aiff, '-ar', '44100', '-ac', '1', '-c:a', 'pcm_s16le', wav]);
    voiceClips.push(wav);
    clipDurations.push(durationOf(wav));
  }

  const silence = join(work, 'silence.wav');
  run(ffmpeg, ['-y', '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=mono', '-t', String(PAUSE), '-c:a', 'pcm_s16le', silence]);
  const preRoll = join(work, 'pre-roll.wav');
  run(ffmpeg, ['-y', '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=mono', '-t', String(PRE_ROLL), '-c:a', 'pcm_s16le', preRoll]);
  const audioParts = [preRoll];
  for (const clip of voiceClips) audioParts.push(clip, silence);
  const manifest = join(work, 'audio-concat.txt');
  writeFileSync(manifest, audioParts.map(path => `file '${path}'`).join('\n') + '\n');
  const audio = join(work, 'narration.wav');
  run(ffmpeg, ['-y', '-f', 'concat', '-safe', '0', '-i', manifest, '-c:a', 'pcm_s16le', audio]);

  const srt = [];
  let time = PRE_ROLL;
  for (let i = 0; i < narration.length; i++) {
    srt.push(`${i + 1}\n${stamp(time)} --> ${stamp(time + clipDurations[i])}\n${narration[i]}\n`);
    time += clipDurations[i] + PAUSE;
  }
  const subtitleFile = join(ROOT, 'docs', 'VIDEO_CAPTIONS.srt');
  writeFileSync(subtitleFile, srt.join('\n'));

  const browser = await chromium.launch({ headless: true, executablePath: CHROME });
  const context = await browser.newContext({
    viewport: { width: 1600, height: 900 },
    deviceScaleFactor: 1,
    recordVideo: { dir: videoDir, size: { width: 1600, height: 900 } },
  });
  const page = await context.newPage();
  const recordingStartedAt = Date.now();
  await page.goto(URL, { waitUntil: 'networkidle' });
  await page.screenshot({ path: join(screenshots, 'folio-workspace.png') });

  const lineStart = index => PRE_ROLL + clipDurations.slice(0, index).reduce((sum, value) => sum + value + PAUSE, 0);
  const waitUntil = async seconds => {
    const remaining = seconds * 1000 - (Date.now() - recordingStartedAt);
    if (remaining > 0) await page.waitForTimeout(remaining);
  };

  await waitUntil(lineStart(0) + clipDurations[0]);
  await page.getByRole('button', { name: 'Run the synthetic investigation' }).click();
  const resultsReady = page.getByRole('heading', { name: 'Evidence overview' }).waitFor({ state: 'visible', timeout: 30000 });
  await waitUntil(lineStart(1));
  await resultsReady;
  await page.locator('#investigation').scrollIntoViewIfNeeded();
  await page.screenshot({ path: join(screenshots, 'folio-investigation.png') });
  await waitUntil(lineStart(1) + clipDurations[1]);

  await page.getByText('Conflicting declared income', { exact: true }).scrollIntoViewIfNeeded();
  await page.screenshot({ path: join(screenshots, 'folio-review-signal.png') });
  await waitUntil(lineStart(2) + clipDurations[2]);

  await page.getByRole('button', { name: /03_income_proof_altered/ }).click();
  await page.getByRole('heading', { name: 'Source trace' }).scrollIntoViewIfNeeded();
  await page.waitForTimeout(1200);
  await page.screenshot({ path: join(screenshots, 'folio-source-trace.png') });
  await waitUntil(lineStart(3) + clipDurations[3]);

  await page.getByRole('button', { name: 'Download dossier' }).scrollIntoViewIfNeeded();
  await waitUntil(lineStart(4) + clipDurations[4]);
  await page.locator('#method').scrollIntoViewIfNeeded();
  await page.screenshot({ path: join(screenshots, 'folio-method-limits.png') });
  await waitUntil(lineStart(5) + clipDurations[5] + 0.45);

  const raw = await page.video().path();
  await context.close();
  await browser.close();

  const filter = 'crop=1440:810:80:0,scale=1280:720,format=yuv420p';
  run(ffmpeg, [
    '-y', '-i', raw, '-i', audio, '-vf', filter, '-map', '0:v:0', '-map', '1:a:0',
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '21', '-r', '30', '-c:a', 'aac', '-b:a', '160k',
    '-shortest', '-movflags', '+faststart', output,
  ], { stdio: 'inherit' });

  console.log(`Video: ${output}`);
  console.log(`Screenshots: ${screenshots}`);
  console.log(`Recorded source: ${basename(raw)}`);
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
