// Sierpinski depth · SVG
function tri(a, b, c, depth, out) {
  if (depth <= 0) { out.push([a,b,c]); return; }
  const mid = (p,q) => ({ x:(p.x+q.x)/2, y:(p.y+q.y)/2 });
  const ab = mid(a,b), bc = mid(b,c), ca = mid(c,a);
  tri(a, ab, ca, depth-1, out);
  tri(ab, b, bc, depth-1, out);
  tri(ca, bc, c, depth-1, out);
}
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#a371f7';
  let depth = 4;
  const draw = () => {
    const tris = [];
    tri({x:180,y:10}, {x:10,y:340}, {x:350,y:340}, depth, tris);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">FRACTAL WRONGNESS</h3>
      <p style="color:var(--dim);font-size:12px">every level wrong the same way</p>
      <svg viewBox="0 0 360 360" width="100%" style="background:var(--bg);border:1px solid var(--border);border-radius:8px;margin:12px 0">
        ${tris.map(([a,b,c]) => `<polygon points="${a.x},${a.y} ${b.x},${b.y} ${c.x},${c.y}" fill="${primary}" opacity="0.85"/>`).join('')}
      </svg>
      <div style="font-family:var(--mono);font-size:12px">depth: <b>${depth}</b></div>
      <input id="d" type="range" min="1" max="7" step="1" value="${depth}" style="width:100%;accent-color:${primary}">
      <p style="color:var(--dim);font-size:11px;margin-top:8px">level 1 = only the outer mistake · level 7 = same mistake all the way down</p>
    `;
    root.querySelector('#d').oninput = e => { depth = +e.target.value; draw(); };
  };
  draw();
}
