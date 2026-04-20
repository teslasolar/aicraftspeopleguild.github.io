import {$,esc,log} from './ui.js';
import {myId,myNm,myEm} from './config.js';
import {addMsg} from './chat.js';
import {getProfile} from './auth.js';
import {PEERS,ROOM,CHAT} from './scada/providers.js';
import {mkUDT} from './scada/udt.js';

// Hook slot: sibling modules (voice fabric, future fabrics) register a
// render callback here; peers.js invokes each at the tail of updPeers
// after the standard peer list is rendered. Keeps peers.js ignorant
// of audio / video concerns without circular imports.
const _extras=[];
export function registerPeerListExtra(fn){_extras.push(fn);updPeers();return()=>{const i=_extras.indexOf(fn);if(i>=0)_extras.splice(i,1)}}

export const pm=new Map();

// Karen presence. Each peer optionally has hasKaren=true on their
// info; the local flag lives here so updPeers() can count self + peers
// uniformly. Flipped by karen.js via the exported setters below.
let selfHasKaren=false;
export function setSelfKaren(on){selfHasKaren=!!on;updPeers()}
export function setPeerKaren(pid,on){
  if(!pm.has(pid))pm.set(pid,{name:pid.slice(-8),emoji:'⚒️',avatar:null,dcs:new Set(),connectedAt:Date.now(),msgsIn:0,msgsOut:0,lastSeen:Date.now()});
  pm.get(pid).hasKaren=!!on;
  updPeers();
}
export function getSelfKaren(){return selfHasKaren}

// Voice fabric state — selfHasVoice is "my mic is live". Peer voice
// state gets surfaced via setPeerVoice (remote muted themselves vs.
// went live). Speaking dots are set directly on info.speaking by
// streams.js (local level) + engine.js (self).
let selfHasVoice=false;
export function setSelfVoice(on){selfHasVoice=!!on;updPeers()}
export function getSelfVoice(){return selfHasVoice}
export function setPeerVoice(pid,on){
  if(!pm.has(pid))pm.set(pid,{name:pid.slice(-8),emoji:'⚒️',avatar:null,dcs:new Set(),connectedAt:Date.now(),msgsIn:0,msgsOut:0,lastSeen:Date.now()});
  pm.get(pid).hasVoice=!!on;
  updPeers();
}

// Dedup recently-seen chat message ids. Bounded LRU (~200 entries) so
// it stays tiny. We key by m.mid — if a peer has two open dcs (one
// freshly reconnected before the old one closed), the same message
// arrives twice and we drop the second copy silently.
const seenMsgIds=new Set();
const seenMsgIdOrder=[];
function sawMsg(mid){
  if(!mid)return false;
  if(seenMsgIds.has(mid))return true;
  seenMsgIds.add(mid);seenMsgIdOrder.push(mid);
  if(seenMsgIdOrder.length>200){const old=seenMsgIdOrder.shift();seenMsgIds.delete(old)}
  return false;
}

// ── Plugin hook: additional apps (whiteboard, etc) can register
// handlers for custom `t:<type>` messages. Chat already has `hi` + `msg`
// baked in; plugins receive any *other* type via this dispatch.
const _extraHandlers=new Map();
export function registerPeerHandler(type,fn){_extraHandlers.set(type,fn);return()=>_extraHandlers.delete(type)}

function meCard(){
  const p=getProfile();
  return {name:p?.username||myNm,emoji:myEm,avatar:p?.avatar||null};
}

function avatarHtml(info){
  return info.avatar
    ?`<div class="pa"><img src="${esc(info.avatar)}" alt=""></div>`
    :`<div class="pa">${esc(info.emoji||'⚒️')}</div>`;
}

function publishPeer(pid){
  const info=pm.get(pid);
  if(!info||info.dcs.size===0){PEERS.del(pid);return}
  PEERS.write(pid,mkUDT('Peer',{
    id:pid,name:info.name,emoji:info.emoji,avatar:info.avatar,
    state:'open',channels:info.dcs.size,connectedAt:info.connectedAt,
    msgsIn:info.msgsIn,msgsOut:info.msgsOut,lastSeen:info.lastSeen,
  }));
}

function publishPeerCount(){
  let n=0;for(const[,i]of pm)if(i.dcs.size>0)n++;
  ROOM.write('peerCount',n+1,{type:'Counter'});
}

