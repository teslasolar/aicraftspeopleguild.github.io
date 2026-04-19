import { esc } from './shell.js';

export const base = {
  id: 'base',
  label: 'base UDTs',
  render(bundle) {
    const rows = Object.entries(bundle.base).map(([n, u]) => {
      const fields = (u.fields || []).map(f =>
        `<span><b>${esc(f.n || f.name)}</b>${f.t ? ':' + esc(f.t) : ''}${f.type ? ':' + esc(f.type) : ''}${f.unit ? ' (' + esc(f.unit) + ')' : ''}</span>`,
      ).join('');
      return `<tr>
        <td class="n">${esc(n)}</td>
        <td class="f">${fields || '<span style="color:var(--ft)">—</span>'}</td>
        <td class="f" style="color:var(--ft)">${esc(u.meta?.desc || '')}</td>
      </tr>`;
    }).join('');
    return `
      <div style="background:var(--card);border:1px solid var(--bd);border-radius:var(--r-md);padding:14px 16px;box-shadow:0 1px 3px var(--sh)">
        <h3 style="font-family:var(--ff-display);font-weight:700;font-size:15px;margin-bottom:4px">Layer 1 · base UDTs</h3>
        <p style="color:var(--dm);font-size:12px;margin-bottom:12px">Atomic reusable types from which every industrial standard is composed. UUID/PATH/TAG/URN for identification · ISO8601/EPOCH/OPC_FILETIME for time · OPC-style Quality flags · {v,q,t,unit} Value wrapper.</p>
        <table class="udts">
          <thead><tr><th>type</th><th>fields</th><th>desc</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  },
};
