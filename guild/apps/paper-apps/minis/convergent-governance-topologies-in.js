// Governance Topology classifier
function classify(c, v, vis) {
  if (c > 0.7 && v < 0.4) return ['Monarchic Hub', "one decider, loose process — fast but brittle; whoever stands next to the hub gets everything"];
  if (c < 0.3 && v > 0.7) return ['Florentine Guild', 'flat body, strong veto — medieval consensus; hard to move, but hard to corrupt'];
  if (c < 0.3 && v < 0.4) return ['Bazaar', "nobody's in charge, majority rules — fast on small things, paralyzed on anything big"];
  if (vis < 0.3)          return ['Star Chamber', 'opaque process regardless of shape — trust erodes; newcomers discover the rules only after breaking them'];
  if (c >= 0.3 && c <= 0.7 && v > 0.5) return ['Committee', 'moderate hierarchy, high-ish veto — every decision is a settlement'];
  return ['Federated Specialists', 'the stable attractor guilds keep rediscovering · domain leads with cross-review + open logs'];
}

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#a371f7';
  let c = 0.5, v = 0.3, vis = 0.5;
  const draw = () => {
    const [name, blurb] = classify(c, v, vis);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">GOVERNANCE TOPOLOGY</h3>
      ${sl('centralization',  c, 'c')}
      ${sl('veto strength',   v, 'v')}
      ${sl('visibility',      vis, 'vis')}
      <div style="border-top:1px solid var(--border);margin:16px 0"></div>
      <p style="color:var(--dim);font-size:11px">emergent form</p>
      <div style="font-size:22px;font-weight:700;color:${primary}">${name}</div>
      <p style="margin-top:8px;line-height:18px">${blurb}</p>
    `;
    root.querySelectorAll('input[type=range]').forEach(r => {
      r.oninput = e => {
        if (r.dataset.k === 'c')   c   = +e.target.value;
        if (r.dataset.k === 'v')   v   = +e.target.value;
        if (r.dataset.k === 'vis') vis = +e.target.value;
        draw();
      };
    });
  };
  const sl = (label, val, key) => `
    <div style="margin:10px 0">
      <div style="font-family:var(--mono);font-size:12px">${label} = <b style="color:${primary}">${val.toFixed(2)}</b></div>
      <input type="range" min="0" max="1" step="0.01" value="${val}" data-k="${key}" style="width:100%;accent-color:${primary}">
    </div>`;
  draw();
}
