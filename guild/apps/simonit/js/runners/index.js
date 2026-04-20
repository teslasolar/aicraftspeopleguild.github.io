// ═══ Simonit · runner registry ═══
// core.js dispatches "▶ run" through here. Each runner is its own
// module so future languages (Pyodide, WASM, sh) plug in cleanly.

import { runJs }   from './js.js';
import { runHtml } from './html.js';

export const runners = {
  js:   { label: 'JS sandbox',   run: runJs,   previewsOutput: false },
  html: { label: 'HTML preview', run: runHtml, previewsOutput: true  },
};

export function run(kind, code) {
  const r = runners[kind]; if (!r) throw new Error('unknown runner: ' + kind);
  r.run(code);
  return r;
}
