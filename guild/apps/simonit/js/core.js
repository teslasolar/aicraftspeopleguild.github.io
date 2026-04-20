// ═══ Simonit · entry ═══
// Boots the shell: wait for Monaco, mount editor/tree/terminal/agent,
// wire the run-button + output-tab switcher + share-URL bookmarklet.
// Each panel is its own module so adding, say, a Pyodide runner is
// one file under runners/ plus one line in the registry.

import { state, bus, term } from './shell.js';
import { mountEditor, getValue, setValue } from './editor.js';
import { mountTree }    from './tree.js';
import { mountTerminal } from './terminal.js';
import { runners, run } from './runners/index.js';
import { mountAgent }   from './agent/index.js';

async function boot() {
  mountTerminal(document.getElementById('term'));
  await mountEditor(document.getElementById('editor'));
  mountTree(document.getElementById('tree'));
  mountAgent(document.getElementById('agent'));

  // Share URL — copy the file content as a fragment-encoded link.
  // Reading ?#code=... on boot replaces scratch.js so recipients
  // land on the same snippet without talking to a server.
  const hash = new URLSearchParams(location.hash.slice(1));
  if (hash.has('code')) {
    try { setValue(decodeURIComponent(atob(hash.get('code')))); } catch (e) {}
  }

  // Run button
  const runBtn  = document.getElementById('runBtn');
  const clrBtn  = document.getElementById('clearBtn');
  const shareBtn = document.getElementById('shareBtn');
  const runKind = document.getElementById('runKind');

  runBtn.addEventListener('click', () => {
    bus.emit('term:clear');
    const kind = runKind.value;
    state.runKind = kind;
    const r = run(kind, getValue());
    setOutputTab(r.previewsOutput ? 'preview' : 'term');
  });
  clrBtn.addEventListener('click', () => bus.emit('term:clear'));
  shareBtn.addEventListener('click', () => {
    const encoded = btoa(encodeURIComponent(getValue()));
    const url = location.origin + location.pathname + '#code=' + encoded;
    navigator.clipboard?.writeText(url).catch(() => {});
    term('📋 share url copied (' + url.length + ' chars)', 'ok');
  });

  // Output tabs
  document.querySelectorAll('.output-tabs button').forEach(b => {
    b.addEventListener('click', () => setOutputTab(b.dataset.out));
  });

  term(`Simonit ready · ${Object.keys(runners).length} runners loaded`, 'ok');
  term('tip: click ▶ run to execute the current file · cmd/ctrl-enter in the agent pane to send', 'dim');
}

function setOutputTab(which) {
  document.querySelectorAll('.output-tabs button').forEach(b =>
    b.classList.toggle('on', b.dataset.out === which));
  document.querySelector('.output-body').classList.toggle('preview-on', which === 'preview');
}

window.addEventListener('DOMContentLoaded', boot);
