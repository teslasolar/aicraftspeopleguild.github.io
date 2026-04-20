// ═══ Voice fabric · UI ═══
// Compact mic-control panel appended to the peer list via the
// registerPeerListExtra hook. Renders a virtual "mic" row with status
// (idle / asking / live / muted / error) plus a master-volume slider.
// Per-peer speaking dots + +🎤 marks are painted by peers.js itself.

import { startMic, stopMic, toggleMute, onVoiceChange, voiceState, onLocalLevel } from './engine.js';
import { setMasterVolume, setMasterMuted, republishLocal } from './streams.js';
import { setSelfVoice, updPeers, registerPeerListExtra } from '../peers.js';

let playMuted = false;

export function startUi() {
  registerPeerListExtra(el => {
    el.insertAdjacentHTML('beforeend',
      `<div class="pc voice">${voiceRow()}</div>` +
      `<div class="pc voice-vol-row">${volRow()}</div>`);
  });

  document.addEventListener('click', e => {
    const id = e.target?.id;
    if (id === 'voiceStart')     startVoice();
    else if (id === 'voiceMute') toggleMute();
    else if (id === 'voiceStop') stopVoice();
    else if (id === 'voiceMon')  togglePlaybackMute();
  });
  document.addEventListener('input', e => {
    if (e.target?.id === 'voiceVol') setMasterVolume(+e.target.value / 100);
  });

  onVoiceChange(() => { updPeers(); });
  onLocalLevel(({ speaking }) => {
    const changed = (window.__selfSpeaking !== speaking);
    window.__selfSpeaking = speaking;
    if (changed) updPeers();
  });
}

async function startVoice() {
  const stream = await startMic();
  if (stream) { setSelfVoice(true); republishLocal(); }
}
function stopVoice() { stopMic(); setSelfVoice(false); }
function togglePlaybackMute() { playMuted = !playMuted; setMasterMuted(playMuted); updPeers(); }
export function isPlaybackMuted() { return playMuted; }

function voiceRow() {
  const s = voiceState();
  let label, actions;
  if (s.status === 'idle' || s.status === 'error') {
    label   = s.status === 'error' ? 'mic · err · ' + (s.text || '?') : 'Mic · click start';
    actions = `<button id="voiceStart" class="karen-btn">start</button>`;
  } else if (s.status === 'asking') {
    label   = 'asking…';
    actions = '';
  } else {
    label   = s.muted ? 'Mic · MUTED' : 'Mic · LIVE';
    actions =
      `<button id="voiceMute" class="karen-btn">${s.muted ? 'unmute' : 'mute'}</button>` +
      `<button id="voiceStop" class="karen-btn" style="margin-left:4px">stop</button>`;
  }
  const dotCls = window.__selfSpeaking && !s.muted && s.active ? 'voice-dot on' : 'voice-dot';
  return `<div class="pa voice-pa">🎤</div>` +
         `<div class="pn2" style="flex:1"><span class="${dotCls}"></span>${label}</div>` +
         actions;
}

function volRow() {
  return `<div class="pa voice-vol-pa">${playMuted ? '🔇' : '🔊'}</div>` +
         `<div class="pn2" style="flex:1">playback</div>` +
         `<button id="voiceMon" class="karen-btn" style="margin-right:4px">${playMuted ? 'unmute' : 'mute'}</button>` +
         `<input id="voiceVol" type="range" min="0" max="100" value="80" style="width:60px">`;
}
