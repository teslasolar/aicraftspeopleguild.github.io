import {$,log} from './ui.js';
import {TRACKERS,myId,myNm,myEm} from './config.js';
import {pm,updPeers,getSelfKaren} from './peers.js';
import {send} from './chat.js';
import {join,announce,wsReady,bcast} from './p2p.js';
import {summonKaren,onKarenChange} from './karen/index.js';
import './voice/index.js';
import {startVersion} from './version.js';
import {startAuth,onProfileChange,getProfile} from './auth.js';
import {startMonitor,toggle as toggleMonitor} from './scada/monitor.js';
import {SYS} from './scada/providers.js';
import {startSandboxBridge} from './sandbox-bridge.js';
import {startErrorCapture} from './errors.js';

function meLabel(){const p=getProfile();return p?.username||myNm}

$('mId').textContent=meLabel();

$('jBtn').onclick=()=>join($('rIn').value.trim()||'acg-guild');
$('cIn').onkeydown=e=>{if(e.key==='Enter')send()};
$('sBtn').onclick=send;
// #scBtn replaced with a link to /controls/; Ctrl+. still toggles the SCADA drawer below.

// keyboard: ctrl/cmd+. → toggle SCADA
window.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='.'){e.preventDefault();toggleMonitor()}});

log('⚒ ACG P2P Mesh v1.1','hi');
log('peer: '+myId+' (20 bytes ✓)','hi');
log('trackers: '+TRACKERS.length+' with auto-fallback','hi');

// sys.* tags
SYS.write('startedAt',Date.now(),{type:'DateTime'});
SYS.write('myId',myId);SYS.write('myNm',myNm);SYS.write('myEm',myEm);
SYS.write('trackerCount',TRACKERS.length,{type:'Counter'});
SYS.write('userAgent',navigator.userAgent);

startErrorCapture();
startMonitor();
startSandboxBridge();
updPeers();
startVersion();
startAuth();

onProfileChange(p=>{
  $('mId').textContent=meLabel();
  updPeers();
  // re-send hi to open peers so they pick up new name/avatar
  const me=getProfile();
  const hi=JSON.stringify({t:'hi',id:myId,nm:me?.username||myNm,em:myEm,av:me?.avatar||null,karen:getSelfKaren()});
  // One dc per peer — multiple would re-fire hi and show the peer
  // "joining" several times on the remote side.
  for(const[,info]of pm){
    for(const dc of info.dcs){
      if(dc.readyState==='open'){try{dc.send(hi)}catch(e){}break}
    }
  }
});

setTimeout(()=>join($('rIn').value.trim()||'acg-guild'),300);

// Karen · summon button in the peer list. Rendered by updPeers() and
// re-rendered every time Karen's state changes, so we re-wire on each
// fire. Once Karen is ready we re-broadcast hi so remote peers flip
// their hasKaren flag on us.
document.addEventListener('click',e=>{
  if(e.target&&e.target.id==='karenSummon')summonKaren();
});
onKarenChange(s=>{
  const st=$('karenStatus');
  if(st){
    if(s.status==='loading')     st.textContent='loading '+s.progress+'%';
    else if(s.status==='importing')st.textContent='importing runtime…';
    else if(s.status==='ready')  st.textContent='ready';
    else if(s.status==='no-gpu') st.textContent='no WebGPU';
    else if(s.status==='error')  st.textContent='error · see log';
    else                         st.textContent='idle';
  }
  if(s.ready){
    // re-announce hi with karen:true on every open dc so remote peers
    // update their hasKaren flag on us.
    const me=getProfile();
    const hi=JSON.stringify({t:'hi',id:myId,nm:me?.username||myNm,em:myEm,av:me?.avatar||null,karen:true});
    for(const[,info]of pm){
      for(const dc of info.dcs){
        if(dc.readyState==='open'){try{dc.send(hi)}catch(e){}break}
      }
    }
    updPeers();
  }
});

// periodic re-announce + uptime tick
setInterval(()=>{if(wsReady())announce()},30000);
setInterval(()=>SYS.write('uptimeSec',Math.floor((Date.now()-SYS.read('startedAt').value)/1000),{type:'Counter'}),1000);
