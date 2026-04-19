// ═══ Karen · per-user WebLLM peer ═══
// Each browser gets a personal Karen — a WebLLM model running on the
// user's GPU. Karens announce themselves on the chat mesh so every
// operator can see how many Karens are online and summon collective
// "super-Karen" answers by asking all of them at once.
//
// Commands in chat:
//   @karen  <prompt>   · ask *your* Karen (local, no network)
//   @karens <prompt>   · ask every online Karen (super-Karen fan-out)
//
// Model: SmolLM2-135M-Instruct-q4f16_1 — ~720 MB download, cached in
// IndexedDB by WebLLM after first fetch so "summon" is instant next time.

import { bcast } from './p2p.js';
import { pm, registerPeerHandler } from './peers.js';
import { myId, myNm } from './config.js';
import { addMsg } from './chat.js';
import { log } from './ui.js';

const WEBLLM_URL = 'https://esm.run/@mlc-ai/web-llm';
// Smallest-first fallback list. The first entry that's actually in the
// WebLLM version's prebuilt appConfig wins — different releases drop
// different models, so hard-coding one id breaks whenever MLC ships.
const MODEL_CANDIDATES = [
  'SmolLM2-135M-Instruct-q0f16-MLC',
  'SmolLM2-135M-Instruct-q4f16_1-MLC',
  'Qwen2.5-0.5B-Instruct-q4f16_1-MLC',
  'SmolLM2-360M-Instruct-q4f16_1-MLC',
  'Llama-3.2-1B-Instruct-q4f16_1-MLC',
];
const KAREN_PID  = 'karen:' + myId;
const KAREN_NAME = `Karen (${myNm})`;
const KAREN_EMO  = '🤖';

const SYSTEM_PROMPT =
  "You are Karen, a concise, friendly AI assistant running locally in " +
  "a peer-to-peer browser chat. Keep replies short (1-3 sentences). " +
  "If multiple Karens are asked the same question, each answers " +
  "independently from their own local model — it's OK for answers to " +
  "differ; the user wants to see the spread.";

let webllm = null;
let engine = null;
let loadState = { status: 'idle', progress: 0, text: '' };
const listeners = new Set();

export function karenState() { return { ...loadState, ready: !!engine, pid: KAREN_PID, name: KAREN_NAME, emo: KAREN_EMO }; }
export function onKarenChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }
function fire() { for (const fn of listeners) try { fn(karenState()); } catch (e) {} }

// ── GPU check + model load ──────────────────────────────────────────
export async function summonKaren() {
  if (engine || loadState.status === 'loading' || loadState.status === 'importing') return;
  if (!navigator.gpu) {
    loadState = { status: 'no-gpu', progress: 0, text: 'WebGPU not available on this device' };
    fire();
    log('Karen: WebGPU not available — needs Chrome/Edge on a device with a GPU', 'er');
    return;
  }
  try {
    loadState = { status: 'importing', progress: 0, text: 'loading webllm runtime…' };
    fire();
    webllm = await import(WEBLLM_URL);

    // Pick the first candidate that's actually in the current
    // prebuiltAppConfig. WebLLM drops/renames models between releases,
    // so probe instead of hard-coding.
    const catalog = webllm.prebuiltAppConfig?.model_list || [];
    const available = new Set(catalog.map(m => m.model_id));
    const modelId = MODEL_CANDIDATES.find(id => available.has(id));
    if (!modelId) {
      throw new Error(
        'no candidate model in WebLLM catalog — first few available: ' +
        catalog.slice(0, 4).map(m => m.model_id).join(', '),
      );
    }

    loadState = { status: 'loading', progress: 0, text: `fetching ${modelId} (first time only)…` };
    fire();
    engine = await webllm.CreateMLCEngine(modelId, {
      initProgressCallback: report => {
        loadState = {
          status: 'loading',
          progress: Math.round((report.progress || 0) * 100),
          text: report.text || 'loading…',
          model: modelId,
        };
        fire();
      },
    });
    loadState = { status: 'ready', progress: 100, text: 'ready' };
    fire();
    log('Karen ready ✓', 'ok');
    announceKaren();
  } catch (e) {
    loadState = { status: 'error', progress: 0, text: e.message };
    fire();
    log('Karen load err: ' + e.message, 'er');
  }
}

