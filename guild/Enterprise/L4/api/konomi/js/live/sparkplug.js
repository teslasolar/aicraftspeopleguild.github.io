import { mqttState, pub } from './mqtt.js';

// Sparkplug B topic conventions declared in the Konomi MQTT/Sparkplug
// standard. Payload is JSON (not protobuf) so you can read what's on
// the wire. Edge-node + device ids are generated per-session so two
// browsers on this page don't stomp on each other.

export function sparkplugPanel() {
  return `
    <div class="live-panel" id="sparkplug">
      <h3>Sparkplug B · edge node lifecycle <span class="tag">live</span></h3>
      <p class="sub">Uses the MQTT client below to publish <code>spBv1.0/acg/{NBIRTH,DBIRTH,DDATA,DDEATH}/&lt;nodeId&gt;/&lt;deviceId&gt;</code> — the topic conventions the paper declares. Payloads are JSON (not protobuf) for readability.</p>
      <div class="trig-row">
        <button class="trig-btn" id="sp-nbirth" disabled>NBIRTH (node online)</button>
        <button class="trig-btn" id="sp-dbirth" disabled>DBIRTH (device online)</button>
        <button class="trig-btn" id="sp-ddata"  disabled>DDATA  (metric value)</button>
        <button class="trig-btn danger" id="sp-ddeath" disabled>DDEATH (device offline)</button>
      </div>
      <div class="sub" style="font-family:var(--ff-mono);color:var(--dm);font-size:10.5px" id="sp-node">node: — · device: —</div>
      <div class="hist" data-role="hist-sp"></div>
    </div>`;
}

export function wireSparkplug(host) {
  const sessionId = mqttState.sessionTopic?.split('/').pop() || 'sess';
  const nodeId   = 'EON-' + sessionId.slice(-4).toUpperCase();
  const deviceId = 'DEV-PUMP-01';
  let   seq      = 0;

  host.querySelector('#sp-node').textContent = `node: ${nodeId} · device: ${deviceId}`;

  const spMetric = () => ({
    name: 'flow_rate', value: +(40 + Math.random() * 5).toFixed(2),
    unit: 'L/min', quality: 192, timestamp: Date.now(),
  });
  const env = (metrics, extra = {}) => ({ timestamp: Date.now(), seq: seq++, metrics, ...extra });

  const ids = ['sp-nbirth', 'sp-dbirth', 'sp-ddata', 'sp-ddeath'];
  const enableAll = on => ids.forEach(i => { host.querySelector('#' + i).disabled = !on; });

  host.querySelector('#sp-nbirth').addEventListener('click', () => pub(host,
    `spBv1.0/acg/NBIRTH/${nodeId}`,
    env([{ name: 'node_online', value: true, timestamp: Date.now() }], { bdSeq: 0 }),
  ));
  host.querySelector('#sp-dbirth').addEventListener('click', () => pub(host,
    `spBv1.0/acg/DBIRTH/${nodeId}/${deviceId}`,
    env([{ name: 'device_online', value: true, timestamp: Date.now() }, spMetric()]),
  ));
  host.querySelector('#sp-ddata').addEventListener('click', () => pub(host,
    `spBv1.0/acg/DDATA/${nodeId}/${deviceId}`,
    env([spMetric()]),
  ));
  host.querySelector('#sp-ddeath').addEventListener('click', () => pub(host,
    `spBv1.0/acg/DDEATH/${nodeId}/${deviceId}`,
    env([{ name: 'device_online', value: false, timestamp: Date.now() }]),
  ));

  // When MQTT connects / disconnects, flip the whole Sparkplug button
  // row along with it so operators can't hammer un-deliverable topics.
  return enableAll;
}
