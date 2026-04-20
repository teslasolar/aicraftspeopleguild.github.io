// ═══ Background fabric · projectiles ═══
// Small glowing balls shot from the camera rim toward the orb. Each
// chat message emits one; Karen replies and super-karen asks splash
// extras. Pooled so spam doesn't allocate garbage.

import { PROJECTILES, ORB_BASE_RADIUS } from './catalog.js';

export function buildProjectiles(scene, orb) {
  const THREE = window.THREE;
  const active = [];  // { mesh, vel, life, kind, impacted }
  const pool   = [];

  function acquire(kind) {
    const spec = PROJECTILES[kind] || PROJECTILES.chat;
    let p = pool.pop();
    if (!p) {
      const mesh = new THREE.Mesh(
        new THREE.SphereGeometry(1, 12, 12),
        new THREE.MeshBasicMaterial({ color: spec.color, transparent: true, opacity: 0.95,
                                      blending: THREE.AdditiveBlending, depthWrite: false }),
      );
      scene.add(mesh);
      p = { mesh, vel: new THREE.Vector3(), life: 0, kind };
    }
    p.kind = kind;
    p.mesh.material.color.setHex(spec.color);
    p.mesh.material.opacity = 0.95;
    p.mesh.scale.setScalar(spec.size);
    return p;
  }

  function release(p) {
    p.mesh.visible = false;
    pool.push(p);
  }

  function spawn(kind) {
    const spec = PROJECTILES[kind] || PROJECTILES.chat;
    const p = acquire(kind);
    // launch from a random point on a shell around the orb, aimed at
    // the orb with a bit of spin so they curve in
    const th = Math.random() * Math.PI * 2;
    const ph = (Math.random() - 0.5) * Math.PI * 0.6;
    const r  = 8 + Math.random() * 2.5;
    p.mesh.position.set(
      r * Math.cos(ph) * Math.cos(th),
      r * Math.sin(ph),
      r * Math.cos(ph) * Math.sin(th),
    );
    const dir = p.mesh.position.clone().multiplyScalar(-1).normalize();
    // add a bit of orbital tangent so they swoosh
    const tangent = new THREE.Vector3(-dir.z, 0, dir.x).multiplyScalar(0.25 + Math.random() * 0.4);
    p.vel.copy(dir.multiplyScalar(spec.speed).add(tangent));
    p.life = spec.life;
    p.mesh.visible = true;
    active.push(p);
  }

  function update(dt) {
    for (let i = active.length - 1; i >= 0; i--) {
      const p = active[i];
      p.mesh.position.addScaledVector(p.vel, dt);
      // gravity toward orb — mild
      const pull = p.mesh.position.clone().multiplyScalar(-2.5 * dt);
      p.vel.add(pull);
      p.life -= dt;

      if (p.mesh.position.length() < ORB_BASE_RADIUS * 1.1) {
        // impact — pulse the orb, fade the projectile
        orb.pulse(p.kind === 'karen' ? 0.45 : 0.28);
        p.life = Math.min(p.life, 0.18);
      }
      if (p.life <= 0) {
        active.splice(i, 1);
        release(p);
        continue;
      }
      // trail fade — opacity proportional to remaining life
      const spec = PROJECTILES[p.kind] || PROJECTILES.chat;
      p.mesh.material.opacity = Math.min(1, p.life / spec.life * 1.2);
      const s = spec.size * (0.6 + 0.6 * (p.life / spec.life));
      p.mesh.scale.setScalar(s);
    }
  }

  return { spawn, update, get count() { return active.length; } };
}
