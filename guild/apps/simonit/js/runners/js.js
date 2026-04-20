// ═══ Simonit · JS runner ═══
// Spins a sandboxed <iframe> srcdoc with the user's code and a
// console proxy that postMessages every log line back. Sandbox
// attribute keeps user code from touching the parent page; the
// proxy captures uncaught errors too.

import { esc, term } from '../shell.js';

const IFRAME_HTML = code => `<!DOCTYPE html><html><body><script>
(function(){
  const send = (kind, args) => parent.postMessage({
    __simonit: true, kind,
    payload: args.map(a => {
      if (a instanceof Error) return a.stack || a.message;
      if (typeof a === 'object') try { return JSON.stringify(a); } catch { return String(a); }
      return String(a);
    }).join(' '),
  }, '*');
  const orig = { log:console.log, warn:console.warn, error:console.error, info:console.info };
  ['log','warn','error','info'].forEach(k => {
    console[k] = (...a) => { send(k, a); try{orig[k].apply(console,a)}catch(e){} };
  });
  window.addEventListener('error', e => {
    send('error', [e.message + ' · ' + (e.filename||'') + ':' + (e.lineno||'')]);
  });
  window.addEventListener('unhandledrejection', e => {
    send('error', ['unhandled rejection: ' + (e.reason?.stack || e.reason)]);
  });
  try {
    ${code}
  } catch (e) { send('error', [e.stack || e.message]); }
  send('done', []);
})();
<\/script></body></html>`;

let listener = null;

export function runJs(code) {
  const host = document.getElementById('preview');
  host.srcdoc = IFRAME_HTML(code);
  if (listener) window.removeEventListener('message', listener);
  listener = ev => {
    const m = ev.data;
    if (!m || !m.__simonit) return;
    if (m.kind === 'log')   term(esc(m.payload));
    if (m.kind === 'info')  term(esc(m.payload), 'info');
    if (m.kind === 'warn')  term(esc(m.payload), 'warn');
    if (m.kind === 'error') term(esc(m.payload), 'err');
    if (m.kind === 'done')  term('· done', 'dim');
  };
  window.addEventListener('message', listener);
  term(`▶ running ${code.length}-char JS in sandbox…`, 'info');
}
