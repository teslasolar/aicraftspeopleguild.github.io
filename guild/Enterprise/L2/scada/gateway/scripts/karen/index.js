// ═══ Karen · facade ═══
// Single import surface. main.js + chat.js pull from here so the
// internal layout (engine / presence / commands / catalog) can change
// without touching callers.

export { summonKaren, onKarenChange, karenState, isReady, askLocalKaren } from './engine.js';
export { startPresence, karenCount, announceKaren,
         KAREN_PID, KAREN_NAME, KAREN_EMO } from './presence.js';
export { handleKarenCommand } from './commands.js';

// Mesh event handlers + the boot-time ping fire as a side effect of
// importing the presence module. Call explicitly so the lifecycle is
// visible in main.js rather than magical.
import { startPresence } from './presence.js';
startPresence();
