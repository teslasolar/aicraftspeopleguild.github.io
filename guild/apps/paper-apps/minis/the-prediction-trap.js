// Calibration Logger
const KEY = 'acg-mini-calib';
const load = () => JSON.parse(localStorage.getItem(KEY) || '[]');
const save = v  => localStorage.setItem(KEY, JSON.stringify(v));

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#e3b341';
  let preds = load();
  let prob = 50;

  const draw = () => {
    const avg = preds.length ? Math.round(preds.reduce((s,p) => s+p.prob, 0) / preds.length) : 0;
    const happ = preds.filter(p => p.outcome === true);
    const miss = preds.filter(p => p.outcome === false);
    const hAvg = happ.length ? Math.round(happ.reduce((s,p)=>s+p.prob,0)/happ.length) : null;
    const mAvg = miss.length ? Math.round(miss.reduce((s,p)=>s+p.prob,0)/miss.length) : null;
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">CALIBRATION LOG</h3>
      <p style="font-family:var(--mono);font-size:12px;margin:8px 0">
        happened avg: <b>${hAvg !== null ? hAvg+'%' : '—'}</b>
        missed avg: <b>${mAvg !== null ? mAvg+'%' : '—'}</b>
      </p>
      <input id="txt" placeholder="prediction" maxlength="60" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--border);border-radius:6px;color:var(--text);margin:4px 0">
      <div style="font-family:var(--mono);font-size:12px">confidence: <b>${prob}%</b></div>
      <input id="p" type="range" min="0" max="100" value="${prob}" style="width:100%;accent-color:${primary}">
      <button id="go" style="width:100%;padding:10px;background:${primary};color:#fff;border:0;border-radius:6px;font-weight:700;cursor:pointer;margin-top:8px">log prediction</button>
      <div style="border-top:1px solid var(--border);margin:14px 0"></div>
      <div style="font-family:var(--mono);font-size:11px">
        ${preds.length ? preds.slice().reverse().map((p, i) => {
          const idx = preds.length - 1 - i;
          const color = p.outcome === true ? '#3fb950' : p.outcome === false ? '#f85149' : 'var(--text)';
          return `
          <div style="display:flex;align-items:center;gap:8px;padding:4px 0;border-bottom:1px solid var(--border)">
            <b style="color:${primary};min-width:38px">${p.prob}%</b>
            <span style="flex:1;color:${color}">${escape(p.text)}</span>
            ${p.outcome === undefined ? `
              <button class="ok" data-i="${idx}" style="background:transparent;color:#3fb950;border:0;cursor:pointer;font-size:13px">✓</button>
              <button class="no" data-i="${idx}" style="background:transparent;color:#f85149;border:0;cursor:pointer;font-size:13px">✗</button>` : ''}
          </div>`;
        }).join('') : '<p style="color:var(--dim);font-style:italic">no predictions yet</p>'}
      </div>
    `;
    root.querySelector('#p').oninput = e => { prob = +e.target.value; draw(); };
    root.querySelector('#go').onclick = () => {
      const t = root.querySelector('#txt').value.trim(); if (!t) return;
      preds = [...preds, { ts: Date.now(), text: t, prob }].slice(-60); save(preds); draw();
    };
    root.querySelectorAll('.ok').forEach(b => b.onclick = () => { preds[+b.dataset.i].outcome = true;  save(preds); draw(); });
    root.querySelectorAll('.no').forEach(b => b.onclick = () => { preds[+b.dataset.i].outcome = false; save(preds); draw(); });
  };
  draw();
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
