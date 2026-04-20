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

// Mount mode:
//   - If `#orbWidget` exists in the DOM, we render INTO it (a compact
//     status widget ~180 px tall in the east dock).
//   - Otherwise we fall back to a fullscreen z-index:-1 canvas so
//     pages that don't host a widget still get the background fabric.
export function buildScene() {
  const THREE = window.THREE;
  const widget = document.getElementById('orbWidget');
  const mode = widget ? 'widget' : 'page';
  const host = widget || document.body;
  const W = () => widget ? widget.clientWidth  : innerWidth;
  const H = () => widget ? widget.clientHeight : innerHeight;

  const scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x0d1117, 0.07);

  const camera = new THREE.PerspectiveCamera(55, W() / H(), 0.1, 60);
  camera.position.set(0, 0.6, 6.2);

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  renderer.setSize(W(), H());
  renderer.domElement.style.cssText = mode === 'widget'
    ? 'width:100%;height:100%;display:block'
    : 'position:fixed;inset:0;z-index:-1;pointer-events:none;width:100%;height:100%';
  if (mode === 'widget') host.appendChild(renderer.domElement);
  else                   document.body.prepend(renderer.domElement);

  // subtle rim light + fill so the orb reads even at low audio
  scene.add(new THREE.AmbientLight(0xffffff, 0.25));
  const key  = new THREE.PointLight(0xe3b341, 1.0, 30); key.position.set(4, 3, 3); scene.add(key);
  const fill = new THREE.PointLight(0x79c0ff, 0.6, 30); fill.position.set(-4, -2, 4); scene.add(fill);

  const onResize = () => {
    camera.aspect = W() / H();
    camera.updateProjectionMatrix();
    renderer.setSize(W(), H());
  };
  if (mode === 'widget' && typeof ResizeObserver !== 'undefined') {
    new ResizeObserver(onResize).observe(widget);
  } else {
    addEventListener('resize', onResize);
  }

  return { scene, camera, renderer, mode };
}
