const BLOOMS = [
  '🌸 list the three assumptions this plan depends on, in priority order',
  '🌸 write the failure case first · then the code · then the test',
  '🌸 name the invariant that must hold across every commit on this branch',
  '🌸 define success in one sentence before opening the editor',
  "🌸 rank the constraints · stop when you hit the one that can't move",
  '🌸 describe the interface using only the words the caller knows',
  "🌸 draw the data flow · highlight the one arrow you can't explain",
  '🌸 enumerate the exits before committing to the entrance',
  '🌸 say what this does NOT do, until someone asks',
  '🌸 convert your intuition into a checklist, then throw the checklist at an intern',
];
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#79c0ff';
  let idx = 0;
  const draw = () => {
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">BLOOM · RATIONAL</h3>
      <div style="background:var(--bg);border:1px solid var(--border);border-radius:8px;padding:22px;margin:14px 0;font-size:18px;line-height:26px;font-weight:600">${BLOOMS[idx]}</div>
      <button id="b" style="width:100%;padding:12px;background:${primary};color:#fff;border:0;border-radius:8px;font-weight:700;cursor:pointer">🌸 bloom again</button>
      <p style="color:var(--dim);font-size:11px;margin-top:12px">structure that keeps the empire running</p>
    `;
    root.querySelector('#b').onclick = () => { idx = (idx + 1 + Math.floor(Math.random()*(BLOOMS.length-1))) % BLOOMS.length; draw(); };
  };
  draw();
}
