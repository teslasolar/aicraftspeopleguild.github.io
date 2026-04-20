// ═══ Background fabric · facade ═══
// Boot once on import. Loads Three.js via CDN, builds the scene,
// orb, vortex, projectile pool, and starts the animation loop.
// Everything is additive — if Three.js fails to load the page works
// exactly as before.

import { loadThree, buildScene } from './scene.js';
import { buildOrb }              from './orb.js';
import { buildVortex }           from './vortex.js';
import { buildProjectiles }      from './projectiles.js';
import { wireEvents, frameSample } from './events.js';
import { log } from '../ui.js';

async function start() {
  try {
    await loadThree();
  } catch (e) {
    log('background fabric disabled: ' + e.message, 'wr');
    return;
  }
  const { scene, camera, renderer } = buildScene();
  const orb         = buildOrb(scene);
  const vortex      = buildVortex(scene);
  const projectiles = buildProjectiles(scene, orb);

  wireEvents({ orb, vortex, projectiles });

  const clock = new window.THREE.Clock();
  (function tick() {
    const dt = Math.min(0.05, clock.getDelta());
    const t  = clock.elapsedTime;
    frameSample(orb, vortex);
    orb.update(dt, t);
    vortex.update(dt, t);
    projectiles.update(dt);
    renderer.render(scene, camera);
    requestAnimationFrame(tick);
  })();
  log('background fabric on · orb + vortex + projectiles', 'ok');
}

start();
