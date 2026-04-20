// ═══ Simonit · terminal pane ═══
// Subscribes to the shell bus and renders colored output lines into
// the #term <pre>. Runners emit term:line with { html, cls }; the
// agent emits here too when narrating what it's about to do.

import { bus } from './shell.js';

export function mountTerminal(host) {
  bus.on('term:line', ({ html, cls }) => {
    const el = document.createElement('div');
    if (cls) el.className = cls;
    el.innerHTML = html;
    host.appendChild(el);
    host.scrollTop = host.scrollHeight;
  });
  bus.on('term:clear', () => { host.innerHTML = ''; });
}
