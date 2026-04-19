// Certainty Reversal Log
const KEY = 'acg-mini-certainty';
const load = () => JSON.parse(localStorage.getItem(KEY) || '[]');
const save = v  => localStorage.setItem(KEY, JSON.stringify(v));

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#f85149';
  let log = load();

  const draw = () => {
    const total = log.length;
    const reversed = log.filter(e => e.reversed).length;
    const pct = total === 0 ? 0 : Math.round((reversed * 100) / total);
    const fmt = t => new Date(t).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">CERTAINTY REVERSAL LOG</h3>
      <p style="margin:8px 0 14px">${pct}% of your absolute certainties got reversed (<b>${reversed} / ${total}</b>)</p>
      <button id="go" style="width:100%;padding:12px;background:${primary};color:#fff;border:0;border-radius:8px;font-weight:700;cursor:pointer;font-size:14px">I'm absolutely certain</button>
      <div style="border-top:1px solid var(--border);margin:18px 0"></div>
      <div style="font-family:var(--mono);font-size:11px">
        ${log.length ? log.map((e, i) => `
          <div style="display:flex;justify-content:space-between;align-items:center;padding:4px 0;border-bottom:1px solid var(--border)">
            <span style="color:${e.reversed ? '#f85149' : 'var(--text)'}">#${i+1} · ${fmt(e.ts)}</span>
            ${e.reversed
              ? '<span style="color:#f85149;font-weight:700">reversed</span>'
              : `<button data-i="${i}" class="rev" style="background:transparent;color:${primary};border:0;cursor:pointer;font-family:inherit;font-size:11px">was wrong</button>`}
          </div>`).reverse().join('') : '<p style="color:var(--dim);font-style:italic">log an absolute certainty to start</p>'}
      </div>
    `;
    root.querySelector('#go').onclick = () => {
      log.push({ ts: Date.now(), reversed: false }); save(log); draw();
    };
    root.querySelectorAll('.rev').forEach(b => b.onclick = () => {
      const i = +b.dataset.i; log[i].reversed = true; save(log); draw();
    });
  };
  draw();
}
