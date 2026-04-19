// URL Auditor (offline)
function audit(raw) {
  const out = [];
  const s = raw.trim();
  if (!s) return [{ tag: '—', msg: '(empty)', color: 'var(--dim)' }];
  const ok = '#3fb950', warn = '#e3b341', bad = '#f85149';
  if (!s.startsWith('https://')) out.push({ tag: 'WARN', msg: 'not https — downstream UA may refuse', color: warn });
  else                           out.push({ tag: 'OK', msg: 'https', color: ok });
  const m = s.match(/^https?:\/\/([^/]+)(\/[^?#]*)?(\?[^#]*)?(#.*)?$/);
  if (!m) { out.push({ tag: 'FAIL', msg: 'malformed URL', color: bad }); return out; }
  const [, host, path = '', query = ''] = m;
  out.push({ tag: 'HOST', msg: host, color: ok });
  if (path) out.push({ tag: 'PATH', msg: path, color: ok });
  if (query) {
    const nParams = query.slice(1).split('&').length;
    out.push({ tag: 'QS',   msg: `${nParams} param(s)`, color: ok });
    if (/utm_/.test(query)) out.push({ tag: 'WARN', msg: 'utm_ tracking parameters present', color: warn });
    if (query.length > 500) out.push({ tag: 'WARN', msg: 'long query string — may hit server limits', color: warn });
  }
  if (/localhost|^10\.|^192\.168\./.test(host)) out.push({ tag: 'WARN', msg: "private address — won't resolve off-LAN", color: warn });
  return out;
}
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#8d95a0';
  let url = 'https://example.com/articles/42?utm_source=x';
  const draw = () => {
    const rows = audit(url);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">URL AUDITOR</h3>
      <input id="u" value="${escape(url)}" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--border);border-radius:6px;color:var(--text);font-family:var(--mono);font-size:12px;margin:10px 0">
      <div style="border-top:1px solid var(--border);margin:10px 0"></div>
      ${rows.map(r => `
        <div style="display:flex;gap:12px;padding:3px 0">
          <b style="color:${r.color};font-family:var(--mono);min-width:56px">${r.tag}</b>
          <span style="font-size:12px">${escape(r.msg)}</span>
        </div>`).join('')}
    `;
    root.querySelector('#u').oninput = e => { url = e.target.value; draw(); };
  };
  draw();
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
