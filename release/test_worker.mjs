import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const handlers = {}, stored = [], deleted = [], network = [];
const context = {
  URL,
  self: {location: {origin: 'https://pilot.taxdesk.test'}, addEventListener: (name, fn) => handlers[name] = fn,
    skipWaiting: async () => {}, clients: {claim: async () => {}}},
  caches: {open: async () => ({addAll: async names => stored.push(...names)}),
    keys: async () => ['taxdesk-public-offline-v0', 'unrelated-cache'],
    delete: async key => {deleted.push(key);}, match: async name => 'cached:' + name},
  fetch: async (request, options) => {network.push({request, options}); if (request.fail) throw Error('offline'); return 'network';}
};
vm.runInNewContext(fs.readFileSync(process.argv[2], 'utf8'), context);
let pending; handlers.install({waitUntil: promise => pending = promise}); await pending;
assert.deepEqual(stored.sort(), ['/offline.html', '/offline.css', '/icons/icon-192.png', '/icons/icon-512.png'].sort());
handlers.activate({waitUntil: promise => pending = promise}); await pending;
assert.deepEqual(deleted, ['taxdesk-public-offline-v0']);
for (const [method, url, mode] of [['GET', '/api/bootstrap', 'navigate'], ['POST', '/api/cases', 'navigate'],
    ['GET', 'https://another.test/', 'navigate'], ['GET', '/app.js', 'cors'], ['GET', '/security.js', 'cors']]) {
  let intercepted = false;
  handlers.fetch({request: {method, url: new URL(url, context.self.location.origin).href, mode}, respondWith: () => intercepted = true});
  assert.equal(intercepted, false, method + ' ' + url);
}
for (const fail of [false, true]) {
  handlers.fetch({request: {method: 'GET', url: context.self.location.origin + '/', mode: 'navigate', fail}, respondWith: promise => pending = promise});
  assert.equal(await pending, fail ? 'cached:/offline.html' : 'network');
  assert.equal(network.at(-1).options.cache, 'no-store');
}
console.log(JSON.stringify({worker_behavior_checks: 10, passed: true}));
