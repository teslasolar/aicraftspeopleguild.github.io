// flywheel-tracker · web port. Mirrors the Kotlin MiniRegistry:
// one-tap cycle logger, streak counter, recent list. LocalStorage.

const KEY = 'acg-paper-mini-flywheel';
const load = () => (JSON.parse(localStorage.getItem(KEY) || '[]')).map(Number).sort((a,b) => a - b);
const save = cs => localStorage.setItem(KEY, JSON.stringify(cs));
const dayKey = t => { const d = new Date(t); return d.getFullYear()*400 + (() => { const s = new Date(d.getFullYear(),0,0); return Math.floor((d - s) / 86400000); })(); };
const isToday = t => dayKey(t) === dayKey(Date.now());

function streak(cs) {
  if (!cs.length) return 0;
  const days = new Set(cs.map(dayKey));
  const today = dayKey(Date.now());
  let cur = days.has(today) ? today : today - 1;
  let n = 0;
  while (days.has(cur)) { n++; cur -= 1; }
  return n;
}

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#1a5c4c';
  let cycles = load();
  const html = () => {
    const fmt = t => new Date(t).toLocaleString(undefined, { weekday: 'short', hour: '2-digit', minute: '2-digit' });
    return `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">FLYWHEEL TRACKER</h3>
      <p style="color:var(--dim);font-size:12px;margin:6px 0 18px">one cycle per trip around the loop · the paper's bet is that this is the only loop that grows</p>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px">
        ${card('total',  cycles.length, primary)}
        ${card('streak', streak(cycles), primary)}
        ${card('today',  cycles.filter(isToday).length, primary)}
      </div>
      <button id="log" style="margin:18px 0 8px;width:100%;padding:14px;background:${primary};color:#fff;border:0;border-radius:8px;font-weight:700;font-family:inherit;font-size:14px;cursor:pointer">+ log a cycle</button>
      <button id="reset" style="width:100%;padding:10px;background:transparent;color:var(--dim);border:1px solid var(--border);border-radius:8px;font-family:inherit;font-size:12px;cursor:pointer">reset</button>
      <h4 style="margin:18px 0 8px;font-family:var(--mono);color:${primary};font-size:10px;letter-spacing:2px;text-transform:uppercase;font-weight:700">RECENT</h4>
      <div id="list" style="font-family:var(--mono);font-size:11px;color:var(--text)">
        ${cycles.length ? cycles.slice(-40).reverse().map((t,i) => `
          <div style="display:flex;justify-content:space-between;padding:3px 0;border-bottom:1px solid var(--border)">
            <span style="color:var(--dim)">#${cycles.length - i}</span>
            <span>${fmt(t)}</span>
          </div>
        `).join('') : '<p style="color:var(--dim);font-style:italic">no cycles yet — the wheel starts here.</p>'}
      </div>
    `;
  };
  const card = (label, value, primary) => `
    <div style="background:var(--bg);border:1px solid var(--border);border-radius:6px;padding:12px;text-align:center">
      <div style="font-size:24px;font-weight:700;color:${primary};font-family:var(--mono)">${value}</div>
      <div style="font-family:var(--mono);font-size:9px;letter-spacing:1.5px;color:var(--dim);text-transform:uppercase">${label}</div>
    </div>`;
  const mount = () => {
    root.innerHTML = html();
    root.querySelector('#log').onclick   = () => { cycles = [...cycles, Date.now()].slice(-1000); save(cycles); mount(); };
    root.querySelector('#reset').onclick = () => { cycles = []; save(cycles); mount(); };
  };
  mount();
}
