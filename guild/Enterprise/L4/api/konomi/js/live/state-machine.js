import { esc } from '../shell.js';

// One runtime per panel id — so the same state machine can have two
// instances open at once (e.g. ISA-88 in Konomi + in SCADA drawer).
const runtimes = {};

export function smPanel(title, id, sub, sm) {
  if (!sm) return `<div class="live-panel"><h3>${esc(title)}</h3><p class="sub">(not declared in the bundle)</p></div>`;
  const states = (sm.states || []).map(s =>
    `<span class="state-chip${s === sm.initial ? ' init' : ''}" data-sm="${id}" data-state="${esc(s)}">${esc(s)}</span>`,
  ).join('');
  const triggers = uniq((sm.transitions || []).map(t => t.trigger).filter(Boolean));
  const btns = triggers.map(tr =>
    `<button class="trig-btn" data-sm="${id}" data-trigger="${esc(tr)}">${esc(tr)}</button>`,
  ).join('');
  return `
    <div class="live-panel" id="${id}">
      <h3>${esc(title)} <span class="tag">live</span></h3>
      <p class="sub">${esc(sub)} · ${sm.states.length} states · ${sm.transitions.length} transitions</p>
      <div class="state-row" data-role="states-${id}">${states}</div>
      <div class="trig-row" data-role="triggers-${id}">${btns}</div>
      <div class="hist" data-role="hist-${id}"></div>
    </div>`;
}

export function wireSm(id, sm, host = document) {
  let cur = sm.initial || sm.states[0];
  runtimes[id] = { sm, get current() { return cur; }, history: [[cur, Date.now(), 'init']] };

  const paint = () => {
    host.querySelectorAll(`[data-sm="${id}"][data-state]`).forEach(el => {
      el.classList.toggle('current', el.dataset.state === cur);
    });
    const valid = new Set(sm.transitions.filter(t => t.from === cur || t.from === '*').map(t => t.trigger));
    host.querySelectorAll(`[data-sm="${id}"][data-trigger]`).forEach(el => {
      el.disabled = !valid.has(el.dataset.trigger);
    });
    const histEl = host.querySelector(`[data-role="hist-${id}"]`);
    histEl.innerHTML = runtimes[id].history.slice(-12).reverse().map(([s, t, tr]) =>
      `<div><span class="ts">${new Date(t).toLocaleTimeString()}</span> <span class="${tr === 'init' ? 'ok' : 'tx'}">${esc(tr)}</span> → <b>${esc(s)}</b></div>`,
    ).join('');
  };

  paint();
  host.querySelectorAll(`[data-sm="${id}"][data-trigger]`).forEach(btn => {
    btn.addEventListener('click', () => {
      const tr = btn.dataset.trigger;
      const hit = sm.transitions.find(t => t.trigger === tr && (t.from === cur || t.from === '*'));
      if (!hit) return;
      cur = hit.to;
      runtimes[id].history.push([cur, Date.now(), tr]);
      paint();
    });
  });
}

export function findSm(bundle, sid, name) {
  const s = bundle.standards[sid]; if (!s) return null;
  return (s.state_machines || []).find(sm => sm.name === name) || null;
}

function uniq(a) { return [...new Set(a)]; }
