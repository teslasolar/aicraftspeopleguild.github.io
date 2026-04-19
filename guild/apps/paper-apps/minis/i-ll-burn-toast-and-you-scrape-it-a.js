// Burner vs Scraper · 5-question quiz
const QUIZ = [
  ['when the bug is already on fire you…',        'ship the patch, retro later',          'write the retro first, then ship'],
  ['new framework drops this week…',              'port a prototype tonight',             'wait three releases and read the postmortems'],
  ['the PR description you write is…',            'one sentence and the test output',     'four sections and the ADR'],
  ['code review comments should…',                'ship the change faster',               'prevent the next one'],
  ['the perfect Friday involves…',                'one big-bet deploy',                   'nothing scheduled, on-call hat off'],
];
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#b85c5c';
  let answers = Array(QUIZ.length).fill(null);
  const label = () => {
    const done = !answers.includes(null);
    const score = answers.reduce((s, a) => s + (a === 0 ? -1 : a === 1 ? 1 : 0), 0);
    if (!done) return `${answers.filter(a=>a!==null).length} / ${QUIZ.length}`;
    if (score <= -3) return 'BURNER · torch in hand';
    if (score <= -1) return 'BURNER-LEANING · shipper with sparks';
    if (score === 0) return 'AMBIDEXTROUS · rare';
    if (score <= 2)  return 'SCRAPER-LEANING · careful operator';
    return 'SCRAPER · the one cleaning up';
  };
  const draw = () => {
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">BURNER / SCRAPER QUIZ</h3>
      <p style="font-size:18px;font-weight:700;color:${primary};margin:8px 0">${label()}</p>
      ${QUIZ.map((q, i) => `
        <div style="margin:16px 0">
          <p style="font-weight:600;margin-bottom:6px">${i+1}. ${q[0]}</p>
          <div style="display:flex;gap:6px">
            ${[0,1].map(v => `
              <button data-i="${i}" data-v="${v}"
                style="flex:1;padding:10px;background:${answers[i]===v ? primary+'33' : 'var(--bg)'};border:1px solid ${answers[i]===v?primary:'var(--border)'};border-radius:6px;color:${answers[i]===v?primary:'var(--text)'};font-size:12px;line-height:15px;cursor:pointer;font-family:inherit;font-weight:${answers[i]===v?700:400}">${q[v+1]}</button>`).join('')}
          </div>
        </div>`).join('')}
      ${!answers.includes(null) ? `<button id="reset" style="width:100%;padding:10px;background:transparent;color:var(--dim);border:1px solid var(--border);border-radius:6px;cursor:pointer">reset</button>` : ''}
    `;
    root.querySelectorAll('[data-i]').forEach(b => b.onclick = () => {
      answers[+b.dataset.i] = +b.dataset.v; draw();
    });
    const r = root.querySelector('#reset'); if (r) r.onclick = () => { answers = Array(QUIZ.length).fill(null); draw(); };
  };
  draw();
}
