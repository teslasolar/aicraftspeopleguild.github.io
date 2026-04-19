// Mood Log · 30-day sparkline
const KEY = 'acg-mini-sad';
const load = () => JSON.parse(localStorage.getItem(KEY) || '[]');
const save = v  => localStorage.setItem(KEY, JSON.stringify(v));
const dayKey = t => { const d = new Date(t); return d.getFullYear()*400 + (() => { const s = new Date(d.getFullYear(),0,0); return Math.floor((d - s) / 86400000); })(); };

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#8d95a0';
  let entries = load();
  const draw = () => {
    const today = dayKey(Date.now());
    const todays = [...entries].reverse().find(e => e.day === today)?.score;
    const spark = Array.from({length:30}, (_,i) => {
      const d = today - (29 - i);
      const e = [...entries].reverse().find(x => x.day === d);
      return e ? e.score : -1;
    });
    const W = 360, H = 120;
    const dx = W / (spark.length - 1);
    let d = '', prev = null, dots = '';
    spark.forEach((s, i) => {
      if (s < 0) { prev = null; return; }
      const x = i * dx, y = H - (s/10) * H;
      if (prev) d += `L${x.toFixed(1)},${y.toFixed(1)}`;
      else       d += `M${x.toFixed(1)},${y.toFixed(1)}`;
      dots += `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="3" fill="${primary}"/>`;
      prev = { x, y };
    });
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">MOOD LOG · 30-DAY</h3>
      <p style="font-family:var(--mono);font-size:16px;margin:8px 0">today: <b>${todays ?? '—'}/10</b></p>
      <svg viewBox="0 0 ${W} ${H}" width="100%" style="background:var(--bg);border:1px solid var(--border);border-radius:6px;margin:4px 0">
        <path d="${d}" stroke="${primary}" stroke-width="2" fill="none"/>
        ${dots}
      </svg>
      <div style="display:grid;grid-template-columns:repeat(10,1fr);gap:4px;margin-top:8px">
        ${Array.from({length:10},(_,i) => `
          <button data-s="${i+1}" style="padding:8px 0;background:transparent;border:1px solid var(--border);border-radius:4px;color:${i+1===todays?primary:'var(--text)'};font-family:var(--mono);font-weight:${i+1===todays?700:400};cursor:pointer">${i+1}</button>
        `).join('')}
      </div>
      <p style="color:var(--dim);font-size:11px;margin-top:10px">1 = night · 10 = the good days</p>
    `;
    root.querySelectorAll('[data-s]').forEach(b => b.onclick = () => {
      entries = [...entries, { day: today, score: +b.dataset.s }].slice(-120); save(entries); draw();
    });
  };
  draw();
}
