'use strict';
(() => {
  if ('serviceWorker' in navigator && window.isSecureContext) {
    navigator.serviceWorker.register('/sw.js', {scope: '/', updateViaCache: 'none'}).catch(() => {});
  }
  let prompt = null;
  const bar = document.getElementById('install-bar');
  window.addEventListener('beforeinstallprompt', event => {
    event.preventDefault(); prompt = event; bar.hidden = false;
  });
  document.getElementById('dismiss-install').addEventListener('click', () => {bar.hidden = true;});
  document.getElementById('install-app').addEventListener('click', async () => {
    if (!prompt) return;
    const current = prompt; prompt = null; bar.hidden = true;
    try {await current.prompt(); await current.userChoice;} catch (_) {}
  });
  window.addEventListener('appinstalled', () => {prompt = null; bar.hidden = true;});
})();
