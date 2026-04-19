import { smPanel, wireSm, findSm } from './live/state-machine.js';
import { mqttPanel, wireMqtt }     from './live/mqtt.js';
import { sparkplugPanel, wireSparkplug } from './live/sparkplug.js';

// The Live tab is an orchestrator — it lays out panels and hands each
// one its own wiring. Each panel is independent so adding (say) a
// Modbus register-map demo is one new file + one import + one panel.
export const live = {
  id: 'live',
  label: 'live · run it',
  render(bundle) {
    const isa88 = findSm(bundle, 'isa88', 'BatchState');
    const isa18 = findSm(bundle, 'isa18', 'AlarmLifecycle') || findSm(bundle, 'isa18', 'AlarmState');
    return `
      <div class="live-grid">
        ${smPanel('ISA-88 · batch state machine', 'isa88-batch',
                  'Drive the Batch state model declared in Layer 3 · ISA-88. Triggers come straight from the paper\'s transitions.',
                  isa88)}
        ${smPanel('ISA-18.2 · alarm lifecycle', 'isa18-alarm',
                  'Annunciator flow for an industrial alarm. Change state by clicking the trigger buttons.',
                  isa18)}
        ${mqttPanel()}
        ${sparkplugPanel()}
      </div>`;
  },
  wire(bundle, host) {
    const isa88 = findSm(bundle, 'isa88', 'BatchState');
    const isa18 = findSm(bundle, 'isa18', 'AlarmLifecycle') || findSm(bundle, 'isa18', 'AlarmState');
    if (isa88) wireSm('isa88-batch', isa88, host);
    if (isa18) wireSm('isa18-alarm', isa18, host);
    const enableSp = wireSparkplug(host);
    wireMqtt(host, on => enableSp(on));
  },
};
