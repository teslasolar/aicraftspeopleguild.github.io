// ═══ Simonit · Monaco editor wrapper ═══
// Monaco loads via the AMD stub the shell HTML pulls from jsdelivr.
// We wait for window.require to resolve the editor, then mount it
// into #editor. Exports the minimal surface other panels need.

import { state, bus } from './shell.js';
import { readFile, writeFile } from './vfs.js';

const CDN_BASE = 'https://cdn.jsdelivr.net/npm/monaco-editor@0.45.0/min/vs';

let editor = null;

function langFromPath(p) {
  if (p.endsWith('.js'))   return 'javascript';
  if (p.endsWith('.html')) return 'html';
  if (p.endsWith('.css'))  return 'css';
  if (p.endsWith('.json')) return 'json';
  if (p.endsWith('.md'))   return 'markdown';
  return 'plaintext';
}

export function mountEditor(host) {
  return new Promise(resolve => {
    window.require.config({ paths: { vs: CDN_BASE } });
    window.require(['vs/editor/editor.main'], () => {
      editor = monaco.editor.create(host, {
        value:    readFile(state.activeFile),
        language: langFromPath(state.activeFile),
        theme:    'vs-dark',
        fontSize: 13,
        fontFamily: 'JetBrains Mono, SFMono-Regular, Consolas, monospace',
        minimap:  { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 2,
        wordWrap: 'on',
      });
      editor.onDidChangeModelContent(() => {
        writeFile(state.activeFile, editor.getValue());
        bus.emit('edit', { path: state.activeFile });
      });
      bus.emit('editor:ready');
      resolve(editor);
    });
  });
}

export function openFile(path) {
  if (!editor) return;
  state.activeFile = path;
  editor.setValue(readFile(path));
  monaco.editor.setModelLanguage(editor.getModel(), langFromPath(path));
  bus.emit('editor:file', { path });
}

export function getValue() { return editor?.getValue() ?? ''; }
export function setValue(v) {
  if (!editor) return;
  editor.setValue(v);
  writeFile(state.activeFile, v);
}
export function insertAtCursor(text) {
  if (!editor) return;
  const sel = editor.getSelection();
  editor.executeEdits('simonit-agent', [{ range: sel, text, forceMoveMarkers: true }]);
}
