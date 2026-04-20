// ═══ Voice fabric · mesh presence ═══
// Three bus events announce voice state to remote peers, mirrored as
// Script UDT instances under L3/udts/script/instances/voice-*.json:
//
//   voice:hello   — "my mic is live / muted / stopped" (broadcast)
//   voice:mute    — "I muted myself" (state delta)
//   voice:level   — lightweight rms tick (optional, not sent by default
//                   because remote ontrack + local analyser already
//                   give us speaking dots without extra bandwidth)

import { bcast } from '../p2p.js';
import { registerPeerHandler } from '../peers.js';
import { myId } from '../config.js';
import { onVoiceChange } from './engine.js';

export function startPresence() {
  onVoiceChange(s => {
    bcast({ t: 'voice:hello', from: myId, state: s.status, muted: !!s.muted });
  });

  registerPeerHandler('voice:hello', (msg, rid) => {
    import('../peers.js').then(m => {
      m.setPeerVoice?.(msg.from || rid, msg.state === 'on' && !msg.muted);
    });
  });

  // Ping so peers who came online before us announce back.
  setTimeout(() => { try { bcast({ t: 'voice:ping', from: myId }); } catch (e) {} }, 1500);
  registerPeerHandler('voice:ping', () => {
    import('./engine.js').then(m => {
      const s = m.voiceState();
      if (s.active) bcast({ t: 'voice:hello', from: myId, state: s.status, muted: !!s.muted });
    });
  });
}
