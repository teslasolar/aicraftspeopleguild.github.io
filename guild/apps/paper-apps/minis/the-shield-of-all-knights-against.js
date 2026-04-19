const DECK = [
  ['Anchoring',            'first number you heard is the one you argue around',
                           'write the decision without the anchor visible, then compare'],
  ['Availability',         'recent vivid events dominate the estimate',
                           'count the base rate · ignore the story'],
  ['Confirmation',         'evidence for > evidence against',
                           "list three ways you'd be wrong before listing one you'd be right"],
  ['Sunk cost',            'we already spent X so we must continue',
                           'if starting today with no history, would you choose this path?'],
  ['Planning fallacy',     "this'll take a week (it took three months)",
                           'estimate the outside view · look at comparable past projects'],
  ['Dunning-Kruger',       "can't see the ceiling from below it",
                           'ask the person who makes you feel dumb · and listen'],
  ['Survivorship',         'only successes are visible',
                           "look for what's missing · the dead don't write memoirs"],
  ['Narrative fallacy',    'a clean story feels true',
                           'the world is noisier than any retrofit story · hold ambiguity'],
];
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#1a5c4c';
  const open = new Set();
  const draw = () => {
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">BIAS FLASHCARDS · ${DECK.length}</h3>
      <div style="display:flex;flex-direction:column;gap:8px;margin-top:12px">
        ${DECK.map((b, i) => `
          <div class="card-bias" data-i="${i}"
               style="background:var(--bg);border:1px solid var(--border);border-radius:8px;padding:14px;cursor:pointer">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b style="color:${primary};font-size:15px">${b[0]}</b>
              <span style="color:${primary}">${open.has(i) ? '▾' : '▸'}</span>
            </div>
            <div style="color:var(--dim);font-size:12px;margin-top:4px">${b[1]}</div>
            ${open.has(i) ? `<div style="color:var(--text);font-size:12px;margin-top:8px;font-weight:600">antidote · ${b[2]}</div>` : ''}
          </div>
        `).join('')}
      </div>
    `;
    root.querySelectorAll('.card-bias').forEach(el => {
      el.onclick = () => { const i = +el.dataset.i; if (open.has(i)) open.delete(i); else open.add(i); draw(); };
    });
  };
  draw();
}
