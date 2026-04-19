import { esc } from './shell.js';

// Demo payload per crosswalk row — drives the "▶ run" button. Keys are
// from_std|from so we hit exactly the mapping the user clicked.
const CW_INPUTS = {
  'ISA-95|WorkCenter':      { id: 'AREA-01/CELL-03', capacity: 420, units: 'kg/h' },
  'ISA-95|WorkUnit':        { id: 'UNIT-07',         capability: 'mixing' },
  'ISA-95|ProcessSegment':  { segment: 'mix', materials_in: ['A', 'B'], materials_out: ['AB'] },
  'ISA-95|Equipment':       { id: 'PUMP-P101', capability: ['pump', 'cip'], state: 'RUNNING' },
  'ISA-95|Property':        { name: 'flow', value: 42.1, unit: 'L/min' },
  'ISA-95|Capability':      { name: 'pump', params: { setpoint: 'L/min' } },
  'ISA-88|Phase':           { name: 'Heat', state: 'RUNNING', setpoint: 72 },
  'ISA-88|UnitState':       { state: 'RUNNING', batch: 'B-2026-0419-001' },
  'ISA-101|AlarmIndicator': { tag: 'TT-305', severity: 'P1', color: '#CC0000' },
  'ISA-101|ColorMeaning':   { color: '#CC0000', meaning: 'alarm' },
  'OPC-UA|Variable':        { node: 'ns=2;s=flow', value: 42.1, quality: 192, ts: new Date().toISOString() },
  'OPC-UA|Subscription':    { monitored: ['ns=2;s=flow', 'ns=2;s=temp'], interval: 1000 },
  'OPC-UA|Method':          { name: 'restartPump', input: { reason: 'ack' } },
  'OPC-UA|AddressSpace':    { nodes: 2, root: 'Objects' },
};

// Very light transform DSL: "k=v" pairs separated by commas. Anything
// we don't recognize is left untouched.
export function applyCrosswalk(m, src) {
  const out = { ...src };
  if (m.transform) {
    const kv = String(m.transform).split(/\s*,\s*/).map(x => x.split('=')).filter(p => p.length === 2);
    for (const [k, v] of kv) out[k.trim()] = isNaN(+v) ? v.trim() : +v;
  }
  out._shape = m.to;
  return out;
}

export const crosswalks = {
  id: 'crosswalks',
  label: 'crosswalks',
  render(bundle) {
    return `<div class="cw-grid">` + bundle.crosswalks.map((c, ci) => {
      const rows = (c.maps || []).map((m, mi) => `
        <div class="cw-row">
          <span class="t">${esc(m.from)}</span>
          <span class="m ${esc((m.mapping || '').toLowerCase())}">${esc((m.mapping || '?').toUpperCase())}</span>
          <span class="t">${esc(m.to)}</span>
          <span class="tr">${esc(m.transform || '')}</span>
        </div>
        <div style="grid-column:1/-1">
          <button class="cw-run" data-ci="${ci}" data-mi="${mi}">▶ run · apply mapping</button>
          <div class="cw-result" data-res="${ci}-${mi}"></div>
        </div>`).join('');
      return `<div class="cw-card">
        <h3>${esc(c.from_std)} <span class="arrow">→</span> ${esc(c.to_std)}</h3>
        ${rows}
      </div>`;
    }).join('') + `</div>`;
  },
  wire(bundle, host) {
    host.querySelectorAll('.cw-run').forEach(b => {
      b.addEventListener('click', () => {
        const ci = +b.dataset.ci, mi = +b.dataset.mi;
        const cw = bundle.crosswalks[ci]; const mp = cw.maps[mi];
        const key = `${cw.from_std}|${mp.from}`;
        const src = CW_INPUTS[key];
        const res = host.querySelector(`[data-res="${ci}-${mi}"]`);
        if (!src) {
          res.className = 'cw-result bad';
          res.style.display = 'block';
          res.innerHTML = `no demo input registered for <b>${esc(key)}</b> — paper declares the mapping but this page has no sample payload yet`;
          return;
        }
        const out = applyCrosswalk(mp, src);
        res.className = 'cw-result';
        res.style.display = 'block';
        res.style.whiteSpace = 'pre-wrap';
        res.innerHTML =
          `<b>${esc(cw.from_std)}.${esc(mp.from)}</b> → <b>${esc(cw.to_std)}.${esc(mp.to)}</b>\n` +
          `in : ${esc(JSON.stringify(src))}\n` +
          `out: ${esc(JSON.stringify(out))}`;
      });
    });
  },
};
