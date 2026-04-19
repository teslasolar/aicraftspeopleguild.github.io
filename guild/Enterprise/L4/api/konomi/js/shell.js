// ═══ Konomi browser · shell ═══
// Zero-dependency primitives used by every panel. Kept separate from
// core.js to break a circular import: panels.js imports panel modules,
// panel modules need `esc`, and core.js is the app entry — if `esc`
// lived there, core.js → panels.js → overview.js → core.js would trip
// a temporal-dead-zone error on the first panels.js evaluation.

export const state = { bundle: null, tab: 'overview' };

export const esc = s =>
  String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
