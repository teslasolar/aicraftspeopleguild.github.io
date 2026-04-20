// ═══ Simonit · HTML preview runner ═══
// Drops the user's HTML string into the preview iframe's srcdoc.
// Sandbox attribute on the iframe (allow-scripts + allow-modals +
// allow-same-origin) lets inline <script> run without granting the
// frame access to parent storage.

import { term } from '../shell.js';

export function runHtml(code) {
  const host = document.getElementById('preview');
  host.srcdoc = code;
  term(`▶ rendered ${code.length}-char HTML in preview tab`, 'info');
}
