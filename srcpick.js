(function(){
if(!window.DB) window.DB={};
const POOL=[
 {id:'remanga', name:'ReManga',  lang:'ru', ms:340},
 {id:'mangalib',name:'MangaLib', lang:'ru', ms:520},
 {id:'mangadex',name:'MangaDex', lang:'en', ms:280},
 {id:'desu',    name:'Desu',     lang:'ru', ms:610},
 {id:'anilib',  name:'AniLibria',lang:'ru', ms:300},
 {id:'ranobelib',name:'RanobeLib',lang:'ru',ms:480}
];
const K={h:'srcHealth',p:'srcPin',n:'progNum'};
const now=()=>Date.now();
const ld=k=>{try{return JSON.parse(localStorage.getItem(k))||{}}catch(e){return{}}};
const sv=(k,v)=>localStorage.setItem(k,JSON.stringify(v));
function hs(s){s=String(s);let h=2166136261;for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
function rng(seed){let x=seed||1;return()=>{x^=x<<13;x^=x>>>17;x^=x<<5;x>>>=0;return x/4294967296}}

function gen(id){
  const r=rng(hs(id)+7), n=1+Math.floor(r()*3);
  const pick=POOL.map((s,i)=>[i,r()]).sort((a,b)=>a[1]-b[1]).slice(0,n).map(a=>a[0]);
  const base=24+Math.floor(r()*110);
  return pick.map((i,k)=>{
    const last=Math.max(4, base-k*(4+Math.floor(r()*16)));
    const gaps=[]; if(k>0&&last>12){gaps.push(6+Math.floor(r()*(last-8)))}
    return Object.assign({},POOL[i],{last:last,cnt:last-gaps.length,gaps:gaps});
  });
}
const S={
  all(id){ DB.sources=DB.sources||{}; if(!DB.sources[id]) DB.sources[id]=gen(id); return DB.sources[id]; },
  down(sid){const h=ld(K.h)[sid];return !!(h&&h.until>now())},
  left(sid){const h=ld(K.h)[sid];return h?Math.max(0,Math.ceil((h.until-now())/60000)):0},
  fail(sid){const h=ld(K.h),r=h[sid]||{f:0};r.f++;r.until=now()+Math.min(21600e3,60e3*Math.pow(2,r.f-1));h[sid]=r;sv(K.h,h)},
  heal(sid){const h=ld(K.h);delete h[sid];sv(K.h,h)},
  pinOf(id){return ld(K.p)[id]||null},
  setPin(id,sid){const p=ld(K.p);sid?p[id]=sid:delete p[id];sv(K.p,p)},
  progOf(id){return ld(K.n)[id]||0},
  setProg(id,n){const p=ld(K.n);p[id]=n;sv(K.n,p)},
  rank(l){return l.slice().sort((a,b)=>(b.last-a.last)||(b.cnt-a.cnt)||((a.lang=='ru'?0:1)-(b.lang=='ru'?0:1))||(a.ms-b.ms))},
  pick(id){
    const rk=S.rank(S.all(id)); if(!rk.length) return{src:null,why:'none',rk:rk};
    const live=rk.filter(s=>!S.down(s.id)), pin=S.pinOf(id), p=pin&&rk.find(s=>s.id===pin);
    if(p&&!S.down(p.id)) return{src:p,why:'pin',rk:rk,best:rk[0]};
    if(p)                return{src:live[0]||null,why:'pindown',pin:p,rk:rk,best:rk[0]};
    if(!live.length)     return{src:null,why:'alldown',rk:rk,best:rk[0]};
    return{src:live[0],why:live[0].id===rk[0].id?'best':'fallback',rk:rk,best:rk[0]};
  },
  has(src,n){return n<=src.last&&src.gaps.indexOf(n)<0},
  merged(id){
    const rk=S.rank(S.all(id)),max=Math.max.apply(null,rk.map(s=>s.last)),out=[];
    for(let n=max;n>=1;n--){const inn=rk.filter(s=>S.has(s,n)).map(s=>s.id);if(inn.length)out.push({n:n,in:inn})}
    return out;
  }
};
window.SrcPick=S;

function esc(s){return String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]))}
function toast(m){ if(window.App&&App.toast)return App.toast(m);
  let t=document.getElementById('sp-toast');
  if(!t){t=document.createElement('div');t.id='sp-toast';t.className='sp-toast';document.body.appendChild(t)}
  t.textContent=m;t.classList.add('on');clearTimeout(t._x);t._x=setTimeout(()=>t.classList.remove('on'),2200)}

function itemId(){
  const q=new URLSearchParams(location.search);
  return q.get('id')||q.get('item')||(DB.items&&DB.items[0]&&(DB.items[0].id||DB.items[0].t))||'demo';
}

