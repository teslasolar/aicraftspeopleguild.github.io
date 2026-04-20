// ═══ Background fabric · vortex ═══
// Particle halo orbiting the orb. Each particle spins on its own slow
// orbit; the mass rotates faster when voice is loud and slower when
// the mesh is quiet, giving the fabric a "breath".

import { VORTEX_COUNT, VORTEX_RADIUS } from './catalog.js';

export function buildVortex(scene) {
  const THREE = window.THREE;
  const geo = new THREE.BufferGeometry();
  const positions = new Float32Array(VORTEX_COUNT * 3);
  const orbit     = new Float32Array(VORTEX_COUNT * 3); // radius · theta · phi
  for (let i = 0; i < VORTEX_COUNT; i++) {
    const r = VORTEX_RADIUS * (0.85 + Math.random() * 0.4);
    const t = Math.random() * Math.PI * 2;
    const p = (Math.random() - 0.5) * Math.PI * 0.9;
    orbit[i*3]   = r;
    orbit[i*3+1] = t;
    orbit[i*3+2] = p;
    positions[i*3]   = r * Math.cos(p) * Math.cos(t);
    positions[i*3+1] = r * Math.sin(p);
    positions[i*3+2] = r * Math.cos(p) * Math.sin(t);
  }
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));

  const mat = new THREE.PointsMaterial({
    color: 0x79c0ff,
    size: 0.055,
    transparent: true,
    opacity: 0.75,
    sizeAttenuation: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });
  const points = new THREE.Points(geo, mat);
  scene.add(points);

  let speed = 0;
  return {
    mesh: points,
    setSpeed(v) { speed = v; },
    update(dt, t) {
      const pos = geo.attributes.position.array;
      const baseSpin = 0.1 + speed * 0.9;
      for (let i = 0; i < VORTEX_COUNT; i++) {
        orbit[i*3+1] += dt * baseSpin * (0.6 + (i % 5) * 0.15);
        const r = orbit[i*3], th = orbit[i*3+1], ph = orbit[i*3+2];
        pos[i*3]   = r * Math.cos(ph) * Math.cos(th);
        pos[i*3+1] = r * Math.sin(ph) + Math.sin(t * 0.5 + i) * 0.05;
        pos[i*3+2] = r * Math.cos(ph) * Math.sin(th);
      }
      geo.attributes.position.needsUpdate = true;
      points.rotation.y += dt * 0.03;
    },
  };
}
