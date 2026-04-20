// ═══ Background fabric · scene ═══
// Three.js scene + renderer + camera. Mounts a fixed-position canvas
// at z-index -1 so everything else in the chat page paints on top.
// Loads Three.js from CDN once via a dynamic <script> tag; returns a
// promise that resolves when THREE is ready.

import { THREE_URL } from './catalog.js';

let loadPromise = null;
export function loadThree() {
  if (window.THREE) return Promise.resolve(window.THREE);
  if (loadPromise) return loadPromise;
  loadPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = THREE_URL; s.async = true;
    s.onload  = () => resolve(window.THREE);
    s.onerror = () => reject(new Error('failed to load Three.js from ' + THREE_URL));
    document.head.appendChild(s);
  });
  return loadPromise;
}

export function buildScene() {
  const THREE = window.THREE;
  const scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x0d1117, 0.07);

  const camera = new THREE.PerspectiveCamera(55, innerWidth / innerHeight, 0.1, 60);
  camera.position.set(0, 0.6, 6.2);

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  renderer.setSize(innerWidth, innerHeight);
  renderer.domElement.style.cssText =
    'position:fixed;inset:0;z-index:-1;pointer-events:none;width:100%;height:100%';
  document.body.prepend(renderer.domElement);

  // subtle rim light + fill so the orb reads even at low audio
  scene.add(new THREE.AmbientLight(0xffffff, 0.25));
  const key  = new THREE.PointLight(0xe3b341, 1.0, 30); key.position.set(4, 3, 3); scene.add(key);
  const fill = new THREE.PointLight(0x79c0ff, 0.6, 30); fill.position.set(-4, -2, 4); scene.add(fill);

  addEventListener('resize', () => {
    camera.aspect = innerWidth / innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(innerWidth, innerHeight);
  });

  return { scene, camera, renderer };
}
