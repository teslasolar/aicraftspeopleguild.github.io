// Pushback Dial
const RUNGS = [
  [0,   'You are completely right, keep going exactly as planned.'],
  [15,  'Sounds reasonable. One quick question — have you tested the edge case where your assumption fails?'],
  [30,  "I'm not sure this holds. What's the specific evidence that your premise is true?"],
  [45,  "You're glossing over the hard part. Name the thing you're avoiding."],
  [60,  'This argument collapses the moment someone asks for citations. Where are they?'],
  [75,  "You're confusing certainty with clarity. Prove you're not just attached."],
  [90,  "This is the kind of take you'll regret in six months. Tell me why you won't."],
  [100, "No. Start over. You haven't earned this conclusion."],
];

export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#e3b341';
  let level = 30;
  const draw = () => {
    const rung = RUNGS.slice().sort((a,b) => Math.abs(a[0]-level) - Math.abs(b[0]-level))[0];
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">PUSHBACK DIAL</h3>
      <p style="color:var(--dim);font-size:12px;margin:8px 0">At level ${level} the AI says:</p>
      <div style="background:var(--bg);border:1px solid var(--border);border-radius:8px;padding:18px;font-size:15px;line-height:22px">${rung[1]}</div>
      <input type="range" min="0" max="100" value="${level}" id="s"
             style="width:100%;margin:16px 0;accent-color:${primary}">
      <div style="display:flex;justify-content:space-between;font-size:11px">
        <span style="color:#3fb950">agreeable</span>
        <span style="color:#e3b341">surgical</span>
        <span style="color:#f85149">scorched</span>
      </div>
    `;
    root.querySelector('#s').oninput = e => { level = +e.target.value; draw(); };
  };
  draw();
}
