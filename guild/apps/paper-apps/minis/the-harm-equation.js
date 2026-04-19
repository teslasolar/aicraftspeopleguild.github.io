// Harm Equation · severity × reach × (1 − reversibility)
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#1a5c4c';
  let sev = 3, reach = 3, rev = 0.5;
  const band = s => s >= 18 ? ['CRITICAL · do not ship',       '#f85149']
                 : s >= 10 ? ['HIGH · mitigation required',    '#e3b341']
                 : s >=  4 ? ['MODERATE · document + monitor', '#f0883e']
                 :            ['LOW · proceed',                 '#3fb950'];
  const draw = () => {
    const score = sev * reach * (1 - rev);
    const [label, color] = band(score);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">HARM EQUATION</h3>
      ${sl('severity (1–5)', sev, 1, 5, 1, 'sev')}
      ${sl('reach (1–5)',    reach, 1, 5, 1, 'reach')}
      ${sl('reversibility',  rev.toFixed(2), 0, 1, 0.01, 'rev')}
      <div style="border-top:1px solid var(--border);margin:18px 0"></div>
      <div style="color:var(--dim);font-size:11px">score = sev × reach × (1 − rev)</div>
      <div style="font-family:var(--mono);font-size:40px;font-weight:700;color:${color};margin:6px 0">${score.toFixed(1)}</div>
      <div style="color:${color};font-weight:700">${label}</div>
    `;
    root.querySelectorAll('input[type=range]').forEach(r => {
      r.oninput = e => {
        if (r.dataset.k === 'sev')   sev   = +e.target.value;
        if (r.dataset.k === 'reach') reach = +e.target.value;
        if (r.dataset.k === 'rev')   rev   = +e.target.value;
        draw();
      };
    });
  };
  const sl = (label, val, min, max, step, key) => `
    <div style="margin:12px 0">
      <div style="font-family:var(--mono);font-size:12px;color:var(--text)">${label}: <b style="color:${primary}">${val}</b></div>
      <input type="range" min="${min}" max="${max}" step="${step}" value="${val}" data-k="${key}"
             style="width:100%;accent-color:${primary}">
    </div>`;
  draw();
}
