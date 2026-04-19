// ═══ Karen · model catalog ═══
// Candidate model ids, smallest-first. WebLLM drops/renames entries
// between releases, so we probe prebuiltAppConfig.model_list at load
// time and pick the first one that matches. These ids were verified
// present in @mlc-ai/web-llm v0.2.82 — if you upgrade the runtime and
// nothing loads, check `webllm.prebuiltAppConfig.model_list` in the
// console to see what's actually shipped.

export const WEBLLM_URL = 'https://esm.run/@mlc-ai/web-llm';

export const MODEL_CANDIDATES = [
  // smallest first — Qwen 0.5B at q4 weighs ~440 MB cached in IndexedDB
  'Qwen2.5-0.5B-Instruct-q4f16_1-MLC',
  'Qwen2.5-0.5B-Instruct-q0f16-MLC',
  'Llama-3.2-1B-Instruct-q4f16_1-MLC',
  'Qwen2.5-1.5B-Instruct-q4f16_1-MLC',
];

// Karen's persona. Short and even-handed so super-Karen replies don't
// all sound identical.
export const SYSTEM_PROMPT =
  "You are Karen, a concise, friendly AI assistant running locally in " +
  "a peer-to-peer browser chat. Keep replies short (1-3 sentences). " +
  "If multiple Karens are asked the same question, each answers " +
  "independently from their own local model — it's OK for answers to " +
  "differ; the user wants to see the spread.";
