// ═══ Karen · chat commands ═══
// Pure dispatcher: chat.js calls into handleKarenCommand when the user
// types @karen or @karens. The solo path asks the local engine and
// broadcasts the reply; the super path fans out karen:ask so every
// online Karen answers independently.

import { bcast } from '../p2p.js';
import { myId, myNm } from '../config.js';
import { addMsg } from '../chat.js';
import { isReady, askLocalKaren } from './engine.js';
import { KAREN_PID, KAREN_NAME, KAREN_EMO } from './presence.js';

const mkMid = pid => pid + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);

export async function handleKarenCommand(kind, prompt) {
  if (!prompt || typeof prompt !== 'string') return;

  if (kind === 'solo') {
    bcast({ t: 'msg', id: myId, mid: mkMid(myId), txt: '@karen ' + prompt });
    addMsg(myId, myNm, '🎤', '@karen ' + prompt, false, null);
    const reply = await askLocalKaren(prompt);
    bcast({ t: 'msg', id: KAREN_PID, mid: mkMid(KAREN_PID), txt: reply });
    addMsg(KAREN_PID, KAREN_NAME, KAREN_EMO, reply, false, null);
    return;
  }

  if (kind === 'super') {
    bcast({ t: 'msg', id: myId, mid: mkMid(myId), txt: '@karens ' + prompt });
    addMsg(myId, myNm, '📡', '@karens ' + prompt, false, null);
    addMsg(null, null, null, 'asking every online Karen…', true);
    bcast({ t: 'karen:ask', prompt, askedBy: myId, at: Date.now() });
    if (isReady()) {
      const reply = await askLocalKaren(prompt);
      bcast({ t: 'msg', id: KAREN_PID, mid: mkMid(KAREN_PID), txt: reply });
      addMsg(KAREN_PID, KAREN_NAME, KAREN_EMO, reply, false, null);
    }
  }
}
