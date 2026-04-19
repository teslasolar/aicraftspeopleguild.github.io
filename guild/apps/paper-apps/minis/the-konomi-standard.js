// Konomi · reference API browser + validator + crosswalk
// Fetches guild/Enterprise/L4/api/konomi.json (same bundle the phone app uses).
const BUNDLE_URL = new URL('../../../Enterprise/L4/api/konomi.json', import.meta.url).href;
const BROWSER_URL = new URL('../../../Enterprise/L4/api/konomi/index.html', import.meta.url).href;

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#4a8868';
  const esc = s => String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  let tab = 'browse';
  let filter = '';
  let bundle = null;
  let loadErr = null;
  let cwFrom = '', cwEntity = '', cwTo = '';

  const SAMPLES = {
    'valid UDT':     JSON.stringify({name:"Pressure",fields:[{n:"v",t:"REAL"},{n:"q",t:"Quality"}]}, null, 2),
    'valid STD':     JSON.stringify({id:"MyStd",scope:"demo",udt:[{name:"X",fields:[{n:"a",t:"INT"}]}]}, null, 2),
    'missing name':  JSON.stringify({fields:[{n:"v",t:"REAL"}]}, null, 2),
    'crosswalk':     JSON.stringify({from_std:"ISA-95",to_std:"ISA-88",maps:[{from:"WorkCenter",to:"ProcessCell",mapping:"exact"}]}, null, 2),
  };
  let vInput = SAMPLES['valid UDT'];

  const inferKind = o => {
    if (!o || typeof o !== 'object') return null;
    if ('from_std' in o && 'to_std' in o && 'maps' in o) return 'CROSSWALK';
    if ('states' in o && 'transitions' in o) return 'STATE_MACHINE';
    if ('id' in o && 'scope' in o) return 'STD';
    if ('name' in o && 'fields' in o) return 'UDT';
    if ('name' in o && 'udt' in o) return 'ENTITY';
    if ('condition' in o && 'action' in o) return 'RULE';
    return 'UNKNOWN';
  };
  const validateObj = (obj, kind) => {
    const errs = [];
    const req = { STD:['id'], UDT:['name'], CROSSWALK:['from_std','to_std','maps'],
                  STATE_MACHINE:['name','states','transitions'], ENTITY:['name','udt'], RULE:['id','condition','action'] };
    for (const k of (req[kind]||[])) if (!(k in obj)) errs.push(`missing required field: ${k}`);
    if (kind === 'UDT') for (const [i,f] of (obj.fields||[]).entries()) {
      if (!f.n && !f.name) errs.push(`fields[${i}]: missing n/name`);
      if (!f.t && !f.type) errs.push(`fields[${i}]: missing t/type`);
    }
    if (kind === 'STATE_MACHINE') {
      const ss = new Set(obj.states||[]);
      if (obj.initial && !ss.has(obj.initial)) errs.push(`initial state "${obj.initial}" not in states`);
      for (const [i,tr] of (obj.transitions||[]).entries()) {
        if (tr.from !== '*' && !ss.has(tr.from)) errs.push(`transitions[${i}].from "${tr.from}" not in states`);
        if (!ss.has(tr.to)) errs.push(`transitions[${i}].to "${tr.to}" not in states`);
      }
    }
    if (kind === 'CROSSWALK') for (const [i,m] of (obj.maps||[]).entries()) {
      if (!m.from) errs.push(`maps[${i}]: missing from`);
      if (!m.to)   errs.push(`maps[${i}]: missing to`);
      if (m.mapping && !['exact','partial','semantic'].includes(m.mapping.toLowerCase()))
        errs.push(`maps[${i}].mapping "${m.mapping}" must be exact|partial|semantic`);
    }
    return errs;
  };

  try {
    const r = await fetch(BUNDLE_URL + '?t=' + Date.now());
    if (!r.ok) throw new Error('HTTP ' + r.status);
    bundle = await r.json();
  } catch (e) { loadErr = e.message; }

  const head = `
    <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">◈ KONOMI · REFERENCE</h3>
    <p style="font-size:12px;color:var(--dim);margin:4px 0 8px">Live API at <a href="${BROWSER_URL}" target="_blank" style="color:${primary};text-decoration:none">L4/api/konomi/</a> · same JSON served to the phone app.</p>`;

  const tabBar = () => `
    <div style="display:flex;gap:4px;border-bottom:1px solid var(--border);margin-bottom:10px">
      ${['browse','crosswalk','validate'].map(t => `
        <button data-tab="${t}" style="background:transparent;border:0;border-bottom:2px solid ${tab===t?primary:'transparent'};padding:6px 10px;font-family:var(--mono);font-size:10px;letter-spacing:.8px;text-transform:uppercase;color:${tab===t?primary:'var(--dim)'};cursor:pointer;font-weight:700">${t}</button>`).join('')}
    </div>`;

  const drawLoadErr = () => `<p style="color:#f85149;font-size:12px;font-family:var(--mono)">could not load bundle: ${esc(loadErr)}<br><span style="color:var(--dim)">expected at ${esc(BUNDLE_URL)}</span></p>`;

  const drawBrowse = () => {
    if (!bundle) return drawLoadErr();
    const m = bundle._meta;
    const stds = Object.entries(bundle.standards);
    const q = filter.toLowerCase();
    const matches = (txt) => !q || String(txt||'').toLowerCase().includes(q);
    const kpi = (l,v) => `<div style="padding:6px 10px;background:var(--bg);border:1px solid var(--border);border-radius:4px;min-width:80px"><div style="font-family:var(--mono);font-size:9px;letter-spacing:1px;text-transform:uppercase;color:var(--dim)">${l}</div><div style="font-family:var(--mono);font-size:16px;font-weight:700;color:${primary}">${v}</div></div>`;
    const baseList = Object.entries(bundle.base).filter(([n,u]) => matches(n)||matches(u.meta?.desc)).map(([n,u]) =>
      `<div style="padding:6px 0;border-bottom:1px dashed var(--border);font-family:var(--mono);font-size:11px"><b style="color:var(--text)">${esc(n)}</b> <span style="color:var(--dim)">· ${(u.fields||[]).slice(0,4).map(f=>esc(f.n||f.name)).join(' · ')}</span></div>`).join('');
    const stdList = stds.map(([sid,s]) => {
      const udts = (s.udts||[]).filter(u => matches(u.name) || matches(u.meta?.desc));
      if (q && udts.length === 0 && !matches(s._std?.id) && !matches(s._std?.scope)) return '';
      const rows = udts.slice(0,10).map(u => `<div style="padding:3px 0;font-family:var(--mono);font-size:10.5px;color:var(--dim)">· <b style="color:var(--text)">${esc(u.name)}</b>${u.meta?.desc?' — '+esc(u.meta.desc):''}</div>`).join('');
      return `<details style="background:var(--bg);border:1px solid var(--border);border-radius:4px;margin:6px 0;padding:8px 10px">
        <summary style="cursor:pointer;font-family:var(--mono);font-size:12px;font-weight:700;color:${primary}">${esc(s._std?.id || sid)} <span style="font-weight:400;color:var(--dim);font-size:11px">· ${esc(s._std?.scope || '')}</span> <span style="float:right;font-weight:400;color:var(--dim);font-size:10px">${s.udts.length} udts</span></summary>
        <div style="margin-top:6px">${rows}${udts.length>10?`<div style="color:var(--dim);font-size:10px;font-style:italic;margin-top:4px">… +${udts.length-10} more</div>`:''}</div>
      </details>`;
    }).join('');
    return `
      <div style="display:flex;gap:6px;flex-wrap:wrap;margin-bottom:10px">
        ${kpi('standards',m.counts.standards)}${kpi('base udts',m.counts.base_udts)}${kpi('std udts',m.counts.udts)}${kpi('state mach',m.counts.state_machines)}${kpi('crosswalks',m.counts.crosswalks)}
      </div>
      <input id="q" value="${esc(filter)}" placeholder="filter UDTs / standards…" style="width:100%;padding:7px 10px;background:var(--bg);border:1px solid var(--border);border-radius:4px;font-family:var(--mono);font-size:11px;color:var(--text);outline:none;margin-bottom:10px">
      <h4 style="font-family:var(--mono);color:var(--dim);font-size:10px;letter-spacing:1.5px;text-transform:uppercase;margin:10px 0 6px">Layer 1 · base</h4>
      ${baseList || '<div style="color:var(--dim);font-size:11px">(no match)</div>'}
      <h4 style="font-family:var(--mono);color:var(--dim);font-size:10px;letter-spacing:1.5px;text-transform:uppercase;margin:12px 0 4px">Layers 2–8 · standards</h4>
      ${stdList || '<div style="color:var(--dim);font-size:11px">(no match)</div>'}`;
  };

  const drawCrosswalk = () => {
    if (!bundle) return drawLoadErr();
    const stds = Object.entries(bundle.standards).map(([sid,s])=>[s._std?.id||sid, s]);
    const from = stds.find(([id]) => id === cwFrom);
    const fromUdts = from ? (from[1].udts||[]).map(u=>u.name) : [];
    const maps = [];
    for (const cw of (bundle.crosswalks||[])) {
      for (const m of (cw.maps||[])) maps.push({ from_std: cw.from_std, to_std: cw.to_std, ...m });
    }
    const hits = maps.filter(m =>
      (!cwFrom   || m.from_std === cwFrom) &&
      (!cwTo     || m.to_std   === cwTo)   &&
      (!cwEntity || m.from     === cwEntity)
    );
    const mc = m => ({exact:'#3fb950',partial:'#e3b341',semantic:primary}[String(m.mapping||'').toLowerCase()] || 'var(--dim)');
    const extraToStds = ['PackML','Sparkplug','OPC-UA'].filter(x=>!stds.some(([id])=>id===x));
    return `
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:6px;margin-bottom:10px">
        <select id="cw-from" style="padding:6px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:4px;font-family:var(--mono);font-size:11px">
          <option value="">from std…</option>${stds.map(([id]) => `<option${cwFrom===id?' selected':''}>${esc(id)}</option>`).join('')}
        </select>
        <select id="cw-entity" style="padding:6px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:4px;font-family:var(--mono);font-size:11px">
          <option value="">any entity</option>${fromUdts.map(n=>`<option${cwEntity===n?' selected':''}>${esc(n)}</option>`).join('')}
        </select>
        <select id="cw-to" style="padding:6px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:4px;font-family:var(--mono);font-size:11px">
          <option value="">to std…</option>${stds.map(([id]) => `<option${cwTo===id?' selected':''}>${esc(id)}</option>`).join('')}${extraToStds.map(x=>`<option${cwTo===x?' selected':''}>${esc(x)}</option>`).join('')}
        </select>
      </div>
      <div style="font-family:var(--mono);font-size:11px;color:var(--dim);margin-bottom:6px">${hits.length} match${hits.length===1?'':'es'}</div>
      ${hits.map(m => `
        <div style="display:grid;grid-template-columns:1fr auto 1fr auto;gap:8px;align-items:center;padding:6px 0;border-bottom:1px dashed var(--border);font-family:var(--mono);font-size:11px">
          <span><b style="color:var(--text)">${esc(m.from_std)}</b>.${esc(m.from)}</span>
          <span style="font-size:9px;padding:2px 6px;border-radius:8px;background:${mc(m)}22;color:${mc(m)};font-weight:700;text-transform:uppercase">${esc(m.mapping||'?')}</span>
          <span><b style="color:var(--text)">${esc(m.to_std)}</b>.${esc(m.to)}</span>
          <span style="color:var(--dim);font-size:10px;font-style:italic">${esc(m.transform||'')}</span>
        </div>`).join('') || '<div style="color:var(--dim);font-size:11px;padding:20px;text-align:center">no crosswalk matches that filter</div>'}`;
  };

  const drawValidate = () => {
    let obj, kind = null, errs = [], parseErr = null;
    try { obj = JSON.parse(vInput); kind = inferKind(obj); errs = kind && kind !== 'UNKNOWN' ? validateObj(obj, kind) : []; }
    catch (e) { parseErr = e.message; }
    const ok = !parseErr && kind && kind !== 'UNKNOWN' && errs.length === 0;
    const result = parseErr ? `<div style="padding:10px;background:#f8514920;border:1px solid #f85149;border-radius:4px;color:#f85149;font-family:var(--mono);font-size:11px">✗ parse error: ${esc(parseErr)}</div>`
      : (kind === 'UNKNOWN' || !kind) ? `<div style="padding:10px;background:#f8514920;border:1px solid #f85149;border-radius:4px;color:#f85149;font-family:var(--mono);font-size:11px">✗ couldn't infer kind — must be STD / UDT / CROSSWALK / STATE_MACHINE / ENTITY / RULE</div>`
      : ok ? `<div style="padding:10px;background:#3fb95020;border:1px solid #3fb950;border-radius:4px;color:#3fb950;font-family:var(--mono);font-size:11px">✓ valid · <b>${kind}</b></div>`
      : `<div style="padding:10px;background:#f8514920;border:1px solid #f85149;border-radius:4px;color:#f85149;font-family:var(--mono);font-size:11px">✗ <b>${kind}</b> has ${errs.length} issue${errs.length>1?'s':''}<ul style="list-style:none;padding:0;margin:6px 0 0">${errs.map(e=>`<li style="font-size:10.5px;padding:1px 0">· ${esc(e)}</li>`).join('')}</ul></div>`;
    return `
      <p style="font-size:11px;color:var(--dim);margin-bottom:8px">Paste a JSON doc; kind is inferred structurally and checked against Konomi Layer-0 rules.</p>
      <div style="display:flex;gap:4px;flex-wrap:wrap;margin-bottom:6px">
        ${Object.keys(SAMPLES).map(k => `<button data-sample="${k}" style="padding:3px 8px;font-family:var(--mono);font-size:10px;background:var(--bg);border:1px solid var(--border);border-radius:4px;color:var(--dim);cursor:pointer">${esc(k)}</button>`).join('')}
      </div>
      <textarea id="v-in" style="width:100%;min-height:180px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:4px;padding:8px 10px;font-family:var(--mono);font-size:10.5px;outline:none;resize:vertical">${esc(vInput)}</textarea>
      <div style="margin-top:8px">${result}</div>`;
  };

  const draw = () => {
    let content = '';
    if (tab === 'browse')    content = drawBrowse();
    if (tab === 'crosswalk') content = drawCrosswalk();
    if (tab === 'validate')  content = drawValidate();
    root.innerHTML = head + tabBar() + content;

    root.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => { tab = b.dataset.tab; draw(); });
    const q = root.querySelector('#q'); if (q) q.oninput = e => { filter = e.target.value; draw(); };
    const cf = root.querySelector('#cw-from');   if (cf) cf.onchange = e => { cwFrom = e.target.value; cwEntity=''; draw(); };
    const ce = root.querySelector('#cw-entity'); if (ce) ce.onchange = e => { cwEntity = e.target.value; draw(); };
    const ct = root.querySelector('#cw-to');     if (ct) ct.onchange = e => { cwTo = e.target.value; draw(); };
    const vi = root.querySelector('#v-in'); if (vi) vi.oninput = e => { vInput = e.target.value; draw(); };
    root.querySelectorAll('[data-sample]').forEach(b => b.onclick = () => { vInput = SAMPLES[b.dataset.sample]; draw(); });
  };
  draw();
}
