// Game of Life · 18×18
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#c47a20';
  const N = 18;
  let cells = Array.from({length:N}, () => Array(N).fill(false));
  let running = false, gen = 0, timer = null;
  const step = g => {
    const out = Array.from({length:N}, () => Array(N).fill(false));
    for (let r=0; r<N; r++) for (let c=0; c<N; c++) {
      let k=0; for (let dr=-1;dr<=1;dr++) for (let dc=-1;dc<=1;dc++) if (dr||dc) {
        const rr=(r+dr+N)%N, cc=(c+dc+N)%N; if (g[rr][cc]) k++;
      }
      out[r][c] = g[r][c] ? (k===2||k===3) : (k===3);
    }
    return out;
  };
  const alive = g => g.reduce((s,row) => s + row.filter(x=>x).length, 0);
  const draw = () => {
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">GRID BRAIN</h3>
      <p style="color:var(--dim);font-size:12px">gen ${gen} · tap to seed · play to tick · B3/S23</p>
      <div style="display:grid;grid-template-columns:repeat(${N},1fr);gap:1px;margin:12px 0;background:var(--border);border:1px solid var(--border);border-radius:4px;overflow:hidden">
        ${cells.map((row,r) => row.map((v,c) =>
          `<div data-r="${r}" data-c="${c}" style="aspect-ratio:1;background:${v ? primary : 'var(--bg)'};cursor:pointer"></div>`
        ).join('')).join('')}
      </div>
      <div style="display:flex;gap:8px">
        <button id="go" style="flex:1;padding:10px;background:${primary};color:#fff;border:0;border-radius:6px;font-weight:700;cursor:pointer">${running ? 'pause' : 'play'}</button>
        <button id="reset" style="flex:1;padding:10px;background:transparent;color:${primary};border:1px solid ${primary};border-radius:6px;cursor:pointer">reset</button>
      </div>
    `;
    root.querySelectorAll('[data-r]').forEach(el => el.onclick = () => {
      const r = +el.dataset.r, c = +el.dataset.c; cells[r][c] = !cells[r][c]; draw();
    });
    root.querySelector('#go').onclick = () => { running = !running; tick(); };
    root.querySelector('#reset').onclick = () => {
      running = false; if (timer) clearTimeout(timer);
      cells = Array.from({length:N}, () => Array(N).fill(false)); gen = 0; draw();
    };
  };
  const tick = () => {
    if (!running) { if (timer) clearTimeout(timer); draw(); return; }
    cells = step(cells); gen += 1;
    if (alive(cells) === 0) running = false;
    draw();
    if (running) timer = setTimeout(tick, 220);
  };
  draw();
}
