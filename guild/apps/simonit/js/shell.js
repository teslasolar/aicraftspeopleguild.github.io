// ═══ Simonit · shell ═══
// Zero-dep primitives and the tiny pub/sub the app panels lean on.
// Lives apart from core.js so panel modules can import { state, bus }
// without a cycle through the entry file.

export const state = {
  activeFile: 'scratch.js',
  runKind:    'js',
  running:    false,
};

export const esc = s =>
  String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// Tiny event bus — the editor emits 'edit' on change, runners emit
// 'run:log' lines, the agent emits 'agent:delta' while streaming.
const listeners = new Map();
export const bus = {
  on(event, fn) {
    let set = listeners.get(event);
    if (!set) { set = new Set(); listeners.set(event, set); }
    set.add(fn);
    return () => set.delete(fn);
  },
  emit(event, payload) {
    const set = listeners.get(event); if (!set) return;
    for (const fn of set) try { fn(payload); } catch (e) { console.error(event, e); }
  },
};

// Terminal helpers — every runner + the agent share this. core.js
// wires them to the #term <pre>.
export function term(html, cls = '') {
  bus.emit('term:line', { html, cls });
}
