// Integrity Inspector
const SAMPLE = `fun compute(x: Int): Int {
    // main-path: halve until we hit 1
    var n = x
    while (n > 1) { n = n / 2 }
    return n
}`;
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#6f42c1';
  let code = SAMPLE;
  const measure = s => {
    const lines = s.split('\n'); const n = Math.max(1, lines.length);
    const long = lines.filter(l => l.length > 100).length;
    const todo = lines.filter(l => /TODO|FIXME|XXX/.test(l)).length;
    const ph   = lines.filter(l => /\.\.\.|pass\b|NotImplementedError/.test(l)).length;
    const cmt  = lines.filter(l => /^\s*(\/\/|#|\*)/.test(l)).length;
    const cpct = Math.round(cmt * 100 / n);
    const score = Math.max(0, Math.min(100, 100 - long*2 - todo*5 - ph*6 + (cpct >= 5 && cpct <= 20 ? 5 : 0)));
    return { n, long, todo, ph, cpct, score };
  };
  const draw = () => {
    const m = measure(code);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">INTEGRITY INSPECTOR</h3>
      <textarea id="c" style="width:100%;min-height:200px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:6px;padding:10px;font-family:var(--mono);font-size:11px;margin:10px 0">${escape(code)}</textarea>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:8px">
        ${stat('lines', m.n, primary)}
        ${stat('long',  m.long, primary)}
        ${stat('TODO',  m.todo, primary)}
        ${stat('placeholders', m.ph, primary)}
        ${stat('comments %',   m.cpct, primary)}
        ${stat('score', `${m.score}/100`, primary)}
      </div>
    `;
    root.querySelector('#c').oninput = e => { code = e.target.value; draw(); };
  };
  const stat = (label, value, primary) => `
    <div style="background:var(--bg);border:1px solid var(--border);border-radius:6px;padding:10px;text-align:center">
      <div style="font-family:var(--mono);font-size:18px;font-weight:700;color:${primary}">${value}</div>
      <div style="font-family:var(--mono);font-size:9px;letter-spacing:1.5px;color:var(--dim);text-transform:uppercase">${label}</div>
    </div>`;
  draw();
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
