// ═══ Karen · engine ═══
// Owns the WebLLM runtime + a single model. Exposes ready-state plus
// ask(prompt) for synchronous-ish replies. Presence + chat commands
// live in sibling modules so this file only deals with the model.

import { log } from '../ui.js';
import { WEBLLM_URL, MODEL_CANDIDATES, SYSTEM_PROMPT } from './catalog.js';

let webllm = null;
let engine = null;
let modelId = null;
let loadState = { status: 'idle', progress: 0, text: '' };
const listeners = new Set();

export function karenState() {
  return { ...loadState, ready: !!engine, modelId };
}
export function onKarenChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }
function fire() { for (const fn of listeners) try { fn(karenState()); } catch (e) {} }
export function isReady() { return !!engine; }

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

    const catalog = webllm.prebuiltAppConfig?.model_list || [];
    const available = new Set(catalog.map(m => m.model_id));
    modelId = MODEL_CANDIDATES.find(id => available.has(id));
    if (!modelId) {
      const firstFew = catalog.slice(0, 6).map(m => m.model_id).join(', ');
      throw new Error(`no candidate model in WebLLM catalog (${catalog.length} models). ` +
        `first few available: ${firstFew || '(catalog empty)'}`);
    }

    loadState = { status: 'loading', progress: 0, text: `fetching ${modelId}…`, model: modelId };
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
    loadState = { status: 'ready', progress: 100, text: 'ready', model: modelId };
    fire();
    log('Karen ready ✓ · ' + modelId, 'ok');
  } catch (e) {
    loadState = { status: 'error', progress: 0, text: e.message };
    fire();
    log('Karen load err: ' + e.message, 'er');
  }
}

export async function askLocalKaren(prompt) {
  if (!engine) return '(Karen is not summoned yet — click "summon" in the peer list)';
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
