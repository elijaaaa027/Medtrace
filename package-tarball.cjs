#!/usr/bin/env node
/**
 * package-tarball.cjs
 * Creates a valid, non-empty, standard POSIX tar+gzip archive (app.tar.gz)
 * for deploying to Google Cloud Run via `gcloud run deploy --source=app.tar.gz`.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const ROOT_DIR = __dirname;
const TARBALL_NAME = 'app.tar.gz';
const TARBALL_PATH = path.join(ROOT_DIR, TARBALL_NAME);
const TEMP_TARBALL = path.join('/tmp', TARBALL_NAME);

console.log('[MedTrace Packager] Preparing source archive for Cloud Run deployment...');

// 1. Clean intermediate build caches and legacy artifacts
const cleanDirs = [
  path.join(ROOT_DIR, 'app', 'build'),
  path.join(ROOT_DIR, 'build'),
  path.join(ROOT_DIR, '.gradle'),
  path.join(ROOT_DIR, '.kotlin'),
  path.join(ROOT_DIR, '.build-outputs')
];

for (const d of cleanDirs) {
  if (fs.existsSync(d)) {
    try {
      fs.rmSync(d, { recursive: true, force: true });
      console.log(`[MedTrace Packager] Cleaned temporary directory: ${path.relative(ROOT_DIR, d)}`);
    } catch (e) {
      console.warn(`[MedTrace Packager] Could not clean ${d}:`, e.message);
    }
  }
}

// 2. Validate critical production files exist
const criticalFiles = [
  'Dockerfile',
  'package.json',
  'server.cjs',
  'dev-server.cjs',
  'index.html',
  'manifest.json',
  'sw.js'
];

for (const f of criticalFiles) {
  const p = path.join(ROOT_DIR, f);
  if (!fs.existsSync(p)) {
    console.error(`[MedTrace Packager] Error: Critical deployment file missing: ${f}`);
    process.exit(1);
  }
}
console.log('[MedTrace Packager] Verified critical runtime files exist.');

// Remove existing archives if present
if (fs.existsSync(TARBALL_PATH)) fs.unlinkSync(TARBALL_PATH);
if (fs.existsSync(TEMP_TARBALL)) fs.unlinkSync(TEMP_TARBALL);

// 3. Build standard POSIX tar+gzip archive into /tmp first (prevents "file changed as we read it")
const excludePatterns = [
  '--exclude=./node_modules',
  '--exclude=./.git',
  '--exclude=./.gradle',
  '--exclude=./build',
  '--exclude=./app/build',
  '--exclude=./.kotlin',
  '--exclude=./.build-outputs',
  '--exclude=./*.keystore',
  '--exclude=./*.keystore.base64',
  '--exclude=./.dev.env.json',
  '--exclude=./*.log',
  `--exclude=./${TARBALL_NAME}`
];

const tarCommand = `tar --warning=no-file-changed --format=ustar ${excludePatterns.join(' ')} -czf "${TEMP_TARBALL}" .`;
console.log(`[MedTrace Packager] Packaging archive to: ${TEMP_TARBALL}`);

try {
  execSync(tarCommand, { cwd: ROOT_DIR, stdio: 'inherit' });
  fs.copyFileSync(TEMP_TARBALL, TARBALL_PATH);
} catch (err) {
  console.error('[MedTrace Packager] Failed to create tar.gz archive:', err);
  process.exit(1);
}

// 4. Verify archive integrity & ensure it is non-empty
if (!fs.existsSync(TARBALL_PATH)) {
  console.error('[MedTrace Packager] Error: app.tar.gz was not created!');
  process.exit(1);
}

const stats = fs.statSync(TARBALL_PATH);
if (stats.size === 0) {
  console.error('[MedTrace Packager] Error: app.tar.gz is empty (0 bytes)!');
  process.exit(1);
}

try {
  const listOutput = execSync(`tar -tzf "${TARBALL_NAME}"`, { cwd: ROOT_DIR, encoding: 'utf8' });
  const entries = listOutput.trim().split(/\r?\n/).filter(Boolean);
  console.log(`[MedTrace Packager] Verification succeeded!`);
  console.log(`  - Archive: ${TARBALL_NAME}`);
  console.log(`  - Size: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);
  console.log(`  - Files bundled: ${entries.length}`);
  console.log(`  - Format: POSIX ustar (tar+gzip)`);
  console.log(`  - Entry point: Dockerfile + server.cjs (port 8080)`);
} catch (testErr) {
  console.error('[MedTrace Packager] Verification failed for app.tar.gz:', testErr);
  process.exit(1);
}

console.log('[MedTrace Packager] Done! Ready for `gcloud run deploy`.');