function render(){
  const id=itemId(), r=S.pick(id), bar=document.getElementById('srcbar'); if(!bar)return;
  const prog=S.progOf(id), best=r.best||{};
  let cls='ok',txt='',warn='';
  if(!r.src){cls='err';txt='<b>Нет доступного источника</b><span>все источники в кулдауне</span>'}
  else{
    const gapN=(best.last||0)-r.src.last;
    txt='<b>'+esc(r.src.name)+'</b><span>гл. 1–'+r.src.last+(r.src.gaps.length?' · пропуск: '+r.src.gaps.join(', '):' · полный каталог')+'</span>';
    if(r.why==='fallback'){cls='warn';warn='Основной источник ('+esc(best.name)+', '+best.last+' гл.) не отвечает — ещё '+S.left(best.id)+' мин. Читаете самый полный из живых: −'+gapN+' гл.'}
    if(r.why==='pin'){cls='pin';if(r.src.id!==best.id)warn='Закреплён вручную. У '+esc(best.name)+' на '+((best.last-r.src.last))+' гл. больше.'}
    if(r.why==='pindown'){cls='warn';warn='Закреплённый '+esc(r.pin.name)+' недоступен — временно '+esc(r.src.name)+'. Закрепление сохранено.'}
    if(prog>r.src.last)warn=(warn?warn+' ':'')+'Ваш прогресс — гл. '+prog+', здесь только до '+r.src.last+'. Позиция не сброшена.';
  }
  bar.className='srcbar '+cls;
  bar.innerHTML='<div class="sb-row"><i class="sb-dot"></i><div class="sb-txt">'+txt+'</div>'+
    '<button class="sb-btn" id="sb-open">Источник</button></div>'+(warn?'<div class="sb-warn">'+warn+'</div>':'');
  document.getElementById('sb-open').onclick=sheet;
  chaps(id,r);
}

function chaps(id,r){
  let box=document.getElementById('chaps');
  if(!box){box=document.getElementById('sp-chaps');
    if(!box){box=document.createElement('div');box.id='sp-chaps';const b=document.getElementById('srcbar');b.parentNode.insertBefore(box,b.nextSibling)}}
  const list=S.merged(id),cur=r.src,prog=S.progOf(id);
  box.innerHTML='<div class="sechead"><span>Главы</span><span class="mut">'+list.length+'</span></div>'+
   list.map(c=>{
     const here=cur&&S.has(cur,c.n), other=c.in.filter(x=>!cur||x!==cur.id);
     return '<div class="chrow'+(here?'':' off')+(c.n<=prog?' read':'')+'" data-n="'+c.n+'">'+
       '<div class="chn">Глава '+c.n+'</div>'+
       '<div class="chs">'+(here?(c.n<=prog?'прочитано':'доступно'):'нет в '+esc(cur?cur.name:'—')+' · есть в '+other.length+' др.')+'</div></div>';
   }).join('');
  box.querySelectorAll('.chrow').forEach(el=>el.onclick=()=>{
    const n=+el.dataset.n;
    if(el.classList.contains('off')){toast('Главы '+n+' нет в текущем источнике — откройте меню источника');return}
    S.setProg(id,n);toast('Читаем гл. '+n);render();
  });
}

function sheet(){
  const id=itemId(),r=S.pick(id),pin=S.pinOf(id);
  const w=document.createElement('div');w.className='sp-scrim';
  w.innerHTML='<div class="sp-sheet"><div class="sp-h">Источники</div>'+
   r.rk.map((s,i)=>{
     const d=S.down(s.id),act=r.src&&r.src.id===s.id;
     return '<div class="sp-src'+(act?' act':'')+(d?' dead':'')+'">'+
       '<div class="sp-i"><b>'+esc(s.name)+(i===0?' <span class="tag">макс. глав</span>':'')+(pin===s.id?' <span class="tag pin">закреплён</span>':'')+'</b>'+
       '<span>'+s.last+' гл. · '+s.lang.toUpperCase()+' · '+s.ms+' мс'+(d?' · недоступен ещё '+S.left(s.id)+' мин':'')+'</span></div>'+
       '<div class="sp-b"><button data-pin="'+s.id+'">'+(pin===s.id?'Открепить':'Закрепить')+'</button>'+
       '<button data-brk="'+s.id+'" class="gh">'+(d?'Починить':'Сломать')+'</button></div></div>';
   }).join('')+
   '<div class="sp-note">Авто-выбор: максимальный номер главы среди живых источников. Кулдаун после ошибки удваивается.</div>'+
   '<button class="sp-close">Закрыть</button></div>';
  document.body.appendChild(w);
  w.onclick=e=>{if(e.target===w||e.target.classList.contains('sp-close'))w.remove()};
  w.querySelectorAll('[data-pin]').forEach(b=>b.onclick=()=>{
    const s=b.dataset.pin;S.setPin(id,pin===s?null:s);w.remove();render();toast(pin===s?'Автовыбор включён':'Источник закреплён')});
  w.querySelectorAll('[data-brk]').forEach(b=>b.onclick=()=>{
    const s=b.dataset.brk;S.down(s)?S.heal(s):S.fail(s);w.remove();render();
    const n=S.pick(id);toast(S.down(s)?'Источник упал → '+(n.src?n.src.name:'нет замены'):'Источник восстановлен')});
}

function mount(){
  if(document.getElementById('srcbar'))return true;
  const host=document.getElementById('chaps')||document.querySelector('.fmtbar')||document.querySelector('main')||document.body;
  const d=document.createElement('div');d.id='srcbar';d.className='srcbar';
  host===document.body||host.id==='chaps'?host.parentNode.insertBefore(d,host):host.parentNode.insertBefore(d,host.nextSibling);
  return true;
}
function boot(){
  const t=/title\.html/.test(location.pathname)||document.getElementById('chaps')||document.getElementById('srcbar');
  if(!t)return; mount(); try{render()}catch(e){console.warn('srcpick',e)}
}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',boot):boot();
})();
