// ═══ Background fabric · catalog ═══
// Visual + behavioral constants. Mirrored by the BackgroundFabric UDT
// instance at L3/udts/background-fabric/instances/default.json so
// humans browsing the catalog see the same tuning as the runtime.

export const THREE_URL = 'https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js';

export const ORB_BASE_RADIUS = 1.6;
export const ORB_PULSE       = 0.45;    // max radial wobble from audio
export const ORB_BASE_COLOR  = 0x2b5a4a; // teal, matches gateway theme
export const ORB_HOT_COLOR   = 0xe3b341; // amber when someone's talking

export const VORTEX_COUNT = 260;
export const VORTEX_RADIUS = 3.2;

// Projectile kinds — each chat event maps to a color/size/speed.
export const PROJECTILES = {
  chat:   { color: 0x79c0ff, size: 0.11, speed: 9.5,  life: 2.4 },  // blue (plain chat)
  karen:  { color: 0xe3b341, size: 0.14, speed: 11.0, life: 2.6 },  // amber (Karen)
  voice:  { color: 0x3fb950, size: 0.09, speed: 13.0, life: 1.4 },  // green (voice peak)
  system: { color: 0xa371f7, size: 0.12, speed: 8.0,  life: 2.0 },  // purple (presence)
};

// How high the orb can pulse from voice rms. 0.05 rms == gentle beat,
// 0.25+ rms == big hit.
export const VOICE_SCALE = 2.2;
