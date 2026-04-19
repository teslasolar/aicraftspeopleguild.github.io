// Toast Carbonization Clock
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#f0883e';
  let started = null, timer = null;
  const stages = t =>
    started === null ? { name: 'cold',         blurb: 'bread is fine. no heat yet.',                                color: '#8b949e' } :
    t <  30          ? { name: 'toasting',     blurb: 'normal.',                                                    color: '#3fb950' } :
    t <  60          ? { name: 'brown',        blurb: 'the intended state. take it out.',                           color: '#e3b341' } :
    t < 120          ? { name: 'carbonizing',  blurb: 'volatiles leaving · structural change beginning.',           color: '#f0883e' } :
    t < 240          ? { name: 'carbonized',   blurb: 'matrix failure · no longer food',                            color: '#f85149' }
                     : { name: 'ash',          blurb: 'technical debt acquired entirely passively',                 color: '#f85149' };
  const draw = () => {
    const t = started ? Math.floor((Date.now() - started) / 1000) : 0;
    const s = stages(t);
    root.innerHTML = `
      <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">TOAST CARBONIZATION</h3>
      <div style="font-family:var(--mono);font-size:64px;font-weight:700;color:${s.color};margin:16px 0">${t}s</div>
      <div style="font-size:20px;font-weight:700;color:${s.color}">${s.name.toUpperCase()}</div>
      <p style="margin:8px 0;line-height:18px">${s.blurb}</p>
      <div style="display:flex;gap:8px;margin-top:16px">
        <button id="start" style="flex:1;padding:10px;background:${primary};color:#fff;border:0;border-radius:6px;font-weight:700;cursor:pointer">${started ? 'restart' : 'start'}</button>
        <button id="stop" style="flex:1;padding:10px;background:transparent;color:${primary};border:1px solid ${primary};border-radius:6px;cursor:pointer">take it out</button>
      </div>
    `;
    root.querySelector('#start').onclick = () => { started = Date.now(); schedule(); };
    root.querySelector('#stop').onclick  = () => { started = null; if (timer) clearTimeout(timer); draw(); };
  };
  const schedule = () => {
    if (timer) clearTimeout(timer);
    draw();
    if (started !== null) timer = setTimeout(schedule, 1000);
  };
  draw();
}