// ── Announce / presence ─────────────────────────────────────────────
// Broadcast "karen is online" so every remote peer can flip their
// hasKaren flag on us. Sent both on local summon and in response to
// any peer's karen:ping. One line of bcast is enough — the normal hi
// payload also carries hasKaren for peers joining later.
function announceKaren() {
  bcast({ t: 'karen:ready', from: myId });
}

// On our own ready, we also set our own hasKaren locally so updPeers()
// counts us in the Karen total without waiting for a remote round-trip.
onKarenChange(s => {
  if (s.ready && window.__pmSelfKaren !== true) {
    window.__pmSelfKaren = true;
    // peers.js watches this flag through its own helper (see below).
    import('./peers.js').then(m => m.setSelfKaren?.(true));
  }
});

// When a peer announces karen:ready, mark them as hasKaren in pm.
registerPeerHandler('karen:ready', (msg, rid) => {
  const from = msg.from || rid;
  import('./peers.js').then(m => m.setPeerKaren?.(from, true));
});

// When a new peer sends us a ping asking who has Karen, reply if we do.
registerPeerHandler('karen:ping', () => {
  if (engine) announceKaren();
});

// Super-Karen: someone asked every Karen a question. Answer locally
// and publish the reply as a normal chat message (pid = karen:<myId>),
// so everyone's chat list shows every Karen's take.
registerPeerHandler('karen:ask', async (msg, rid) => {
  if (!engine) return;
  const prompt = String(msg.prompt || '').slice(0, 2000);
  if (!prompt) return;
  // Show locally that our Karen is thinking
  const reply = await askLocalKaren(prompt);
  // broadcast as a chat msg so all peers render it like any chat line
  const mid = KAREN_PID + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
  bcast({ t: 'msg', id: KAREN_PID, mid, txt: reply });
  addMsg(KAREN_PID, KAREN_NAME, KAREN_EMO, reply, false, null);
});

// ── Local chat ──────────────────────────────────────────────────────
export async function askLocalKaren(prompt) {
  if (!engine) return '(Karen is not summoned yet — click “summon” in the peer list)';
  try {
    const res = await engine.chat.completions.create({
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user',   content: prompt },
      ],
      max_tokens: 256, temperature: 0.7, stream: false,
    });
    return res.choices?.[0]?.message?.content?.trim() || '(empty reply)';
  } catch (e) {
    return '(Karen errored: ' + e.message + ')';
  }
}

// ── Public commands ─────────────────────────────────────────────────
// Called from chat.js when the user types @karen / @karens.
export async function handleKarenCommand(kind, prompt) {
  if (kind === 'solo') {
    // Publish the question locally + remotely so the chat shows context
    const mid = myId + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
    bcast({ t: 'msg', id: myId, mid, txt: '@karen ' + prompt });
    addMsg(myId, myNm, '🎤', '@karen ' + prompt, false, null);
    const reply = await askLocalKaren(prompt);
    const rmid = KAREN_PID + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
    bcast({ t: 'msg', id: KAREN_PID, mid: rmid, txt: reply });
    addMsg(KAREN_PID, KAREN_NAME, KAREN_EMO, reply, false, null);
  } else if (kind === 'super') {
    // Broadcast the ask so every Karen on the mesh answers independently.
    const mid = myId + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
    bcast({ t: 'msg', id: myId, mid, txt: '@karens ' + prompt });
    addMsg(myId, myNm, '📡', '@karens ' + prompt, false, null);
    addMsg(null, null, null, 'asking every online Karen…', true);
    bcast({ t: 'karen:ask', prompt, askedBy: myId, at: Date.now() });
    // Our own Karen answers too
    if (engine) {
      const reply = await askLocalKaren(prompt);
      const rmid = KAREN_PID + ':' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
      bcast({ t: 'msg', id: KAREN_PID, mid: rmid, txt: reply });
      addMsg(KAREN_PID, KAREN_NAME, KAREN_EMO, reply, false, null);
    }
  }
}

// Count Karens on the mesh (including self if local Karen is ready).
export function karenCount() {
  let n = engine ? 1 : 0;
  for (const [, info] of pm) if (info.hasKaren) n++;
  return n;
}

// On boot, send a ping so any existing Karens announce back.
setTimeout(() => { try { bcast({ t: 'karen:ping', from: myId }); } catch (e) {} }, 1500);
