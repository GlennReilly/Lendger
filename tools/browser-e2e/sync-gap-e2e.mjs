// Regression check: a founder saves their name while the first sync is between its push and its pull.
// The engine used to take pushed events out of the outbox before the pull had stored them, so for that moment the
// brand-new circle had no events at all and "Your name" answered "A steward needs to let you in" (about 1 run in 3 of
// e2e.mjs). Here the first pull is held for HOLD_MILLIS through the DevTools Fetch domain so the gap is always hit.
import { startServer, startChrome, sleep, waitUntil, markStep } from './lib.mjs';
const PASS = 'correct horse battery staple';
const HOLD_MILLIS = 3000;
const server = await startServer(18092);
const c = await startChrome(9350);
let failed = false;
const until = (expr, what, ms = 15000) => waitUntil(what, async () => await c.evaluate(expr) === true, { ms });
const submit = (formId, fields) => c.evaluate(`(() => { const f = document.getElementById(${JSON.stringify(formId)}); if (!f) return 'no form'; for (const [k, v] of Object.entries(${JSON.stringify(fields)})) { const e = f.elements.namedItem(k); e.value = v; e.dispatchEvent(new Event('input', { bubbles: true })); } f.requestSubmit(); return true; })()`);
try {
  let held = 0;
  await c.send('Fetch.enable', { patterns: [{ urlPattern: '*events*', requestStage: 'Request' }] });
  c.on(d => {
    if (d.method !== 'Fetch.requestPaused') return;
    const { requestId, request } = d.params;
    const isPull = request.method === 'GET' && request.url.includes('after=');
    if (isPull) held++;
    setTimeout(() => c.send('Fetch.continueRequest', { requestId }), isPull && held === 1 ? HOLD_MILLIS : 0);
  });
  await c.goto(server.url + '/');
  markStep('welcome page'); await until('document.querySelector(\'a[href="#/start"]\') !== null', 'welcome page');
  await c.evaluate('document.querySelector(\'a[href="#/start"]\').click()');
  markStep('start page'); await until('document.getElementById("start-form") !== null', 'start page');
  await submit('start-form', { passphrase: PASS });
  markStep('name page'); await until('document.getElementById("name-form") !== null', 'name page');
  const t0 = Date.now(); while (held === 0 && Date.now() - t0 < 10000) await sleep(20);
  if (held === 0) throw new Error('the first pull never started');
  await sleep(300); // the push has been answered; the pull is still held
  const sent = await submit('name-form', { 'display-name': 'Alice Smith' });
  if (sent !== true) throw new Error('name form gone during the sync: ' + sent);
  markStep('saving the name while the first pull is held');
  await until('document.getElementById("app").innerText.includes("Circle home")', 'the name saved while the first pull was held (Circle home)', 10000);
  console.log('ok: the founder saved a name while the first pull was on the wire');
} catch (e) {
  failed = true;
  console.log('FAIL: ' + e.message);
} finally {
  c.stop(); server.stop();
}
process.exit(failed ? 1 : 0);
