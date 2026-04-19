// ═══ Karen · mesh presence ═══
// Three bus events shape Karen's gossip on the mesh. Each is also
// registered as a Script UDT instance under L3/udts/script/instances/
// so the catalog page reflects the handler graph.
//
//   karen:ready   — "I just summoned my Karen" (on local ready + on ping reply)
//   karen:ping    — "who has Karen?" (broadcast on boot so late joiners sync)
//   karen:ask     — super-karen fan-out prompt (each Karen answers independently)
//
// Event handlers live here so the wiring is all in one place.

import { bcast } from '../p2p.js';
import { pm, registerPeerHandler } from '../peers.js';
import { myId, myNm } from '../config.js';
import { addMsg } from '../chat.js';
import { isReady, askLocalKaren, onKarenChange } from './engine.js';

export const KAREN_PID  = 'karen:' + myId;
export const KAREN_NAME = `Karen (${myNm})`;
export const KAREN_EMO  = '🤖';

// Broadcast "karen is online". Called on local ready + in response to
// karen:ping from a remote peer so late joiners catch up.
export function announceKaren() {
  bcast({ t: 'karen:ready', from: myId });
}

export function startPresence() {
  // Flip the local selfHasKaren bit when Karen becomes ready, then
  // announce to the mesh. Dynamic import of peers.js is fine here —
  // the static import for pm/registerPeerHandler already ensures the
  // module is loaded by the time this listener fires.
  onKarenChange(s => {
    if (!s.ready) return;
    import('../peers.js').then(m => m.setSelfKaren?.(true));
    announceKaren();
  });

  // Mesh event handlers — each mirrors a Script UDT instance.
  registerPeerHandler('karen:ready', (msg, rid) => {
    const from = msg.from || rid;
    import('../peers.js').then(m => m.setPeerKaren?.(from, true));
  });

  registerPeerHandler('karen:ping', () => {
    if (isReady()) announceKaren();
  });

  registerPeerHandler('karen:ask', async msg => {
    if (!isReady()) return;
    const prompt = String(msg.prompt || '').slice(0, 2000);
    if (!prompt) return;
    const reply = await askLocalKaren(prompt);
    const mid = KAREN_PID + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
    bcast({ t: 'msg', id: KAREN_PID, mid, txt: reply });
    addMsg(KAREN_PID, KAREN_NAME, KAREN_EMO, reply, false, null);
  });

  // On boot, send a ping so any existing Karens announce back. Small
  // delay lets data channels open first; no peers yet means a silent
  // no-op, which is fine.
  setTimeout(() => { try { bcast({ t: 'karen:ping', from: myId }); } catch (e) {} }, 1500);
}

// Count Karens on the mesh (including self if local Karen is ready).
export function karenCount() {
  let n = isReady() ? 1 : 0;
  for (const [, info] of pm) if (info.hasKaren) n++;
  return n;
}
