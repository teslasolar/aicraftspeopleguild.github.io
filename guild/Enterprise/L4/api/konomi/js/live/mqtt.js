import { esc } from '../shell.js';

// Singleton MQTT session for the page. Sparkplug publishes share this
// same client so the operator only sees one connection in their broker.
export const mqttState = { client: null, connected: false, sessionTopic: null };

export const PUBLIC_BROKER = 'wss://broker.hivemq.com:8884/mqtt';

export function mqttPanel() {
  return `
    <div class="live-panel" id="mqtt">
      <h3>MQTT · live pub/sub <span class="tag">live</span></h3>
      <p class="sub">Real in-browser MQTT over WSS to <code>broker.hivemq.com</code> (public). Messages are visible to anyone on the same topic — the demo uses a random per-session prefix.</p>
      <div class="mqtt-bar">
        <span>broker:</span><code>${esc(PUBLIC_BROKER)}</code>
        <span class="pill off" id="mqtt-state">offline</span>
        <span id="mqtt-topic" style="opacity:.6"></span>
      </div>
      <div class="trig-row">
        <button class="trig-btn" id="mqtt-connect">connect</button>
        <button class="trig-btn ghost" id="mqtt-pub">publish "hello"</button>
        <button class="trig-btn danger" id="mqtt-disconnect" disabled>disconnect</button>
      </div>
      <div class="mqtt-ctl">
        <input id="mqtt-in" placeholder="payload to publish…" value='{"v":42,"q":192,"t":"now"}'>
        <button class="trig-btn" id="mqtt-send-custom" disabled>publish</button>
      </div>
      <div class="hist" data-role="hist-mqtt"></div>
    </div>`;
}

export function liveLog(host, role, html, cls = '') {
  const h = host.querySelector(`[data-role="hist-${role}"]`);
  if (!h) return;
  const t = new Date().toLocaleTimeString();
  h.insertAdjacentHTML('afterbegin',
    `<div><span class="ts">${t}</span> <span class="${cls}">${html}</span></div>`);
  while (h.childElementCount > 80) h.lastElementChild.remove();
}

export function pub(host, topic, payload) {
  if (!mqttState.client || !mqttState.connected) return;
  const data = typeof payload === 'string' ? payload : JSON.stringify(payload);
  mqttState.client.publish(topic, data, { qos: 0 });
  liveLog(host, topic.startsWith('spBv1.0/') ? 'sp' : 'mqtt',
          `→ <b>${esc(topic)}</b>  ${esc(data)}`, 'tx');
}

export function wireMqtt(host, onEnable) {
  const $q = s => host.querySelector('#' + s);
  const setState = on => {
    const el = $q('mqtt-state');
    el.textContent = on ? 'connected' : 'offline';
    el.className = 'pill ' + (on ? 'on' : 'off');
    $q('mqtt-pub').disabled = !on;
    $q('mqtt-send-custom').disabled = !on;
    $q('mqtt-disconnect').disabled = !on;
    $q('mqtt-connect').disabled    =  on;
    onEnable?.(on);
  };

  const sessionId = 'sess-' + Math.random().toString(36).slice(2, 8);
  const topicBase = `acg/konomi/${sessionId}`;
  mqttState.sessionTopic = topicBase;
  $q('mqtt-topic').textContent = `· topic-prefix: ${topicBase}/*`;

  $q('mqtt-connect').addEventListener('click', () => {
    if (!window.mqtt) { liveLog(host, 'mqtt', 'mqtt.js not loaded — retry in a second', 'err'); return; }
    liveLog(host, 'mqtt', `→ connecting to ${PUBLIC_BROKER}`, 'tx');
    const client = window.mqtt.connect(PUBLIC_BROKER, {
      clientId: 'acg-konomi-' + sessionId, keepalive: 30, reconnectPeriod: 0,
    });
    mqttState.client = client;
    client.on('connect', () => {
      mqttState.connected = true; setState(true);
      liveLog(host, 'mqtt', '✓ connected', 'ok');
      client.subscribe(`${topicBase}/#`, err => {
        if (err) liveLog(host, 'mqtt', 'subscribe err: ' + err.message, 'err');
        else     liveLog(host, 'mqtt', `✓ subscribed ${topicBase}/#`, 'ok');
      });
      client.subscribe('spBv1.0/acg/+/+/+', err => {
        if (!err) liveLog(host, 'mqtt', '✓ subscribed spBv1.0/acg/+/+/+  (Sparkplug lane)', 'ok');
      });
    });
    client.on('message', (topic, buf) => {
      const text = buf.toString();
      liveLog(host, topic.startsWith('spBv1.0/') ? 'sp' : 'mqtt',
              `← <b>${esc(topic)}</b>  ${esc(text.length > 200 ? text.slice(0, 200) + '…' : text)}`, 'rx');
    });
    client.on('error',   e  => liveLog(host, 'mqtt', 'err: ' + esc(e.message), 'err'));
    client.on('offline', () => { mqttState.connected = false; setState(false); liveLog(host, 'mqtt', 'offline', 'err'); });
    client.on('close',   () => { mqttState.connected = false; setState(false); });
  });

  $q('mqtt-disconnect').addEventListener('click', () => {
    if (mqttState.client) { mqttState.client.end(true); mqttState.client = null; }
    setState(false);
  });

  $q('mqtt-pub').addEventListener('click', () => pub(host, `${topicBase}/hello`, { msg: 'hello from konomi', ts: Date.now() }));

  $q('mqtt-send-custom').addEventListener('click', () => {
    const raw = $q('mqtt-in').value.trim();
    try { pub(host, `${topicBase}/custom`, JSON.parse(raw)); }
    catch { pub(host, `${topicBase}/custom`, raw); }
  });
}
