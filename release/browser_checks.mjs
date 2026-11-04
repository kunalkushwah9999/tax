import fs from 'node:fs';
import {pathToFileURL} from 'node:url';
import assert from 'node:assert/strict';
const c = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const {chromium} = await import(pathToFileURL(c.playwright).href);
// The only origin used is our disposable local TLS fixture. No public service is visited.
const browser = await chromium.launch({executablePath: c.chromium, headless: true, args: ['--no-sandbox', '--ignore-certificate-errors']});
const context = await browser.newContext({viewport: {width: 1440, height: 1000}});
const page = await context.newPage(), checks = [], errors = [];
let casePosts = 0;
page.on('pageerror', e => errors.push(e.message));
page.on('request', r => {if (r.url() === c.base + '/api/cases' && r.method() === 'POST') casePosts++;});
async function check(name, fn) {await fn(); checks.push(name);}
async function idle() {await page.waitForFunction(() => typeof ui !== 'undefined' && !ui.busy && !ui.pending);}
try {
  await page.goto(c.base); await idle();
  await check('served manifest and actual Java health', async () => {
    const h = await page.evaluate(async () => (await fetch('/api/health')).json());
    assert.equal(h.version, '5.1.0'); assert.equal(h.production_ready, false);
    const m = await page.evaluate(async () => (await fetch('/manifest.webmanifest')).json());
    assert.equal(m.display, 'standalone'); assert.equal(m.start_url, '/');
    assert.equal(m.icons.length, 2);
  });
  await check('real service worker controls browser', async () => {
    await page.evaluate(() => navigator.serviceWorker.ready);
    await page.reload(); await idle();
    await page.waitForFunction(() => !!navigator.serviceWorker.controller);
  });
  await check('authenticated login through generated front end', async () => {
    await page.locator('#login-top').click();
    await page.locator('#auth-username').fill('release_owner');
    await page.locator('#auth-password').fill(c.password);
    const response = page.waitForResponse(r => r.url().endsWith('/api/login') && r.request().method() === 'POST');
    await page.locator('#auth-submit').click(); assert.equal((await response).status(), 200);
    await idle(); await page.locator('#app-shell').waitFor({state: 'visible'});
    const cookie = (await context.cookies()).find(x => x.name === '__Host-tax_startup_session');
    assert(cookie?.secure && cookie.httpOnly && cookie.sameSite === 'Strict');
  });
  await check('authenticated saved draft through UI', async () => {
    await page.locator('#new-case-top').click();
    await page.locator('#new-case-title').fill('Synthetic RELEASE PRIVATE CANARY');
    const response = page.waitForResponse(r => r.url().endsWith('/api/cases') && r.request().method() === 'POST');
    await page.locator('#create-form button[type=submit]').click(); assert.equal((await response).status(), 200);
    await idle(); assert.equal(casePosts, 1);
  });
  await check('authenticated 390px layout', async () => {
    await page.setViewportSize({width: 390, height: 844});
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    await page.screenshot({path: c.output + '/mobile.png', fullPage: true});
  });
  await check('service worker does not weaken CSRF', async () => {
    const status = await page.evaluate(async () => (await fetch('/api/cases', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-Token': 'wrong'}, body: JSON.stringify({title: 'Denied', request_id: 'wrong-csrf'})})).status);
    assert.equal(status, 403);
    assert.equal(casePosts, 2);
  });
  await check('Cache Storage contains only four public resources', async () => {
    const state = await page.evaluate(async () => {
      const result = [];
      for (const name of await caches.keys()) for (const req of await (await caches.open(name)).keys()) result.push(new URL(req.url).pathname);
      return result.sort();
    });
    assert.deepEqual(state, ['/icons/icon-192.png', '/icons/icon-512.png', '/offline.css', '/offline.html'].sort());
  });
  await check('signout completes online', async () => {
    const response = page.waitForResponse(r => r.url().endsWith('/api/logout') && r.request().method() === 'POST');
    await page.locator('#logout-top').click(); assert.equal((await response).status(), 200);
    await idle(); await page.locator('#marketing').waitFor({state: 'visible'});
  });
  await check('offline navigation shows generic page without private draft', async () => {
    await context.setOffline(true); await page.goto(c.base + '/');
    await page.getByRole('heading', {name: 'Reconnect to your workspace.'}).waitFor();
    assert(!(await page.locator('body').innerText()).includes('PRIVATE CANARY'));
    assert(await page.evaluate(async () => {try {await fetch('/api/cases'); return false;} catch (_) {return true;}}));
  });
  await check('reconnect does not replay mutations', async () => {
    await context.setOffline(false); await page.reload(); await idle();
    await page.locator('#marketing').waitFor({state: 'visible'}); assert.equal(casePosts, 2);
    assert.equal((await page.evaluate(async () => (await (await fetch('/api/bootstrap')).json()).user)), null);
    assert.deepEqual(errors, []);
  });
  let deletionKey;
  await check('navigation locks an old security form until refreshed data renders', async () => {
    await page.locator('#login-top').click();await page.locator('#auth-username').fill('release_owner');await page.locator('#auth-password').fill(c.password);await page.locator('#auth-submit').click();await page.locator('#app-shell').waitFor({state:'visible'});await idle();
    await page.locator('[data-page="security"]').click();await idle();
    let unlock,observed;const gate=new Promise(resolve=>unlock=resolve),seen=new Promise(resolve=>observed=resolve);
    await page.route(c.base+'/api/security',async route=>{observed();await gate;await route.continue();});
    await page.locator('[data-page="security"]').click();await seen;
    assert(await page.getByLabel('Current password',{exact:true}).first().isDisabled());
    unlock();await idle();await page.unroute(c.base+'/api/security');
  });
  await check('mobile customer can review active-data deletion', async () => {
    await page.locator('[data-page="privacy"]').click();await page.getByRole('heading',{name:'Save your deletion status key',exact:true}).waitFor();
    deletionKey=(await page.locator('pre.code').textContent()).trim();assert.equal(deletionKey.length,43);
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    await page.screenshot({path:c.output+'/privacy-mobile.png',fullPage:true});
  });
  await check('fresh proof removes active account through UI', async () => {
    await page.getByLabel('Current password',{exact:true}).fill(c.password);await page.getByLabel('Type DELETE MY ACCOUNT',{exact:true}).fill('DELETE MY ACCOUNT');
    await page.getByLabel('I saved my status key and understand that this removes active application data permanently.').check();
    const response=page.waitForResponse(r=>r.url().endsWith('/api/account-deletion/erase')&&r.request().method()==='POST');
    await page.getByRole('button',{name:'Delete my active account data',exact:true}).click();assert.equal((await response).status(),200);
    await page.getByRole('heading',{name:'Active application data removed',exact:true}).waitFor();
    assert.equal(await page.evaluate(async()=>(await fetch('/api/cases')).status),401);
  });
  await check('external deletion page checks secret receipt without sign-in', async () => {
    await page.goto(c.base+'/account-deletion');await page.getByLabel('Deletion status key',{exact:true}).fill(deletionKey);
    await page.getByRole('button',{name:'Check deletion result',exact:true}).click();
    await page.locator('#deletion-status-result').filter({hasText:'Active application data removed at'}).waitFor();
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));assert.deepEqual(errors,[]);
  });
  fs.writeFileSync(c.output + '/browser-result.json', JSON.stringify({browser_checks_passed: checks.length, checks, js_errors: errors, chromium_version: browser.version(), viewport: '1440x1000 and 390x844', expected_api_error: {path: '/api/cases', status: 403}}, null, 2));
  console.log('PASS ' + checks.length + ' browser checks');
} finally {await context.close(); await browser.close();}
