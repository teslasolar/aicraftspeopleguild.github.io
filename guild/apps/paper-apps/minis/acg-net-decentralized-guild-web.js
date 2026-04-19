// Mesh Visualizer · full-mesh edges, SVG
const KEY = 'acg-mini-mesh';
const load = () => JSON.parse(localStorage.getItem(KEY) || '[]');
const save = v  => localStorage.setItem(KEY, JSON.stringify(v));

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#79c0ff';
  let peers = load();
  const draw = () => {
    const n = peers.length;
    const r = 140;
    const pts = Array.from({ length: n }, (_, i) => {
      const a = (i / (n || 1)) * 2 * Math.PI - Math.PI / 2;
      return { x: 180 + r * Math.cos(a), y: 180 + r * Math.sin(a) };
    });
    const edges = [];
    for (let i = 0; i < n; i++) for (let j = i+1; j < n; j++) edges.push([pts[i], pts[j]]);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">MESH</h3>
      <p style="color:var(--dim);font-size:12px;margin:6px 0">${n} node${n===1?'':'s'} · ${n*(n-1)/2} edge(s)</p>
      <svg viewBox="0 0 360 360" width="100%" style="background:var(--bg);border:1px solid var(--border);border-radius:8px">
        ${edges.map(([a, b]) => `<line x1="${a.x}" y1="${a.y}" x2="${b.x}" y2="${b.y}" stroke="${primary}" stroke-opacity="0.35" stroke-width="1.2"/>`).join('')}
        ${pts.map(p => `<circle cx="${p.x}" cy="${p.y}" r="12" fill="${primary}"/>`).join('')}
      </svg>
      <div style="display:flex;gap:8px;margin-top:12px">
        <input id="peer" placeholder="peer id" maxlength="12"
               style="flex:1;padding:8px;background:var(--bg);border:1px solid var(--border);border-radius:6px;color:var(--text);font-family:var(--mono)">
        <button id="add" style="padding:8px 16px;background:${primary};color:#fff;border:0;border-radius:6px;font-weight:700;cursor:pointer">+</button>
      </div>
      ${peers.length ? `<button id="reset" style="margin-top:8px;background:transparent;color:var(--dim);border:1px solid var(--border);border-radius:6px;padding:6px 12px;cursor:pointer;font-family:inherit">reset</button>` : ''}
    `;
    root.querySelector('#add').onclick = () => {
      const v = root.querySelector('#peer').value.trim();
      if (!v || peers.includes(v)) return;
      peers = [...peers, v].slice(-12); save(peers); draw();
    };
    const r2 = root.querySelector('#reset');
    if (r2) r2.onclick = () => { peers = []; save(peers); draw(); };
  };
  draw();
}
