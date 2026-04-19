import { esc } from './shell.js';

export const standards = {
  id: 'standards',
  label: 'standards',
  render(bundle) {
    return Object.entries(bundle.standards).map(([sid, s]) => {
      const meta = s._std || {};
      const udtRows = (s.udts || []).map(u => {
        const fields = (u.fields || []).slice(0, 8).map(f =>
          `<span><b>${esc(f.n || f.name)}</b>${f.t ? ':' + esc(f.t) : ''}${f.type ? ':' + esc(f.type) : ''}</span>`,
        ).join('');
        return `<tr>
          <td class="n">${esc(u.name)}</td>
          <td class="f">${fields || '<span style="color:var(--ft)">—</span>'}</td>
          <td class="f" style="color:var(--ft)">${esc((u.meta?.desc) || '')}</td>
        </tr>`;
      }).join('');
      const smBlocks = (s.state_machines || []).map(sm => {
        const states = (sm.states || []).map(st =>
          `<span class="state${st === sm.initial ? ' init' : ''}">${esc(st)}</span>`,
        ).join('');
        const trs = (sm.transitions || []).map(t =>
          `<div class="trans"><b>${esc(t.from)}</b> → <b>${esc(t.to)}</b>${t.trigger ? ' · <i>' + esc(t.trigger) + '</i>' : ''}</div>`,
        ).join('');
        return `<div class="sm">
          <b style="font-family:var(--ff-mono);font-size:11px">${esc(sm.name)}</b>
          <div class="states">${states}</div>
          <div class="transitions">${trs}</div>
        </div>`;
      }).join('');
      const h = meta.hierarchy
        ? `<p style="font-family:var(--ff-mono);font-size:11px;color:var(--dm);margin:6px 0 10px">${esc(meta.hierarchy)}</p>`
        : '';
      const levels = meta.levels
        ? `<h4>levels</h4><div class="states">${(meta.levels || []).map(l => `<span class="state">${esc(l)}</span>`).join('')}</div>`
        : '';
      return `<details class="std" data-sid="${sid}">
        <summary>
          ${esc(meta.id || sid)}
          <span class="scope">· ${esc(meta.scope || '')}</span>
          <span class="pill">${s.udts.length} udts</span>
          ${s.state_machines.length ? `<span class="pill">${s.state_machines.length} sm</span>` : ''}
        </summary>
        <div class="content">
          ${h}
          ${levels}
          ${udtRows ? `<h4>user-defined types</h4><table class="udts"><thead><tr><th>name</th><th>fields</th><th>desc</th></tr></thead><tbody>${udtRows}</tbody></table>` : ''}
          ${smBlocks ? `<h4>state machines</h4>${smBlocks}` : ''}
          <div style="margin-top:10px;font-family:var(--ff-mono);font-size:10.5px;color:var(--ft)">
            <a href="stds/${sid}.json" target="_blank" rel="noopener" style="color:var(--tl);text-decoration:none">stds/${sid}.json ↗</a>
          </div>
        </div>
      </details>`;
    }).join('');
  },
};
