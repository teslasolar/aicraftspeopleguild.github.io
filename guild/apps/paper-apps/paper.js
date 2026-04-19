// ── paper detail renderer ────────────────────────────────
// Mirrors the 3-tab Android app: Paper · Read · Try. The Try tab
// lands on a placeholder with the Kotlin source link for papers
// whose mini has not been JS-ported yet. Every mini available in
// JS lives in ./minis/<slug>.js and export-defaults a render fn.

const $ = s => document.querySelector(s);

async function load() {
  const slug = new URLSearchParams(location.search).get('slug');
  if (!slug) return fail('no ?slug=');

  const papers = await fetch('./papers.json', { cache: 'no-store' }).then(r => r.json());
  const p = papers.find(x => x.slug === slug);
  if (!p) return fail(`unknown slug: ${slug}`);

  document.documentElement.style.setProperty('--accent', p.theme_color_hex || '#1a5c4c');
  document.title = `⚒ ${p.title} · ACG Paper App`;
  $('#slug-chip').textContent = p.slug;

  const tabs = document.querySelectorAll('nav.tabs button');
  const render = async (name) => {
    tabs.forEach(b => b.classList.toggle('on', b.dataset.tab === name));
    const main = $('#app');
    main.className = '';
    main.classList.add(name);
    main.innerHTML = '';
    if (name === 'paper') return renderPaper(main, p);
    if (name === 'read')  return renderRead(main, p);
    if (name === 'try')   return renderTry(main, p);
  };
  tabs.forEach(b => b.addEventListener('click', () => render(b.dataset.tab)));

  const initial = new URLSearchParams(location.search).get('tab') || 'paper';
  render(initial);
}

function renderPaper(root, p) {
  root.innerHTML = `
    ${p.doc_number ? `<div class="doc">${esc(p.doc_number)}</div>` : ''}
    <h2>${esc(p.title)}</h2>
    <div class="byline">${esc([p.author, p.date].filter(Boolean).join('  ·  '))}</div>
    <div class="divider"></div>
    ${p.abstract ? `<p class="abstract">${esc(p.abstract)}</p>` : ''}
    ${p.paper_url ? `<a class="btn" href="${esc(p.paper_url)}" target="_blank">Open in browser ↗</a>` : ''}
    <p class="foot">part of the ACG whitepaper library · aicraftspeopleguild.github.io</p>
  `;
}

function renderRead(root, p) {
  const url = p.body_url || p.paper_url;
  if (!url) { root.innerHTML = `<p class="hint">no body_url on this paper</p>`; return; }
  const ifr = document.createElement('iframe');
  ifr.className = 'reader';
  ifr.src = url; ifr.loading = 'lazy';
  ifr.referrerPolicy = 'no-referrer';
  root.appendChild(ifr);
}

async function renderTry(root, p) {
  const box = document.createElement('div');
  box.className = 'mini-box';
  root.appendChild(box);

  // Try to load a JS port of the mini; fall back to the "see the
  // APK" message if none exists yet.
  const slug = p.slug;
  try {
    const mod = await import(`./minis/${slug}.js`);
    if (mod?.default) { await mod.default(box, p); return; }
  } catch (e) {
    // fall through to placeholder
  }

  const ktSrc = `https://github.com/teslasolar/aicraftspeopleguild.github.io/tree/main/phone/whitepapers/_minis/${encodeURIComponent(slug)}/android/app/src/main/java/com/aicraftspeopleguild/acg/papers/MiniRegistry.kt`;
  const apkPath = `https://github.com/teslasolar/aicraftspeopleguild.github.io/tree/main/phone/whitepapers/${encodeURIComponent(slug)}/android`;
  box.innerHTML = `
    <p>This paper's <b>Try</b> layer is a Kotlin mini that runs on the phone.
       Web port not yet shipped.</p>
    <p style="margin-top:12px">Either:</p>
    <ul style="margin:8px 0 8px 22px">
      <li>build the Android APK:
        <code>cd phone/whitepapers/${slug}/android && gradle assembleDebug</code></li>
      <li>read the Kotlin source for the mini:
        <a href="${ktSrc}" target="_blank">MiniRegistry.kt ↗</a></li>
      <li>clone the generated project:
        <a href="${apkPath}" target="_blank">${slug}/android/ ↗</a></li>
    </ul>
  `;
}

function fail(msg) { $('#app').innerHTML = `<p class="err">${esc(msg)}</p>`; }
const esc = s => String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');

load();
