// ═══ Background fabric · event wiring ═══
// Translates chat-mesh events into visual triggers. Each registered
// peer handler fires a projectile at the orb; voice levels drive the
// orb's pulse + vortex spin.

import { registerPeerHandler, pm } from '../peers.js';
import { onLocalLevel } from '../voice/engine.js';

export function wireEvents({ orb, vortex, projectiles }) {
  // Every chat msg → projectile. Karen replies carry id=karen:<...>
  // so they get the gold treatment.
  registerPeerHandler('msg', msg => {
    const kind = String(msg.id || '').startsWith('karen:') ? 'karen' : 'chat';
    projectiles.spawn(kind);
  });

  // Karen presence — super-karen ask = burst of projectiles
  registerPeerHandler('karen:ask', () => {
    for (let i = 0; i < 5; i++) setTimeout(() => projectiles.spawn('karen'), i * 80);
  });
  registerPeerHandler('karen:ready', () => projectiles.spawn('system'));

  // Voice presence pings give a small green projectile per event,
  // and the orb "heats" when anyone announces a mic.
  registerPeerHandler('voice:hello', msg => {
    projectiles.spawn('voice');
    if (msg.state === 'on' && !msg.muted) orb.setHot(true);
  });

  // Local mic level feeds the orb + the vortex spin directly. Remote
  // speakers are already tracked on info.speaking (see peers.js
  // updPeers) — we sample the mesh below on each frame.
  onLocalLevel(({ level, speaking }) => {
    orb.setLevel(level);
    if (speaking) orb.setHot(true);
  });
}

// Called each frame from index.js so the orb reflects remote activity
// too — anyone speaking on the mesh bumps the pulse. This is cheap:
// we just count speaking peers and bias the level.
export function frameSample(orb, vortex) {
  let speaking = 0;
  for (const [, info] of pm) if (info.speaking) speaking++;
  const meshLevel = Math.min(1, speaking * 0.25);
  if (meshLevel > 0) orb.setLevel(meshLevel);
  if (speaking > 0) orb.setHot(true);
  vortex.setSpeed(meshLevel);
}
