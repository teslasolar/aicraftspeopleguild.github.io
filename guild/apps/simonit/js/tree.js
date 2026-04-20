// ═══ Simonit · file tree ═══
// Lists VFS files. Click → open in the editor. "+ new" prompts
// for a name; "del" wipes the currently-active file. MVP is
// single-folder; a future rev can nest by path.

import { state, bus } from './shell.js';
import { listFiles, writeFile, deleteFile } from './vfs.js';
import { openFile } from './editor.js';

export function mountTree(host) {
  render(host);
  bus.on('editor:file', () => render(host));
  host.addEventListener('click', e => {
    const entry = e.target.closest('[data-path]');
    if (entry)            openFile(entry.dataset.path);
    else if (e.target.id === 'treeNew') {
      const name = prompt('new file name:', 'untitled.js');
      if (!name) return;
      writeFile(name, '// ' + name + '\n');
      openFile(name);
      render(host);
    }
    else if (e.target.id === 'treeDel') {
      if (!confirm('delete ' + state.activeFile + '?')) return;
      deleteFile(state.activeFile);
      const remaining = listFiles();
      if (remaining.length) openFile(remaining[0]);
      render(host);
    }
  });
}

function render(host) {
  const files = listFiles();
  host.innerHTML =
    `<div class="tree-h">files</div>` +
    `<div class="tree-list">` +
      files.map(p =>
        `<div class="tree-entry ${p === state.activeFile ? 'active' : ''}" data-path="${p}">${p}</div>`,
      ).join('') +
    `</div>` +
    `<div class="tree-actions">
       <button id="treeNew" class="btn">+ new</button>
       <button id="treeDel" class="btn">del</button>
     </div>`;
}
