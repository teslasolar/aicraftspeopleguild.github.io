// Pareidolia game · SVG
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#c47a20';
  let seed = Date.now(), marked = new Set(), rounds = 0, totalMarks = 0;
  const rand = s => { const x = Math.sin(s) * 10000; return x - Math.floor(x); };
  const mkPts = (seed) => Array.from({length:60}, (_,i) => ({ x: rand(seed + i*2)*340 + 10, y: rand(seed + i*2 + 1)*340 + 10 }));
  const draw = () => {
    const pts = mkPts(seed);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">THE PATTERN THAT WASN'T THERE</h3>
      <p style="color:var(--dim);font-size:12px">tap dots that form a shape · they don't</p>
      <svg viewBox="0 0 360 360" width="100%" style="background:var(--bg);border:1px solid var(--border);border-radius:8px;margin:12px 0" id="svg">
        ${pts.map((p, i) => `<circle cx="${p.x}" cy="${p.y}" r="${marked.has(i)?10:5}"
          fill="${marked.has(i) ? primary : '#8b949e'}" data-i="${i}" style="cursor:pointer"/>`).join('')}
      </svg>
      <div style="display:flex;justify-content:space-between;font-family:var(--mono);font-size:12px;margin:8px 0">
        <span>rounds played: ${rounds}</span>
        <span style="color:${totalMarks?'#f85149':'var(--text)'}">patterns found: ${totalMarks}</span>
      </div>
      <button id="go" style="width:100%;padding:10px;background:${primary};color:#fff;border:0;border-radius:6px;font-weight:700;cursor:pointer">new grid</button>
      <p style="color:var(--dim);font-size:11px;margin-top:8px">if that number isn't 0, that's you adding the pattern</p>
    `;
    root.querySelectorAll('circle').forEach(el => el.onclick = () => {
      const i = +el.dataset.i;
      if (marked.has(i)) marked.delete(i); else marked.add(i);
      draw();
    });
    root.querySelector('#go').onclick = () => {
      totalMarks += marked.size; rounds += 1; marked.clear(); seed = Date.now() + rounds;
      draw();
    };
  };
  draw();
}
