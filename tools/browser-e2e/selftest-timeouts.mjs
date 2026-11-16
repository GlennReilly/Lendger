// Self-test of the time limits in lib.mjs (about 3 seconds, no Chrome, no server; run by hand, not part of CI):
//   node tools/browser-e2e/selftest-timeouts.mjs
// 1. a wait that never becomes true fails with the step name and what it waited for;
// 2. a run that outlives E2E_TIMEOUT_MIN prints the last step, runs its cleanups and exits non-zero.
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { existsSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { waitUntil, markStep } from './lib.mjs';

const fail = m => { console.error('SELFTEST FAILED: ' + m); process.exit(1); };

markStep('ok 7: the step before the stuck wait');
const t0 = Date.now();
try { await waitUntil('a flag that never turns true', () => false, { ms: 1000, every: 50 }); fail('the wait returned'); }
catch (e) {
  if (!e.message.includes('ok 7: the step before the stuck wait') || !e.message.includes('a flag that never turns true') || !e.message.includes('1 s')) fail('message lacks step or reason: ' + e.message);
  if (Date.now() - t0 > 3000) fail('took too long');
}
console.log('ok: a never-true wait fails after its limit, naming the step and what it waited for');

// The whole-run limit, in a child that registers a cleanup (a temp folder) and then hangs.
const lib = fileURLToPath(new URL('./lib.mjs', import.meta.url));
const marker = join(mkdtempSync(join(tmpdir(), 'e2e-selftest-')), 'marker');
const child = `import { markStep, onCleanup, sleep } from ${JSON.stringify(lib)}; import { writeFileSync, rmSync } from 'node:fs'; writeFileSync(${JSON.stringify(marker)}, 'x'); onCleanup(() => rmSync(${JSON.stringify(marker)})); markStep('ok 3: hanging on purpose'); await sleep(60000);`;
const r = spawnSync('node', ['--input-type=module', '-e', child], { env: { ...process.env, E2E_TIMEOUT_MIN: '0.02' }, encoding: 'utf8', timeout: 20000 });
if (r.status !== 1) fail('expected exit 1, got ' + r.status + ' ' + r.stderr);
if (!r.stderr.includes('ok 3: hanging on purpose')) fail('stderr lacks the last step: ' + r.stderr);
if (existsSync(marker)) fail('the cleanup did not run');
console.log('ok: a run past its limit prints the last step, cleans up and exits 1');
process.exit(0);
