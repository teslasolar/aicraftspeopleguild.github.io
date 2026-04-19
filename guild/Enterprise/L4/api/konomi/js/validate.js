import { esc } from './shell.js';

const SAMPLES = {
  'valid STD':    JSON.stringify({ id: 'MyStd', scope: 'demo', udt: [{ name: 'X', fields: [{ n: 'a', t: 'INT' }] }] }, null, 2),
  'valid UDT':    JSON.stringify({ name: 'Pressure', fields: [{ n: 'v', t: 'REAL' }, { n: 'q', t: 'Quality' }] }, null, 2),
  'missing name': JSON.stringify({ fields: [{ n: 'v', t: 'REAL' }] }, null, 2),
  'crosswalk':    JSON.stringify({ from_std: 'ISA-95', to_std: 'ISA-88', maps: [{ from: 'WorkCenter', to: 'ProcessCell', mapping: 'exact' }] }, null, 2),
};

// Kind inference is structural — we look at which fields are present
// rather than requiring a discriminator, because the paper's own
// examples don't use one either.
export function inferKind(obj) {
  if (!obj || typeof obj !== 'object')                       return null;
  if ('from_std' in obj && 'to_std' in obj && 'maps' in obj) return 'CROSSWALK';
  if ('states' in obj && 'transitions' in obj)               return 'STATE_MACHINE';
  if ('id' in obj && 'scope' in obj)                         return 'STD';
  if ('name' in obj && 'fields' in obj)                      return 'UDT';
  if ('name' in obj && 'udt' in obj)                         return 'ENTITY';
  if ('condition' in obj && 'action' in obj)                 return 'RULE';
  return 'UNKNOWN';
}

export function validateObj(obj, kind) {
  const errs = [];
  const req = {
    STD:           ['id'],
    UDT:           ['name'],
    CROSSWALK:     ['from_std', 'to_std', 'maps'],
    STATE_MACHINE: ['name', 'states', 'transitions'],
    ENTITY:        ['name', 'udt'],
    RULE:          ['id', 'condition', 'action'],
  };
  for (const k of req[kind] || []) if (!(k in obj)) errs.push(`missing required field: ${k}`);
  if (kind === 'UDT') {
    for (const [i, f] of (obj.fields || []).entries()) {
      if (!f.n && !f.name) errs.push(`fields[${i}]: missing n/name`);
      if (!f.t && !f.type) errs.push(`fields[${i}]: missing t/type`);
    }
  }
  if (kind === 'STATE_MACHINE') {
    const ss = new Set(obj.states || []);
    if (obj.initial && !ss.has(obj.initial)) errs.push(`initial state "${obj.initial}" not in states`);
    for (const [i, tr] of (obj.transitions || []).entries()) {
      if (tr.from !== '*' && !ss.has(tr.from)) errs.push(`transitions[${i}].from "${tr.from}" not in states`);
      if (!ss.has(tr.to)) errs.push(`transitions[${i}].to "${tr.to}" not in states`);
    }
  }
  if (kind === 'CROSSWALK') {
    for (const [i, m] of (obj.maps || []).entries()) {
      if (!m.from) errs.push(`maps[${i}]: missing from`);
      if (!m.to)   errs.push(`maps[${i}]: missing to`);
      if (m.mapping && !['exact', 'partial', 'semantic'].includes(m.mapping.toLowerCase()))
        errs.push(`maps[${i}].mapping "${m.mapping}" must be exact|partial|semantic`);
    }
  }
  return errs;
}

export const validate = {
  id: 'validate',
  label: 'validate',
  render() {
    return `
      <div class="validate-grid">
        <div class="validate-col">
          <h3>input</h3>
          <p style="font-size:12px;color:var(--dm);margin-bottom:8px">Paste a JSON document claiming to be an STD, UDT, CROSSWALK, ENTITY, or STATE_MACHINE. Inference is structural.</p>
          <div class="sample-picker">${Object.keys(SAMPLES).map(k => `<button data-s="${k}">${k}</button>`).join('')}</div>
          <textarea id="v-input" placeholder="paste JSON…"></textarea>
        </div>
        <div class="validate-col">
          <h3>result</h3>
          <div id="v-out"></div>
        </div>
      </div>`;
  },
  wire(_bundle, host) {
    const input = host.querySelector('#v-input');
    const out   = host.querySelector('#v-out');
    input.value = SAMPLES['valid UDT'];
    const run = () => {
      let obj; try { obj = JSON.parse(input.value); }
      catch (e) { out.innerHTML = `<div class="v-result bad">✗ parse error<br><small>${esc(e.message)}</small></div>`; return; }
      const kind = inferKind(obj);
      if (kind === 'UNKNOWN' || !kind) {
        out.innerHTML = `<div class="v-result bad">✗ couldn't infer kind — must match STD / UDT / CROSSWALK / STATE_MACHINE / ENTITY / RULE shape</div>`;
        return;
      }
      const errs = validateObj(obj, kind);
      if (errs.length === 0) {
        out.innerHTML = `<div class="v-result ok">✓ valid · <b>${kind}</b></div>`;
        return;
      }
      out.innerHTML = `<div class="v-result bad">✗ <b>${kind}</b> has ${errs.length} issue${errs.length > 1 ? 's' : ''}<ul class="v-errors">${errs.map(e => `<li>· ${esc(e)}</li>`).join('')}</ul></div>`;
    };
    input.addEventListener('input', run);
    host.querySelectorAll('.sample-picker button').forEach(b => {
      b.addEventListener('click', () => { input.value = SAMPLES[b.dataset.s]; run(); });
    });
    run();
  },
};
