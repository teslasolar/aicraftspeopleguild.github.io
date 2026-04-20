// ═══ Voice fabric · engine ═══
// Owns the local mic MediaStream + its analyser for self-speaking
// detection. Emits level events so the UI + remote peers can show
// speaking dots, and exposes toggleMute/setEnabled for the controls.

import { log } from '../ui.js';
import { AUDIO_CONSTRAINTS, SPEAKING_RMS, SPEAKING_HOLD_MS, LEVEL_POLL_MS } from './catalog.js';

let localStream = null;
let analyser    = null;
let ctx         = null;
let muted       = false;
let loadState   = { status: 'idle', text: '' };
const listeners = new Set();

export function voiceState() { return { ...loadState, active: !!localStream, muted }; }
export function onVoiceChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }
function fire() { for (const fn of listeners) try { fn(voiceState()); } catch (e) {} }

export function getLocalStream() { return localStream; }
export function isMuted() { return muted; }

export async function startMic() {
  if (localStream) return localStream;
  loadState = { status: 'asking', text: 'requesting mic…' }; fire();
  try {
    localStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS);
  } catch (e) {
    loadState = { status: 'error', text: e.message }; fire();
    log('mic denied / unavailable: ' + e.message, 'er');
    return null;
  }
  // analyser for self-speaking detection
  ctx = new (window.AudioContext || window.webkitAudioContext)();
  const src = ctx.createMediaStreamSource(localStream);
  analyser  = ctx.createAnalyser();
  analyser.fftSize = 512;
  analyser.smoothingTimeConstant = 0.6;
  src.connect(analyser);
  startLevelLoop();
  loadState = { status: 'on', text: 'live' }; fire();
  log('mic live', 'ok');
  return localStream;
}

export function stopMic() {
  if (!localStream) return;
  try { localStream.getTracks().forEach(t => t.stop()); } catch (e) {}
  try { ctx?.close(); } catch (e) {}
  localStream = null; analyser = null; ctx = null;
  loadState = { status: 'idle', text: '' }; fire();
}

export function toggleMute() {
  if (!localStream) return;
  muted = !muted;
  for (const t of localStream.getAudioTracks()) t.enabled = !muted;
  fire();
}

// ── level emitter ───────────────────────────────────────────────────
// Single RAF-ish loop that reads the analyser and publishes a level
// via the listener set. SPEAKING_HOLD_MS debounces the bool so the
// dot doesn't flicker off between syllables.
let speaking = false;
let lastAbove = 0;
function startLevelLoop() {
  const buf = new Float32Array(analyser.fftSize);
  const tick = () => {
    if (!analyser) return;
    analyser.getFloatTimeDomainData(buf);
    let sum = 0;
    for (let i = 0; i < buf.length; i++) sum += buf[i] * buf[i];
    const rms = Math.sqrt(sum / buf.length);
    const now = performance.now();
    const above = rms > SPEAKING_RMS && !muted;
    if (above) lastAbove = now;
    const sp = (now - lastAbove) < SPEAKING_HOLD_MS;
    if (sp !== speaking) { speaking = sp; fireLevel({ level: rms, speaking: sp }); }
    else                 { fireLevel({ level: rms, speaking: sp }); }
    setTimeout(tick, LEVEL_POLL_MS);
  };
  tick();
}
const levelListeners = new Set();
export function onLocalLevel(fn) { levelListeners.add(fn); return () => levelListeners.delete(fn); }
function fireLevel(ev) { for (const fn of levelListeners) try { fn(ev); } catch (e) {} }