export function wire(dc,rid){
  dc.binaryType='arraybuffer';
  dc.onopen=()=>{
    log('✓ channel open → '+rid.slice(-8),'ok');
    if(!pm.has(rid))pm.set(rid,{name:rid.slice(-8),emoji:'⚒️',avatar:null,dcs:new Set(),connectedAt:Date.now(),msgsIn:0,msgsOut:0,lastSeen:Date.now()});
    const info=pm.get(rid);info.dcs.add(dc);info.lastSeen=Date.now();
    updPeers();publishPeer(rid);
    addMsg(null,null,null,'⚒️ '+rid.slice(-8)+' connected',true);
    const me=meCard();
    try{dc.send(JSON.stringify({t:'hi',id:myId,nm:me.name,em:me.emoji,av:me.avatar,karen:selfHasKaren}));info.msgsOut++}catch(e){}
  };
  dc.onclose=()=>{
    if(pm.has(rid)){const info=pm.get(rid);info.dcs.delete(dc);
      if(info.dcs.size===0){
        addMsg(null,null,null,info.emoji+' '+info.name+' left',true);
        pm.delete(rid);PEERS.del(rid);
      }else publishPeer(rid);
    }
    updPeers();
  };
  dc.onerror=e=>log('dc err: '+rid.slice(-8),'er');
  dc.onmessage=e=>{try{
    const m=JSON.parse(e.data);
    if(m.t==='hi'){
      const pid=m.id||rid;
      if(!pm.has(pid))pm.set(pid,{name:m.nm||pid.slice(-8),emoji:m.em||'⚒️',avatar:m.av||null,dcs:new Set(),connectedAt:Date.now(),msgsIn:0,msgsOut:0,lastSeen:Date.now()});
      const info=pm.get(pid);
      info.name=m.nm||info.name;info.emoji=m.em||info.emoji;info.avatar=m.av||info.avatar;
      if('karen' in m)info.hasKaren=!!m.karen;
      info.dcs.add(dc);info.lastSeen=Date.now();info.msgsIn++;
      updPeers();publishPeer(pid);
    }else if(m.t==='msg'){
      // Drop duplicate deliveries (multi-dc crossfire during a tracker
      // reconnect) silently — no log line, no counter bump, no UI row.
      if(sawMsg(m.mid))return;
      const pid=m.id||rid;const info=pm.get(pid)||{name:pid.slice(-8),emoji:'⚒️',avatar:null};
      if(pm.has(pid)){pm.get(pid).msgsIn++;pm.get(pid).lastSeen=Date.now();publishPeer(pid)}
      CHAT.inc('msgsIn');CHAT.write('lastMsgAt',Date.now(),{type:'DateTime'});
      addMsg(pid,info.name,info.emoji,m.txt,false,info.avatar);
    }
    // Plugin observers: fire for every typed message (including `hi`
    // and `msg`) so apps like terminal/whiteboard can tap into chat
    // traffic alongside the built-in dispatcher.
    if(m.t && _extraHandlers.has(m.t)){
      try{_extraHandlers.get(m.t)(m,rid)}catch(er){log('plugin('+m.t+') err: '+er.message,'er')}
    }
  }catch(er){log('parse err','er')}};
}

export function updPeers(){
  const el=$('pList');if(!el)return;el.innerHTML='';
  const me=meCard();
  const s=document.createElement('div');s.className='pc me';
  s.innerHTML=`${avatarHtml(me)}<div class="pn2">${esc(me.name)}<small>you</small></div>`;
  el.appendChild(s);
  // Karen virtual-peer row. Count is self (if summoned) + peers with
  // hasKaren=true — gives operators a "×N super-karen" tally they can
  // fan out to with @karens in chat. Rendering always happens so the
  // summon button stays findable even before the model downloads.
  const karens=(selfHasKaren?1:0)+[...pm.values()].filter(i=>i.hasKaren).length;
  const k=document.createElement('div');
  k.className='pc karen'+(selfHasKaren?' on':' off');
  k.id='karenRow';
  const badge=karens>0?`<span class="karen-badge">×${karens}</span>`:'';
  const action=selfHasKaren
    ? `<small>ready · ${karens} online</small>`
    : `<small id="karenStatus">idle</small>`;
  k.innerHTML=`<div class="pa karen-pa">🤖</div>`
    +`<div class="pn2">Karen ${badge}${action}</div>`
    +(selfHasKaren?'':`<button id="karenSummon" class="karen-btn">summon</button>`);
  el.appendChild(k);
  let n=0;
  for(const[,info]of pm){if(info.dcs.size===0)continue;n++;
    const c=document.createElement('div');c.className='pc'+(info.speaking?' speaking':'');
    const karenMark=info.hasKaren?' <small style="color:var(--tl)">+🤖</small>':'';
    const voiceMark=info.hasVoice?' <small style="color:var(--am)">+🎤</small>':'';
    const dot=info.speaking?`<span class="voice-dot on"></span>`:'';
    c.innerHTML=`${avatarHtml(info)}<div class="pn2">${dot}${esc(info.name)}${karenMark}${voiceMark}</div>`;
    el.appendChild(c);
  }
  const nP=$('nP');if(nP)nP.textContent=n+1;
  const pLb=$('pLb');if(pLb)pLb.textContent=(n+1)+' online';
  publishPeerCount();

  // Extras (voice fabric, future fabrics): each is invoked with the
  // pList element so it can append its own chrome after the peers.
  for(const fn of _extras){try{fn(el)}catch(e){log('peer extra err: '+e.message,'er')}}
}
