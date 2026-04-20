// ═══ Voice fabric · peer-stream plumbing ═══
// Hooks into p2p.js via setVoicePrep: whenever a new RTCPeerConnection
// is minted (either as offerer via mkOffers, or as answerer via
// onOffer), we:
//   1) add the local audio track if mic is on — the SDP offer/answer
//      then carries it without a second renegotiation
//   2) wire pc.ontrack to mount the remote audio element and start a
//      level analyser so updPeers can draw a speaking dot
//
// All per-peer audio elements live in remotes (keyed by rid). They
// listen to the shared masterVolume so a single slider steers every
// remote's playback.

import { setVoicePrep } from '../p2p.js';
import { pm, updPeers } from '../peers.js';
import { getLocalStream } from './engine.js';
import { SPEAKING_RMS, SPEAKING_HOLD_MS, LEVEL_POLL_MS } from './catalog.js';

const remotes = new Map(); // rid -> { audio, stream, analyser, ctx }
const peerPcs = new Map(); // rid -> Set<RTCPeerConnection>
let   masterVolume = 0.8;
let   masterMuted  = false; // playback-mute (local monitor), not mic mute

export function setMasterVolume(v) {
  masterVolume = Math.max(0, Math.min(1, v));
  for (const [, r] of remotes) r.audio.volume = masterMuted ? 0 : masterVolume;
}
export function setMasterMuted(m) {
  masterMuted = !!m;
  for (const [, r] of remotes) r.audio.volume = masterMuted ? 0 : masterVolume;
}

function mountRemote(rid, stream) {
  let r = remotes.get(rid);
  if (r) { r.audio.srcObject = stream; r.stream = stream; return r; }
  const audio = document.createElement('audio');
  audio.autoplay = true;
  audio.srcObject = stream;
  audio.volume    = masterMuted ? 0 : masterVolume;
  document.body.appendChild(audio);

  const ctx = new (window.AudioContext || window.webkitAudioContext)();
  const src = ctx.createMediaStreamSource(stream);
  const an  = ctx.createAnalyser();
  an.fftSize = 512; an.smoothingTimeConstant = 0.6;
  src.connect(an);
  r = { audio, stream, analyser: an, ctx, lastAbove: 0, speaking: false };
  remotes.set(rid, r);
  startRemoteLevel(rid, r);
  // flag the peer info so updPeers can render "+🎤"
  const info = pm.get(rid);
  if (info) { info.hasVoice = true; updPeers(); }
  return r;
}

function startRemoteLevel(rid, r) {
  const buf = new Float32Array(r.analyser.fftSize);
  const tick = () => {
    if (!remotes.has(rid)) return;
    r.analyser.getFloatTimeDomainData(buf);
    let sum = 0;
    for (let i = 0; i < buf.length; i++) sum += buf[i] * buf[i];
    const rms = Math.sqrt(sum / buf.length);
    const now = performance.now();
    if (rms > SPEAKING_RMS) r.lastAbove = now;
    const sp = (now - r.lastAbove) < SPEAKING_HOLD_MS;
    if (sp !== r.speaking) {
      r.speaking = sp;
      const info = pm.get(rid);
      if (info) { info.speaking = sp; updPeers(); }
    }
    setTimeout(tick, LEVEL_POLL_MS);
  };
  tick();
}

function dropRemote(rid) {
  const r = remotes.get(rid);
  if (!r) return;
  try { r.audio.remove(); } catch (e) {}
  try { r.ctx?.close();   } catch (e) {}
  remotes.delete(rid);
  const info = pm.get(rid);
  if (info) { info.speaking = false; info.hasVoice = false; updPeers(); }
}

// Runs for every new pc — offer side at mint time (rid is the offer
// id then, not yet a real peer id) and answer side at on-offer time
// (rid is the real peer id). We cope with both.
function trackPc(rid, pc) {
  let set = peerPcs.get(rid);
  if (!set) { set = new Set(); peerPcs.set(rid, set); }
  set.add(pc);
}
function untrackPc(rid, pc) {
  const set = peerPcs.get(rid);
  if (!set) return;
  set.delete(pc);
  if (!set.size) peerPcs.delete(rid);
}

function voicePrep(pc, rid) {
  trackPc(rid, pc);
  // 1. add our audio track if mic is on
  const stream = getLocalStream();
  if (stream) {
    for (const t of stream.getAudioTracks()) {
      try { pc.addTrack(t, stream); } catch (e) {}
    }
  }
  // 2. wire remote-track arrival
  pc.addEventListener('track', ev => {
    const s = ev.streams?.[0] || new MediaStream([ev.track]);
    mountRemote(rid, s);
  });
  pc.addEventListener('connectionstatechange', () => {
    if (['failed', 'closed', 'disconnected'].includes(pc.connectionState)) {
      untrackPc(rid, pc);
      setTimeout(() => {
        const r = remotes.get(rid);
        if (r && !r.stream.active) dropRemote(rid);
      }, 500);
    }
  });
}

export function startStreams() { setVoicePrep(voicePrep); }

// Re-attach local track to every existing open pc. Called after mic
// starts so peers connected BEFORE the user hit mic still get audio.
// Renegotiation over the existing signalling path is non-trivial, but
// the 30 s re-announce cycle churns the pc pool so new connections
// carry audio from the start.
export function republishLocal() {
  const stream = getLocalStream();
  if (!stream) return;
  for (const [, set] of peerPcs) {
    for (const pc of set) {
      const existing = pc.getSenders?.().find(s => s.track?.kind === 'audio');
      if (existing) continue;
      for (const t of stream.getAudioTracks()) {
        try { pc.addTrack(t, stream); } catch (e) {}
      }
    }
  }
}
