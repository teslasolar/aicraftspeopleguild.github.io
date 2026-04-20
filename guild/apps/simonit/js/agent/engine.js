// ═══ Simonit · agent engine ═══
// Borrows the Karen WebLLM engine — same model, same IndexedDB cache,
// different system prompt tuned for code. Dynamic import so Simonit
// loads instantly even if the user never opens the agent pane.

const KAREN_ENGINE = '../../Enterprise/L2/scada/gateway/scripts/karen/engine.js';

const CODE_SYSTEM =
  "You are Simon, a terse coding assistant inside a browser IDE. " +
  "The user is editing a single file. When they ask for code, reply " +
  "with ONLY the code inside a fenced block (```js or ```html etc) — " +
  "no prose unless they ask for explanation. Keep snippets runnable " +
  "as-is. Match the language of the file they're editing when obvious.";

let karen = null;
let loadState = { status: 'idle' };
const listeners = new Set();
export function agentState() { return { ...loadState, ready: !!karen && karen.isReady() }; }
export function onAgentChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }
function fire() { for (const fn of listeners) try { fn(agentState()); } catch (e) {} }

export async function summonAgent() {
  if (karen && karen.isReady()) return;
  loadState = { status: 'importing' }; fire();
  karen = await import(KAREN_ENGINE);
  karen.onKarenChange(s => { loadState = s; fire(); });
  await karen.summonKaren();
}

export async function askAgent(prompt, context) {
  if (!karen || !karen.isReady()) return '(agent not ready — click summon)';
  const full = context ? `Current file (first 2000 chars):\n\`\`\`\n${context.slice(0, 2000)}\n\`\`\`\n\n${prompt}` : prompt;
  // Karen's askLocalKaren takes a string and returns a string.
  // We monkey-swap the system prompt by re-creating messages via the
  // engine's private chat path. Easiest path: reuse askLocalKaren but
  // wrap the prompt to set context for the model.
  const wrapped =
    `[SYSTEM OVERRIDE] ${CODE_SYSTEM}\n\n[USER] ${full}`;
  return karen.askLocalKaren(wrapped);
}

// Extract the FIRST fenced code block from a model reply, if any.
// Agents are told to reply code-only, but LLMs leak prose — this
// grabs just the block so the editor doesn't fill with commentary.
export function extractCode(text) {
  const m = /```(?:\w+)?\n([\s\S]*?)```/.exec(text);
  return m ? m[1].trim() : null;
}
