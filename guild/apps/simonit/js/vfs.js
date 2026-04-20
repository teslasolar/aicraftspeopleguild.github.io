// ═══ Simonit · virtual filesystem ═══
// localStorage-backed file store. Single-file for the MVP but the
// API is multi-file ready so the tree panel can grow without
// touching callers.

const KEY = 'acg.simonit.vfs.v1';

const DEFAULT_JS = `// Simonit · scratch.js
// Click ▶ run  —  output lands in the terminal tab.
// Drop "HTML preview" in the run-kind dropdown for live <iframe>.

console.log("hello from simonit");

for (let i = 0; i < 3; i++) {
  console.log(\`loop \${i}\`);
}
`;

const DEFAULT_HTML = `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>preview</title></head>
<body style="font-family:sans-serif;padding:24px">
  <h1>hello from simonit · preview</h1>
  <button onclick="alert('clicked')">click me</button>
</body></html>`;

function load() {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {}
  return {
    files: {
      'scratch.js':   DEFAULT_JS,
      'scratch.html': DEFAULT_HTML,
    },
    order: ['scratch.js', 'scratch.html'],
  };
}

let fs = load();
function save() { try { localStorage.setItem(KEY, JSON.stringify(fs)); } catch (e) {} }

export function listFiles() { return [...fs.order]; }
export function readFile(path) { return fs.files[path] ?? ''; }
export function writeFile(path, content) {
  fs.files[path] = content;
  if (!fs.order.includes(path)) fs.order.push(path);
  save();
}
export function deleteFile(path) {
  delete fs.files[path];
  fs.order = fs.order.filter(p => p !== path);
  save();
}
export function reset() { fs = load(); save(); }
