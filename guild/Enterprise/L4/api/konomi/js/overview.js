import { esc } from './shell.js';

export const overview = {
  id: 'overview',
  label: 'overview',
  render(bundle) {
    const m = bundle._meta;
    const stds = Object.entries(bundle.standards).map(([sid, s]) => `
      <div class="card" data-std="${sid}">
        <h3>${esc(s._std?.id || sid)}</h3>
        <p>${esc(s._std?.scope || '')}</p>
        <div class="meta">
          <span><b>${s.udts.length}</b> udts</span>
          <span><b>${s.state_machines.length}</b> sm</span>
          <span>${esc(s._std?.version || '')}</span>
        </div>
      </div>`).join('');
    return `
      <div class="hero">
        <h2>◈ Konomi · v${esc(m.version)}</h2>
        <div class="motto">“${esc(m.motto)}”</div>
        <p>${esc(m.scope)}. This bundle is the live projection of the <code>guild/Enterprise/docs/standards/konomi/</code> source tree into a single JSON — consumed by Pages apps, phone apps, and agents. Generated at <code>${esc(m.generated_at)}</code>.</p>
        <div class="kpis">
          <div class="kpi"><div class="k">base UDTs</div><div class="v">${m.counts.base_udts}</div></div>
          <div class="kpi"><div class="k">standards</div><div class="v">${m.counts.standards}</div></div>
          <div class="kpi"><div class="k">std UDTs</div><div class="v">${m.counts.udts}</div></div>
          <div class="kpi"><div class="k">state machines</div><div class="v">${m.counts.state_machines}</div></div>
          <div class="kpi"><div class="k">crosswalks</div><div class="v">${m.counts.crosswalks}</div></div>
        </div>
        <div class="api-use">
<span class="c"># fetch the whole bundle — phone or browser, doesn't matter</span>
curl <span class="s">https://teslasolar.github.io/aicraftspeopleguild.github.io/guild/Enterprise/L4/api/konomi.json</span>
<span class="c"># or just the catalog + one standard</span>
curl <span class="s">.../api/konomi/index.json</span>
curl <span class="s">.../api/konomi/stds/isa95.json</span>
        </div>
      </div>
      <h4 style="font-family:var(--ff-mono);color:var(--ft);font-size:11px;letter-spacing:1.5px;text-transform:uppercase;margin:8px 0 10px">standards in the bundle</h4>
      <div class="grid">${stds}</div>`;
  },
};
