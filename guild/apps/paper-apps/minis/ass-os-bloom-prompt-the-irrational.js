const BLOOMS = [
  '🌸 describe this bug to an octopus that has never seen a keyboard',
  '🌸 rewrite your commit message as a haiku about weather',
  '🌸 swap all nouns in the README with musical instruments',
  '🌸 answer the question with a question that is also the answer',
  '🌸 propose the fix that makes the most people weep',
  "🌸 name a variable 'arbitrage' and justify it in one breath",
  '🌸 describe the function by the noise it makes when it misbehaves',
  '🌸 recurse on the vibes, not the data',
  '🌸 refactor the test suite as a recipe for a sandwich',
  '🌸 narrate the stack trace from the perspective of gravity',
];
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#a371f7';
  let idx = 0;
  const draw = () => {
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">BLOOM · IRRATIONAL</h3>
      <div style="background:var(--bg);border:1px solid var(--border);border-radius:8px;padding:22px;margin:14px 0;font-size:18px;line-height:26px;font-weight:600">${BLOOMS[idx]}</div>
      <button id="b" style="width:100%;padding:12px;background:${primary};color:#fff;border:0;border-radius:8px;font-weight:700;cursor:pointer">🌸 bloom again</button>
      <p style="color:var(--dim);font-size:11px;margin-top:12px">one prompt per problem · if you understood it, keep going</p>
    `;
    root.querySelector('#b').onclick = () => { idx = (idx + 1 + Math.floor(Math.random()*(BLOOMS.length-1))) % BLOOMS.length; draw(); };
  };
  draw();
}
