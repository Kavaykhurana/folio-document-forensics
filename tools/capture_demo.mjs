#!/usr/bin/env node
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, join, resolve } from 'node:path';

const require = createRequire(import.meta.url);
const ROOT = resolve(import.meta.dirname, '..');
const URL = process.env.FOLIO_DEMO_URL || 'http://127.0.0.1:5173/';
const VOICE = process.env.FOLIO_VOICE_AUDIO;
const PLAYWRIGHT = process.env.PLAYWRIGHT_MODULE || 'playwright';
const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const ffmpeg = process.env.FFMPEG || 'ffmpeg';
const ffprobe = process.env.FFPROBE || 'ffprobe';
const { chromium } = require(PLAYWRIGHT);
const screenshots = join(ROOT, 'docs', 'images');
const output = join(ROOT, 'media', 'folio-demo.mp4');
const scriptPath = join(ROOT, 'docs', 'VIDEO_SCRIPT.md');
const narration = readFileSync(scriptPath, 'utf8')
  .split('\n')
  .map(line => line.match(/^\d+\.\s+(.+)$/)?.[1])
  .filter(Boolean);

if (!VOICE) throw new Error('Set FOLIO_VOICE_AUDIO to the human voice memo file.');
if (narration.length !== 6) throw new Error(`Expected 6 narration paragraphs in ${scriptPath}; found ${narration.length}.`);

const PRE_ROLL = 1.5;
const TRIM_LEAD = Number(process.env.FOLIO_TRIM_LEAD || 0.4);
const TRIM_END = Number(process.env.FOLIO_TRIM_END || 55.0);
const sceneStarts = (process.env.FOLIO_SCENE_STARTS || '0.6,8.1,16.6,28.3,34.2,46.6').split(',').map(Number);
const sceneEnds = (process.env.FOLIO_SCENE_ENDS || '7.1,15.8,27.3,33.5,45.4,54.7').split(',').map(Number);
if (sceneStarts.length !== 6 || sceneEnds.length !== 6 || sceneStarts.some((x, i) => !(x < sceneEnds[i]))) {
  throw new Error('FOLIO_SCENE_STARTS and FOLIO_SCENE_ENDS must provide six start/end times in seconds.');
}

const work = mkdtempSync(join(tmpdir(), 'folio-human-demo-'));
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
  const voice = join(work, 'voice.wav');
  run(ffmpeg, [
    '-y', '-i', VOICE, '-af', `atrim=start=${TRIM_LEAD}:end=${TRIM_END},asetpts=PTS-STARTPTS,loudnorm=I=-16:TP=-1.5:LRA=11`,
    '-ar', '44100', '-ac', '1', '-c:a', 'pcm_s16le', voice,
  ]);
  const voiceDuration = durationOf(voice);
  if (sceneEnds[5] - TRIM_LEAD > voiceDuration + 0.2) {
    throw new Error('Scene end times extend past the voice memo. Adjust FOLIO_SCENE_ENDS or FOLIO_TRIM_END.');
  }

  const silence = join(work, 'pre-roll.wav');
  run(ffmpeg, ['-y', '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=mono', '-t', String(PRE_ROLL), '-c:a', 'pcm_s16le', silence]);
  const manifest = join(work, 'audio-concat.txt');
  writeFileSync(manifest, `file '${silence}'\nfile '${voice}'\n`);
  const audio = join(work, 'narration.wav');
  run(ffmpeg, ['-y', '-f', 'concat', '-safe', '0', '-i', manifest, '-c:a', 'pcm_s16le', audio]);

  const captions = narration.map((line, index) => {
    const start = PRE_ROLL + sceneStarts[index] - TRIM_LEAD;
    const end = PRE_ROLL + sceneEnds[index] - TRIM_LEAD;
    return `${index + 1}\n${stamp(start)} --> ${stamp(end)}\n${line}\n`;
  }).join('\n');
  writeFileSync(join(ROOT, 'docs', 'VIDEO_CAPTIONS.srt'), captions);

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

  const target = sourceTime => PRE_ROLL + sourceTime - TRIM_LEAD;
  const waitUntil = async seconds => {
    const remaining = seconds * 1000 - (Date.now() - recordingStartedAt);
    if (remaining > 0) await page.waitForTimeout(remaining);
  };

  await waitUntil(target(sceneEnds[0]));
  await page.getByRole('button', { name: 'Run the synthetic investigation' }).click();
  const resultsReady = page.getByRole('heading', { name: 'Evidence overview' }).waitFor({ state: 'visible', timeout: 30000 });
  await waitUntil(target(sceneStarts[1]));
  await resultsReady;
  await page.locator('#investigation').scrollIntoViewIfNeeded();
  await page.screenshot({ path: join(screenshots, 'folio-investigation.png') });

  await waitUntil(target(sceneEnds[1]));
  await page.getByText('Conflicting declared income', { exact: true }).scrollIntoViewIfNeeded();
  await page.screenshot({ path: join(screenshots, 'folio-review-signal.png') });

  await waitUntil(target(sceneEnds[2]));
  await page.getByRole('button', { name: /03_income_proof_altered/ }).click();
  await page.getByRole('heading', { name: 'Source trace' }).scrollIntoViewIfNeeded();
  await page.waitForTimeout(700);
  await page.screenshot({ path: join(screenshots, 'folio-source-trace.png') });

  await waitUntil(target(sceneEnds[3]));
  await page.getByRole('button', { name: 'Download dossier' }).scrollIntoViewIfNeeded();
  await waitUntil(target(sceneEnds[4]));
  await page.locator('#method').scrollIntoViewIfNeeded();
  await page.screenshot({ path: join(screenshots, 'folio-method-limits.png') });
  await waitUntil(PRE_ROLL + voiceDuration);

  const raw = await page.video().path();
  await context.close();
  await browser.close();

  run(ffmpeg, [
    '-y', '-i', raw, '-i', audio, '-vf', 'crop=1440:810:80:0,scale=1280:720,format=yuv420p',
    '-map', '0:v:0', '-map', '1:a:0', '-c:v', 'libx264', '-preset', 'medium', '-crf', '21', '-r', '30',
    '-c:a', 'aac', '-b:a', '160k', '-shortest', '-movflags', '+faststart', output,
  ], { stdio: 'inherit' });

  console.log(`Human-narrated video: ${output}`);
  console.log(`Voice duration: ${voiceDuration.toFixed(1)} seconds; video: ${(PRE_ROLL + voiceDuration).toFixed(1)} seconds.`);
  console.log(`Recorded UI: ${basename(raw)}`);
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
