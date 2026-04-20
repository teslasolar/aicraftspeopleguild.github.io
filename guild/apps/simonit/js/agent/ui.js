// ═══ Simonit · agent UI ═══
// Right-pane chat. User types a prompt; agent engine replies; if the
// reply is a fenced code block, two buttons appear: "→ editor"
// replaces the current file, "▷ append" inserts at the cursor.

import { state, esc } from '../shell.js';
import { getValue, setValue, insertAtCursor } from '../editor.js';
import { summonAgent, askAgent, agentState, onAgentChange, extractCode } from './engine.js';

const history = []; // {who:'u'|'a', text, code?}

export function mountAgent(host) {
  render(host);
  onAgentChange(() => renderHead(host));

  host.addEventListener('click', async e => {
    const id = e.target?.id;
    if (id === 'agentSummon') {
      summonAgent();
    } else if (id === 'agentSend') {
      await onSend(host);
    } else if (e.target.dataset?.applyIdx != null) {
      const idx = +e.target.dataset.applyIdx;
      const code = history[idx]?.code;
      if (code) setValue(code);
    } else if (e.target.dataset?.appendIdx != null) {
      const idx = +e.target.dataset.appendIdx;
      const code = history[idx]?.code;
      if (code) insertAtCursor(code);
    }
  });
  host.addEventListener('keydown', e => {
    if (e.target.id === 'agentIn' && e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault(); onSend(host);
    }
  });
}

function renderHead(host) {
  const s = agentState();
  const state = s.status === 'ready' ? '✓ ready · ' + (s.model || '')
              : s.status === 'loading'   ? `⌛ loading ${s.progress || 0}%`
              : s.status === 'importing' ? '⌛ importing runtime'
              : s.status === 'no-gpu'    ? '✗ no WebGPU'
              : s.status === 'error'     ? '✗ ' + (s.text || 'error')
              : 'idle';
  const headEl = host.querySelector('.agent-state');
  if (headEl) headEl.textContent = state;
  const summonBtn = host.querySelector('#agentSummon');
  if (summonBtn) summonBtn.style.display = s.ready ? 'none' : '';
}

function render(host) {
  host.innerHTML = `
    <div class="agent-h">
      <span>🤖 Simon · coding agent</span>
      <span class="agent-state">idle</span>
    </div>
    <div class="agent-log" id="agentLog"></div>
    <div class="agent-in">
      <textarea id="agentIn" placeholder="ask simon — e.g. 'write a 3-button quiz'  (⌘/Ctrl-Enter to send)"></textarea>
      <div style="display:flex;flex-direction:column;gap:4px">
        <button id="agentSummon" class="btn primary">summon</button>
        <button id="agentSend"   class="btn">send</button>
      </div>
    </div>`;
  renderLog(host);
  renderHead(host);
}

function renderLog(host) {
  const log = host.querySelector('#agentLog');
  if (!history.length) {
    log.innerHTML = `<div class="agent-empty">click <b>summon</b>, then ask Simon to write something</div>`;
    return;
  }
  log.innerHTML = history.map((m, i) => {
    if (m.who === 'u') return `<div class="agent-msg u">${esc(m.text)}</div>`;
    const body = m.code
      ? `<pre>${esc(m.code)}</pre>
         <div style="display:flex;gap:4px;margin-top:4px">
           <button data-apply-idx="${i}"  class="btn primary" style="padding:3px 8px;font-size:10px">→ editor</button>
           <button data-append-idx="${i}" class="btn" style="padding:3px 8px;font-size:10px">▷ append</button>
         </div>`
      : esc(m.text);
    return `<div class="agent-msg a">${body}</div>`;
  }).join('');
  log.scrollTop = log.scrollHeight;
}

async function onSend(host) {
  const input = host.querySelector('#agentIn');
  const prompt = input.value.trim();
  if (!prompt) return;
  input.value = '';

  history.push({ who: 'u', text: prompt });
  renderLog(host);

  history.push({ who: 'a', text: '…thinking' });
  renderLog(host);
  const placeholder = history.length - 1;

  try {
    const reply = await askAgent(prompt, getValue());
    const code  = extractCode(reply);
    history[placeholder] = { who: 'a', text: reply, code };
  } catch (e) {
    history[placeholder] = { who: 'a', text: '(error: ' + e.message + ')' };
  }
  renderLog(host);
}
