// Constraint Ledger
const KEY = 'acg-mini-constraint';
const load = () => JSON.parse(localStorage.getItem(KEY) || '[]');
const save = v  => localStorage.setItem(KEY, JSON.stringify(v));

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#79c0ff';
  let items = load();
  const draw = () => {
    const met = items.filter(i => i.met).length;
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">CONSTRAINT LEDGER</h3>
      <p style="font-family:var(--mono);font-size:16px;font-weight:700;margin:6px 0">${met} / ${items.length} met</p>
      <div style="display:flex;gap:8px;margin:10px 0">
        <input id="c" placeholder="add constraint" maxlength="80"
               style="flex:1;padding:8px;background:var(--bg);border:1px solid var(--border);border-radius:6px;color:var(--text)">
        <button id="add" style="padding:8px 16px;background:${primary};color:#fff;border:0;border-radius:6px;cursor:pointer;font-weight:700">+</button>
      </div>
      <div style="border-top:1px solid var(--border);margin:10px 0"></div>
      ${items.length ? items.map((it,i) => `
        <div style="display:flex;align-items:center;gap:8px;padding:4px 0">
          <input type="checkbox" data-i="${i}" ${it.met?'checked':''} style="accent-color:${primary}">
          <span style="flex:1;font-size:13px;${it.met?'text-decoration:line-through;color:var(--dim)':''}">${escape(it.text)}</span>
          <button data-rm="${i}" style="background:transparent;color:#f85149;border:0;cursor:pointer">×</button>
        </div>`).join('') : '<p style="color:var(--dim);font-style:italic">none</p>'}
    `;
    root.querySelector('#add').onclick = () => {
      const t = root.querySelector('#c').value.trim(); if (!t) return;
      items = [...items, { ts: Date.now(), text: t, met: false }].slice(-80); save(items); draw();
    };
    root.querySelectorAll('input[type=checkbox]').forEach(el => el.onchange = () => {
      items[+el.dataset.i].met = el.checked; save(items); draw();
    });
    root.querySelectorAll('[data-rm]').forEach(el => el.onclick = () => {
      items.splice(+el.dataset.rm, 1); save(items); draw();
    });
  };
  draw();
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
