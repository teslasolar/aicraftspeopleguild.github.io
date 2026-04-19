// ═══ Konomi browser · panel registry ═══
// Each tab is a Panel UDT instance — { render(bundle)->html, wire?(bundle, host) }.
// Register new tabs here so core.js can look them up by id.

import { overview }   from './overview.js';
import { base }       from './base.js';
import { standards }  from './standards.js';
import { live }       from './live.js';
import { crosswalks } from './crosswalks.js';
import { validate }   from './validate.js';

export const panels = { overview, base, standards, live, crosswalks, validate };
