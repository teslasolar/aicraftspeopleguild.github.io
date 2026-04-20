// ═══ Voice fabric · catalog ═══
// Constants + tuning for the voice layer. Kept separate so the UDT
// instance JSON (L3/udts/voice-fabric/instances/default.json) is the
// single source of truth for humans; this file is the runtime view
// that the other voice modules read.

export const AUDIO_CONSTRAINTS = {
  audio: {
    echoCancellation: true,
    noiseSuppression: true,
    autoGainControl:  true,
    channelCount: 1,
    sampleRate:   48000,
  },
  video: false,
};

// Speaking threshold — RMS of mic / remote analyser needs to cross
// this for at least SPEAKING_HOLD_MS before the peer's voice dot
// lights up. Stops the dot flickering on typing / fan noise.
export const SPEAKING_RMS    = 0.04;
export const SPEAKING_HOLD_MS = 140;

// Analyser poll rate. 20 Hz is plenty for a speaking indicator and
// cheap enough to run across every peer simultaneously.
export const LEVEL_POLL_MS = 50;
