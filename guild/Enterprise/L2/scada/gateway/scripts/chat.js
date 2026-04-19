import {$,esc,log} from './ui.js';
import {myId,myNm,myEm} from './config.js';
import {bcast} from './p2p.js';
import {getProfile} from './auth.js';
import {CHAT} from './scada/providers.js';

export function addMsg(pid,nm,em,txt,sys=false,avatar=null){
  const me=pid===myId;const d=document.createElement('div');d.className='ms'+(me?' me':'');
  if(sys){d.innerHTML=`<div class="mb sy">${esc(txt)}</div>`}
  else{
    const who=avatar
      ?`<img class="mav" src="${esc(avatar)}" alt=""> ${esc(nm)}`
      :`${esc(em)} ${esc(nm)}`;
    d.innerHTML=`<div class="mm">${who}</div><div class="mb">${esc(txt)}</div>`;
  }
  $('chat').appendChild(d);$('chat').scrollTop=$('chat').scrollHeight;
}

export function send(){
  const txt=$('cIn').value.trim();if(!txt)return;
  $('cIn').value='';

  // Karen commands — keep the input element behavior identical but
  // route @karen / @karens through the Karen plugin instead of a
  // plain bcast. Use a dynamic import so chat.js stays loadable on
  // pages that don't bundle Karen (sandbox etc).
  const mKarens=/^@karens\s+(.+)$/i.exec(txt);
  const mKaren =/^@karen\s+(.+)$/i .exec(txt);
  if (mKaren || mKarens) {
    const kind   = mKarens ? 'super' : 'solo';
    const prompt = (mKarens?.[1] || mKaren?.[1] || '').trim();
    import('./karen/index.js').then(k => k.handleKarenCommand(kind, prompt));
    return;
  }

  const p=getProfile();
  const nm=p?.username||myNm,av=p?.avatar||null;
  // mid = message id — receivers dedup on this so a peer with two open
  // dcs (mid-reconnect) still only shows the line once.
  const mid=myId+':'+Date.now().toString(36)+Math.random().toString(36).slice(2,6);
  const n=bcast({t:'msg',id:myId,mid,txt});
  addMsg(myId,nm,myEm,txt,false,av);
  CHAT.inc('msgsOut');CHAT.write('lastMsgAt',Date.now(),{type:'DateTime'});
  if(n>0)log('→ sent to '+n+' peer(s)','ok');
  else log('→ no peers connected yet','wr');
}
