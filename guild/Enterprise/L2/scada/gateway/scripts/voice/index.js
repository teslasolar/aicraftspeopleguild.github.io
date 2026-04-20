// ═══ Voice fabric · facade ═══
// Single import surface. main.js pulls from here; the internals
// (engine / streams / ui / presence / catalog) can move without
// touching callers.

export { startMic, stopMic, toggleMute, onVoiceChange, voiceState } from './engine.js';
export { setMasterVolume, setMasterMuted, republishLocal } from './streams.js';
export { isPlaybackMuted } from './ui.js';

// Wire the three sub-systems (streams hook · presence events · UI).
// Order matters: streams first so setVoicePrep registers before any
// pc gets created; then presence; then UI last so buttons are bound.
import { startStreams } from './streams.js';
import { startPresence } from './presence.js';
import { startUi } from './ui.js';

startStreams();
startPresence();
startUi();
