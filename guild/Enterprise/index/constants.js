// ── Timing ────────────────────────────────────────────────────────────
export const HEARTBEAT_MS = 5000;
export const REFRESH_MS   = 5000;
export const CLIENT_TTL   = 15000;
export const LS_TAB_KEY   = 'acg.ix.tab';

// ── Subsystem catalogue ───────────────────────────────────────────────
// Children render as a collapsible <details> group in the west dock.
// Every subsystem resolves to /{path}/ (trailing slash added by paintWest).
// Paths are written relative to the Enterprise root and organised by
// ISA-95 level (L1 plc · L2 scada/hmi · L3 db · L4 sandbox).
export const SUBSYSTEMS=[
  {sub:'chat',    glyph:'💬', name:'chat',    path:'L2/hmi/chat'},
  {sub:'scada',   glyph:'🖥️', name:'scada',   path:'L2/scada', children:[
    {sub:'errors',  glyph:'⚠',  name:'errors',  path:'L2/scada/errors'},
  ]},
  {sub:'auth',    glyph:'🔑', name:'auth',    path:'L2/scada/gateway/auth', children:[
    {sub:'auth.webrtc',     glyph:'📡', name:'webrtc',     path:'L2/scada/gateway/auth/webrtc'},
    {sub:'auth.webtorrent', glyph:'🌊', name:'webtorrent', path:'L2/scada/gateway/auth/webtorrent'},
    {sub:'auth.discord',    glyph:'🎮', name:'discord',    path:'L2/scada/gateway/auth/discord'},
    {sub:'auth.github',     glyph:'🐙', name:'github',     path:'L2/scada/gateway/auth/github'},
    {sub:'auth.google',     glyph:'🔎', name:'google',     path:'L2/scada/gateway/auth/google'},
  ]},
  {sub:'hmi',     glyph:'🖼', name:'hmi',     path:'L2/hmi'},
  {sub:'plc',     glyph:'🔧', name:'plc',     path:'L1/plc'},
  {sub:'db',      glyph:'🗄️', name:'db',      path:'L3/db'},
  {sub:'sandbox', glyph:'🧪', name:'sandbox', path:'L4/sandbox'},
];

// ── Shell nav links ───────────────────────────────────────────────────
// hrefs are relative to the Enterprise root (prepended with basePath at render time).
export const SHELL_LINKS=[
  {href:'../apps/p2p/',                          label:'⚒ mesh'},
  {href:'L2/hmi/chat/',                          label:'💬 chat'},
  {href:'L2/scada/gateway/gateway-log.html',     label:'⚠ log'},
  {href:'L2/scada/gateway/health.html',          label:'⚕ health'},
];
