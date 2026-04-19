const FILLERS = new Set([
  'very','really','actually','basically','literally','just','simply',
  'quite','rather','somewhat','maybe','perhaps','arguably','clearly',
  'obviously','definitely','certainly','naturally','essentially',
]);
const WEASELS = [
  [/\bin order to\b/gi,              'to'],
  [/\bdue to the fact that\b/gi,     'because'],
  [/\bat this point in time\b/gi,    'now'],
  [/\bit goes without saying that\b/gi, ''],
  [/\bneedless to say\b/gi,          ''],
  [/\bfor all intents and purposes\b/gi, ''],
];
const SAMPLE = 'It goes without saying that we should, in order to make a decision, very clearly and obviously actually consider all the options that are arguably somewhat relevant at this point in time.';

function occam(raw) {
  let s = raw;
  for (const [re, r] of WEASELS) s = s.replace(re, r);
  s = s.split(/\s+/).filter(w => !FILLERS.has(w.toLowerCase().replace(/[.,!?]$/, ''))).join(' ');
  s = s.replace(/\s+([.,!?])/g, '$1').trim();
  return [s, raw.length - s.length];
}

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#6f42c1';
  let input = SAMPLE;
  const draw = () => {
    const [out, saved] = occam(input);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">OCCAM FILTER</h3>
      <p style="color:var(--dim);font-size:12px;font-family:var(--mono);margin:6px 0 10px">saved ${saved} char(s)</p>
      <textarea id="in" style="width:100%;min-height:140px;background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:6px;padding:10px;font-family:inherit;font-size:13px;line-height:19px" placeholder="paste verbose text">${escapeHtml(input)}</textarea>
      <div style="border-top:1px solid var(--border);margin:12px 0"></div>
      <div style="background:var(--bg);border:1px solid var(--border);border-radius:6px;padding:14px;font-size:15px;line-height:21px;font-weight:600;${out ? '' : 'color:var(--dim);font-style:italic'}">${out ? escapeHtml(out) : '(nothing left.)'}</div>
    `;
    root.querySelector('#in').oninput = e => { input = e.target.value; draw(); };
  };
  draw();
}
const escapeHtml = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
