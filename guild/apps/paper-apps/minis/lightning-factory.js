// 7.83 Hz pulse · CSS keyframes
export default async function render(root, paper) {
  const primary = paper.theme_color_hex || '#c47a20';
  const periodMs = (1000 / 7.83).toFixed(0);
  root.innerHTML = `
    <style>
      @keyframes schumann { 0%,100% { transform: scale(1); } 50% { transform: scale(1.55); } }
      .pulse { animation: schumann ${periodMs}ms linear infinite; }
    </style>
    <h3 style="font-family:var(--mono);color:${primary};font-size:11px;letter-spacing:2px;text-transform:uppercase;font-weight:700">7.83 Hz</h3>
    <p style="color:var(--dim);font-size:12px">the Earth's Schumann resonance · one pulse every ${periodMs} ms</p>
    <div style="display:grid;place-items:center;padding:40px 0">
      <div class="pulse" style="width:120px;height:120px;border-radius:50%;background:${primary};box-shadow:0 0 80px ${primary}66"></div>
    </div>
    <p style="color:var(--dim);font-size:11px;font-family:var(--mono)">no audio — speakers don't do subsonic. this is the visual metronome.</p>
  `;
}
