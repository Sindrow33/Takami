(function(){
const q=new URLSearchParams(location.search);
const esc=s=>String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
function toast(m){let t=document.getElementById('sp-toast');
 if(!t){t=document.createElement('div');t.id='sp-toast';t.className='sp-toast';document.body.appendChild(t)}
 t.textContent=m;t.classList.add('on');clearTimeout(t._x);t._x=setTimeout(()=>t.classList.remove('on'),2200)}
function itemId(){return q.get('id')||q.get('item')||(window.DB&&DB.items&&DB.items[0]&&(DB.items[0].id||DB.items[0].t))||'demo'}

function nums(s){const a=[];for(let n=1;n<=s.last;n++)if(s.gaps.indexOf(n)<0)a.push(n);return a}
function map(n,off,mer){return Math.ceil(n/mer)+off}
function score(A,B,off,mer){
  const set={};nums(B).forEach(n=>set[n]=1);
  const a=nums(A);let hit=0;a.forEach(n=>{if(set[map(n,off,mer)])hit++});
  return{hit:hit,tot:a.length,p:a.length?hit/a.length:0};
}
function detect(A,B){
  let best={off:0,mer:1,p:-1};
  [1,2,3].forEach(mer=>{for(let off=-6;off<=6;off++){
    const s=score(A,B,off,mer);
    const pen=(mer-1)*0.04+Math.abs(off)*0.005;
    if(s.p-pen>best.p-((best.mer-1)*0.04+Math.abs(best.off)*0.005))best={off:off,mer:mer,p:s.p,hit:s.hit,tot:s.tot};
  }});
  return best;
}

const ST={from:null,to:null,off:0,mer:1,auto:true};

function render(){
  const id=itemId(),all=SrcPick.rank(SrcPick.all(id)),prog=SrcPick.progOf(id);
  if(all.length<2){document.getElementById('mg').innerHTML=
    '<div class="mg-empty">У тайтла один источник — мигрировать некуда.<br><span>Миграция появится, когда парсер найдёт этот тайтл ещё где-то.</span></div>';return}
  if(!ST.from)ST.from=(SrcPick.pick(id).src||all[0]).id;
  if(!ST.to)ST.to=(all.find(s=>s.id!==ST.from)||all[1]).id;
  const A=all.find(s=>s.id===ST.from),B=all.find(s=>s.id===ST.to);
  if(ST.auto){const d=detect(A,B);ST.off=d.off;ST.mer=d.mer}
  const sc=score(A,B,ST.off,ST.mer),pct=Math.round(sc.p*100);
  const np=prog?Math.max(1,map(prog,ST.off,ST.mer)):0;
  const inB=np&&SrcPick.has(B,np);
  const lost=nums(A).filter(n=>!nums(B).includes(map(n,ST.off,ST.mer)));
  const conf=pct>=90?'ok':pct>=65?'warn':'err';

  const pick=(who,cur)=>'<select data-who="'+who+'">'+all.map(s=>
    '<option value="'+s.id+'"'+(s.id===cur?' selected':'')+'>'+esc(s.name)+' · '+s.last+' гл.'+(SrcPick.down(s.id)?' (недоступен)':'')+'</option>').join('')+'</select>';

  let rows='';
  const show=nums(A).slice(-14).reverse();
  show.forEach(n=>{const t=map(n,ST.off,ST.mer),ok=SrcPick.has(B,t);
    rows+='<div class="mg-row'+(ok?'':' bad')+(n===prog?' cur':'')+'">'+
      '<span>гл. '+n+'</span><i>→</i><span>'+(ok?'гл. '+t:'нет главы '+t)+'</span></div>'});

  document.getElementById('mg').innerHTML=
   '<div class="mg-pair">'+pick('from',ST.from)+'<div class="mg-ar">→</div>'+pick('to',ST.to)+'</div>'+
   '<div class="mg-conf '+conf+'"><b>'+pct+'% совпадение</b>'+
     '<span>'+sc.hit+' из '+sc.tot+' глав сопоставлены'+(ST.mer>1?' · склейка '+ST.mer+' в 1':'')+
     (ST.off?' · сдвиг '+(ST.off>0?'+':'')+ST.off:'')+'</span></div>'+
   (pct<65?'<div class="mg-warn">Низкая уверенность. Скорее всего это разные тайтлы или разбиение по томам. Проверьте вручную перед переносом.</div>':'')+
   '<div class="mg-tune"><span>Сдвиг</span><button data-d="-1">−</button><b>'+(ST.off>0?'+':'')+ST.off+'</b><button data-d="1">+</button>'+
     '<span class="mut">склейка</span>'+[1,2,3].map(m=>'<button class="m'+(m===ST.mer?' on':'')+'" data-m="'+m+'">'+m+'</button>').join('')+
     (ST.auto?'':'<button class="mg-auto">авто</button>')+'</div>'+
   '<div class="mg-prog"><div><span>Прогресс сейчас</span><b>'+(prog?'гл. '+prog+' · '+esc(A.name):'не начат')+'</b></div>'+
     '<div><span>После переноса</span><b class="'+(prog&&!inB?'bad':'')+'">'+(prog?(inB?'гл. '+np:'гл. '+np+' — нет в источнике'):'не начат')+'</b></div></div>'+
   (prog&&!inB?'<div class="mg-warn">Целевой источник не содержит вашу главу. Прогресс сохранится числом, но продолжить придётся с ближайшей доступной.</div>':'')+
   (lost.length?'<div class="mg-warn soft">Потеряется доступ к '+lost.length+' гл.: '+lost.slice(0,8).join(', ')+(lost.length>8?'…':'')+'</div>':'')+
   '<div class="sechead"><span>Сопоставление</span><span class="mut">последние 14</span></div><div class="mg-list">'+rows+'</div>'+
   '<button class="mg-go">Перенести и закрепить '+esc(B.name)+'</button>'+
   '<div class="sp-note">Перенос меняет только источник и номер главы. Закладки, оценка и комментарии привязаны к тайтлу и не трогаются.</div>';

  document.querySelectorAll('#mg select').forEach(s=>s.onchange=()=>{
    ST[s.dataset.who]=s.value;if(ST.from===ST.to){const o=all.find(x=>x.id!==s.value);ST[s.dataset.who==='from'?'to':'from']=o.id}
    ST.auto=true;render()});
  document.querySelectorAll('#mg [data-d]').forEach(b=>b.onclick=()=>{ST.auto=false;ST.off+=+b.dataset.d;render()});
  document.querySelectorAll('#mg [data-m]').forEach(b=>b.onclick=()=>{ST.auto=false;ST.mer=+b.dataset.m;render()});
  const a=document.querySelector('.mg-auto');if(a)a.onclick=()=>{ST.auto=true;render()};
  document.querySelector('.mg-go').onclick=()=>{
    SrcPick.setPin(id,B.id);if(prog)SrcPick.setProg(id,np);
    toast('Перенесено в '+B.name+(prog?' · гл. '+np:''));
    setTimeout(()=>location.href='title.html?id='+encodeURIComponent(id),700)};
}

function injectBtn(){
  const bar=document.getElementById('srcbar');if(!bar||document.getElementById('mg-link'))return;
  const b=document.createElement('button');b.id='mg-link';b.className='mg-link';
  b.textContent='Сменить источник с переносом прогресса';
  b.onclick=()=>location.href='migrate.html?id='+encodeURIComponent(itemId());
  bar.appendChild(b);
}
function boot(){
  if(document.getElementById('mg')){try{render()}catch(e){console.warn('mg',e)}return}
  const t=setInterval(()=>{if(document.getElementById('srcbar')){injectBtn();clearInterval(t)}},150);
  setTimeout(()=>clearInterval(t),3000);
}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',boot):boot();
})();
