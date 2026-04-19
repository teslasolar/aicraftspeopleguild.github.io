// Guild Coin ledger
const KEY = 'acg-mini-kcc';
const load = () => JSON.parse(localStorage.getItem(KEY) || '[]');
const save = v  => localStorage.setItem(KEY, JSON.stringify(v));

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#a371f7';
  let entries = load();
  const draw = () => {
    const bal = entries.reduce((s, e) => s + e.amount, 0);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">GUILD COIN LEDGER</h3>
      <p style="color:var(--dim);font-size:11px;margin:6px 0">balance</p>
      <div style="font-size:32px;font-weight:700;color:${primary};font-family:var(--mono)">${bal} KCC</div>
      <div style="display:flex;gap:8px;margin:12px 0">
        <input id="amt" type="number" placeholder="amount" style="flex:1;padding:8px;background:var(--bg);border:1px solid var(--border);border-radius:6px;color:var(--text);font-family:var(--mono)">
        <input id="memo" type="text" placeholder="memo" style="flex:2;padding:8px;background:var(--bg);border:1px solid var(--border);border-radius:6px;color:var(--text);font-family:var(--mono)">
      </div>
      <button id="post" style="width:100%;padding:12px;background:${primary};color:#fff;border:0;border-radius:8px;font-weight:700;cursor:pointer">post entry</button>
      <div style="border-top:1px solid var(--border);margin:18px 0"></div>
      <div style="font-family:var(--mono);font-size:11px">
        ${entries.length ? entries.slice().reverse().map(e => `
          <div style="display:flex;justify-content:space-between;padding:4px 0;border-bottom:1px solid var(--border)">
            <span>${escape(e.memo || '—')}</span>
            <b style="color:${e.amount >= 0 ? '#3fb950' : '#f85149'}">${e.amount >= 0 ? '+' : ''}${e.amount}</b>
          </div>`).join('') : '<p style="color:var(--dim);font-style:italic">no entries</p>'}
      </div>
    `;
    root.querySelector('#post').onclick = () => {
      const a = parseInt(root.querySelector('#amt').value, 10);
      if (!Number.isFinite(a)) return;
      const m = root.querySelector('#memo').value.slice(0, 40);
      entries = [...entries, { ts: Date.now(), amount: a, memo: m }].slice(-200);
      save(entries); draw();
    };
  };
  draw();
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
