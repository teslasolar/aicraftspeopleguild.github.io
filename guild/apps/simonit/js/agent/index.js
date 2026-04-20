// ═══ Simonit · agent facade ═══
// Single import surface for the agent panel. core.js only touches
// mountAgent — engine details stay behind this barrel.

export { mountAgent } from './ui.js';
export { summonAgent, askAgent, agentState, onAgentChange } from './engine.js';
