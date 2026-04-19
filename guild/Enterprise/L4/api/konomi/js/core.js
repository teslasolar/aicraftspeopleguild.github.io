// ═══ Konomi browser · entry ═══
// Boots the page, fetches the API, and routes tab clicks to a panel
// registered in panels.js. Kept tiny on purpose — each panel is its
// own module, so adding a tab is one new file + one import.

import { state }  from './shell.js';
import { panels } from './panels.js';

const BUNDLE_URL = '../konomi.json';

async function boot() {
  const r = await fetch(BUNDLE_URL + '?t=' + Date.now());
  state.bundle = await r.json();

  const m = state.bundle._meta.counts;
  document.getElementById('sum').innerHTML =
    `<b>${m.standards}</b> standards · <b>${m.base_udts}</b> base · ` +
    `<b>${m.udts}</b> udts · <b>${m.state_machines}</b> sm · ` +
    `<b>${m.crosswalks}</b> crosswalks`;

  document.querySelectorAll('#tabs button').forEach(b => {
    b.addEventListener('click', () => {
      document.querySelectorAll('#tabs button').forEach(x => x.classList.remove('on'));
      b.classList.add('on');
      state.tab = b.dataset.t;
      render();
    });
  });
  render();
}

function render() {
  const host = document.getElementById('view');
  const panel = panels[state.tab];
  if (!panel) { host.innerHTML = `<p style="color:var(--ft)">no panel for "${state.tab}"</p>`; return; }
  host.innerHTML = panel.render(state.bundle);
  panel.wire?.(state.bundle, host);
}

boot();
