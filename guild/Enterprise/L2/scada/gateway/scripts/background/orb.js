// ═══ Background fabric · orb ═══
// Central audio-reactive sphere. A second wireframe sphere pulses
// outward from it when the mesh voice level peaks, and the emissive
// color lerps from teal-base to amber-hot so you can see who's
// speaking by glance alone.

import { ORB_BASE_RADIUS, ORB_PULSE, ORB_BASE_COLOR, ORB_HOT_COLOR, VOICE_SCALE } from './catalog.js';

export function buildOrb(scene) {
  const THREE = window.THREE;

  // Inner solid orb — icosahedron so vertex count stays reasonable
  const core = new THREE.Mesh(
    new THREE.IcosahedronGeometry(ORB_BASE_RADIUS, 3),
    new THREE.MeshStandardMaterial({
      color: ORB_BASE_COLOR,
      emissive: ORB_BASE_COLOR,
      emissiveIntensity: 0.5,
      roughness: 0.35,
      metalness: 0.25,
      flatShading: true,
      transparent: true,
      opacity: 0.85,
    }),
  );
  scene.add(core);

  // Outer wireframe halo — scales with voice
  const halo = new THREE.Mesh(
    new THREE.IcosahedronGeometry(ORB_BASE_RADIUS, 2),
    new THREE.MeshBasicMaterial({
      color: ORB_BASE_COLOR, wireframe: true, transparent: true, opacity: 0.35,
    }),
  );
  scene.add(halo);

  const pos = core.geometry.attributes.position;
  const ref = new Float32Array(pos.array.length);
  ref.set(pos.array); // original vertex positions for wobble

  let level = 0; let hotness = 0; let hitPulse = 0;

  return {
    mesh: core, halo,
    setLevel(v)      { level   = Math.min(1, level * 0.7 + v * 0.4); },
    setHot(on)       { hotness = Math.min(1, on ? hotness + 0.08 : hotness - 0.05); hotness = Math.max(0, hotness); },
    pulse(strength = 1) { hitPulse = Math.min(1.5, hitPulse + strength); },

    update(dt, t) {
      hitPulse *= Math.pow(0.05, dt); // decay hit ripple

      // vertex wobble — low-freq ripple driven by level + time
      const amp = ORB_PULSE * (0.15 + level * VOICE_SCALE + hitPulse * 0.5);
      const p = pos.array;
      for (let i = 0; i < p.length; i += 3) {
        const x = ref[i], y = ref[i+1], z = ref[i+2];
        const n = Math.sin(x*2.1 + t*1.3) * Math.cos(y*1.7 + t*0.9) * Math.sin(z*2.4 + t*1.1);
        p[i]   = x + x * n * amp * 0.08;
        p[i+1] = y + y * n * amp * 0.08;
        p[i+2] = z + z * n * amp * 0.08;
      }
      pos.needsUpdate = true;

      // emissive lerp between cool & hot
      const color = new window.THREE.Color().lerpColors(
        new window.THREE.Color(ORB_BASE_COLOR),
        new window.THREE.Color(ORB_HOT_COLOR),
        hotness,
      );
      core.material.emissive.copy(color);
      core.material.emissiveIntensity = 0.45 + level * 1.4 + hitPulse * 0.7;

      // halo lives a little bigger than the core; scales with voice
      const sc = 1.15 + level * 0.9 + hitPulse * 0.4;
      halo.scale.setScalar(sc);
      halo.rotation.y += dt * (0.15 + level * 1.0);
      halo.rotation.x += dt * 0.07;
      halo.material.opacity = 0.20 + level * 0.5;
      halo.material.color.copy(color);

      core.rotation.y -= dt * 0.08;
      core.rotation.x += dt * 0.04;

      // decay the instant reactions so a silent mesh settles down
      level *= Math.pow(0.65, dt);
      hotness *= Math.pow(0.4, dt);
    },
  };
}
