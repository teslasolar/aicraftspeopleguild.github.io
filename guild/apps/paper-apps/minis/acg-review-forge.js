const PROMPTS = [
  "name the invariant this change protects. if you can't, the PR isn't ready",
  'what breaks first when this is wrong, and how does the on-call find out?',
  'trace the one path a hostile input could take through this function',
  'which test would have caught the previous version of this bug?',
  'if this gets reverted in six months, whose name is on the hotfix?',
  "explain the data flow in one sentence. if you can't, split the PR",
  'mark every line that would be unchanged by a completely different approach',
  'would you land this on Friday at 5pm? show your work',
  "is there a simpler thing that's 80% as good? who argues for it?",
  'circle the commit that would still ship if the others were reverted',
  'which reader needs to understand this in two years with no context?',
  'name the assumption that, if false, makes the entire change wrong',
];
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#c47a20';
  let flash = '';
  const draw = () => {
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">REVIEW FORGE · ${PROMPTS.length}</h3>
      ${flash ? `<div style="color:#3fb950;font-size:11px;margin-top:8px">${flash}</div>` : ''}
      <div style="display:flex;flex-direction:column;gap:6px;margin-top:10px">
        ${PROMPTS.map((p,i) => `
          <div class="prompt-card" data-i="${i}"
               style="background:var(--bg);border:1px solid var(--border);border-radius:8px;padding:12px;cursor:pointer;font-size:13px;line-height:17px">${escape(p)}</div>
        `).join('')}
      </div>
    `;
    root.querySelectorAll('.prompt-card').forEach(el => {
      el.onclick = async () => {
        const p = PROMPTS[+el.dataset.i];
        try { await navigator.clipboard.writeText(p); flash = 'copied: ' + p.slice(0, 40) + '…'; }
        catch { flash = 'select and copy: '+ p; }
        draw();
      };
    });
  };
  draw();
}
const escape = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
