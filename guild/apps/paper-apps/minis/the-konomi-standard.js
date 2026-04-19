// Konomi Validator
const SAMPLE = JSON.stringify({
  udtType: 'WhitepaperApp',
  instance: 'flywheel',
  parameters: { slug: 'flywheel', title: 'The Flywheel' },
}, null, 2);

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#4a8868';
  let text = SAMPLE;
  const draw = () => {
    const v = validate(text);
    const c = v.ok ? '#3fb950' : '#f85149';
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">KONOMI VALIDATOR</h3>
      <p style="color:var(--dim);font-size:12px">checks for { udtType, instance, parameters }</p>
      <textarea id="in" style="width:100%;min-height:220px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:6px;padding:10px;font-family:var(--mono);font-size:11px;margin:10px 0">${escape(text)}</textarea>
      <div style="border-top:1px solid var(--border);margin:12px 0"></div>
      <div style="color:${c};font-weight:700;font-size:15px">${v.ok ? '✓ valid UDT instance' : '✗ invalid'}</div>
      <pre style="margin-top:8px;color:var(--dim);font-family:var(--mono);font-size:11px;white-space:pre-wrap">${escape(v.msg)}</pre>
    `;
    root.querySelector('#in').oninput = e => { text = e.target.value; draw(); };
  };
  draw();
}
function validate(raw) {
  const t = raw.trim();
  if (!t) return { ok: false, msg: 'empty input' };
  let obj;
  try { obj = JSON.parse(t); } catch (e) { return { ok: false, msg: 'parse error: ' + e.message }; }
  const missing = ['udtType','instance','parameters'].filter(k => !(k in obj));
  if (missing.length) return { ok: false, msg: 'missing fields: ' + missing.join(', ') };
  return { ok: true, msg:
    `udtType = ${obj.udtType}\n` +
    `instance = ${obj.instance}\n` +
    `parameters = ${Object.keys(obj.parameters || {}).length} keys` };
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
