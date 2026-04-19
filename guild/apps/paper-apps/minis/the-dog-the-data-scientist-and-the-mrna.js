// Bayes pocketbook
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#79c0ff';
  let prior = 0.05, sens = 0.9, fpr = 0.1;
  const draw = () => {
    const num = prior * sens;
    const den = num + (1 - prior) * fpr;
    const post = den === 0 ? 0 : num / den;
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">BAYES POCKETBOOK</h3>
      ${sl('prior  P(H)',     prior, 'prior')}
      ${sl('sens   P(+|H)',   sens,  'sens')}
      ${sl('fpr    P(+|¬H)',  fpr,   'fpr')}
      <div style="border-top:1px solid var(--border);margin:16px 0"></div>
      <p style="color:var(--dim);font-size:11px">posterior P(H|+)</p>
      <div style="font-family:var(--mono);font-size:32px;font-weight:700;color:${primary}">${post.toFixed(4)} <span style="font-size:16px">(${(post*100).toFixed(1)}%)</span></div>
    `;
    root.querySelectorAll('input[type=range]').forEach(r => {
      r.oninput = e => {
        if (r.dataset.k === 'prior') prior = +e.target.value;
        if (r.dataset.k === 'sens')  sens  = +e.target.value;
        if (r.dataset.k === 'fpr')   fpr   = +e.target.value;
        draw();
      };
    });
  };
  const sl = (label, val, key) => `
    <div style="margin:10px 0">
      <div style="font-family:var(--mono);font-size:12px">${label} = <b style="color:${primary}">${val.toFixed(3)}</b></div>
      <input type="range" min="0.001" max="0.999" step="0.001" value="${val}" data-k="${key}" style="width:100%;accent-color:${primary}">
    </div>`;
  draw();
}
