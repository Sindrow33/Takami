(function(){
const esc=s=>String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
function hs(s){s=String(s);let h=2166136261;for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
function rng(sd){let x=sd||1;return()=>{x^=x<<13;x^=x>>>17;x^=x<<5;x>>>=0;return x/4294967296}}
function items(){const a=(window.DB&&DB.items)||[];return a.map((x,i)=>({
  id:String(x.id||x.t||i),t:x.t||x.title||('Тайтл '+(i+1)),type:x.type||x.kind||'manga',g:x.g||x.grad||''}))}
const UNIT={manga:'Глава',ranobe:'Глава',anime:'Эпизод'};
const LIB={};(function(){try{Object.assign(LIB,JSON.parse(localStorage.getItem('lib'))||{})}catch(e){}})();

function sched(it){
  const r=rng(hs(it.id)+31);
  const st=r();const status=st<.12?'finished':st<.24?'hiatus':'ongoing';
  const wd=Math.floor(r()*7),hour=10+Math.floor(r()*12),every=r()<.78?7:14;
  const num=5+Math.floor(r()*120),late=r()<.18;
  return{wd:wd,hour:hour,min:r()<.5?0:30,every:every,status:status,num:num,late:late};
}
function occur(it,s,from,days){
  if(s.status!=='ongoing')return[];
  const out=[],base=new Date(from);base.setHours(0,0,0,0);
  for(let d=0;d<days;d++){
    const x=new Date(+base+d*864e5);
    if(x.getDay()!==s.wd)continue;
    if(s.every===14&&Math.floor((+x/864e5))%2)continue;
    x.setHours(s.hour,s.min,0,0);
    const idx=Math.round((+x-Date.now())/(s.every*864e5));
    out.push({ts:+x,n:s.num+idx});
  }
  return out;
}
function state(o,s){
  const now=Date.now();
  if(o.ts>now)return'soon';
  if(s.late&&now-o.ts<3*864e5)return'late';
  return'out';
}
let SEL=new Date();SEL.setHours(0,0,0,0);
let F='all',ONLY=false;

function render(){
  const box=document.getElementById('cal');if(!box)return;
  const all=items().filter(i=>F==='all'||i.type===F).filter(i=>!ONLY||LIB[i.id]);
  const wk=[];const mon=new Date();mon.setHours(0,0,0,0);mon.setDate(mon.getDate()-((mon.getDay()+6)%7));
  for(let i=0;i<14;i++)wk.push(new Date(+mon+i*864e5));
  const cnt={};all.forEach(i=>{const s=sched(i);occur(i,s,mon,14).forEach(o=>{const k=new Date(o.ts).toDateString();cnt[k]=(cnt[k]||0)+1})});

  const strip='<div class="c-strip">'+wk.map(d=>{
    const k=d.toDateString(),td=k===new Date().toDateString(),sl=k===SEL.toDateString();
    return '<button class="c-day'+(sl?' on':'')+(td?' today':'')+'" data-d="'+(+d)+'">'+
      '<span>'+['вс','пн','вт','ср','чт','пт','сб'][d.getDay()]+'</span><b>'+d.getDate()+'</b>'+
      (cnt[k]?'<i class="c-dot">'+cnt[k]+'</i>':'<i class="c-dot off"></i>')+'</button>'}).join('')+'</div>';

  const chips='<div class="chips">'+[['all','Всё'],['manga','Манга'],['anime','Аниме'],['ranobe','Ранобэ']]
    .map(c=>'<button class="chip'+(F===c[0]?' on':'')+'" data-f="'+c[0]+'">'+c[1]+'</button>').join('')+
    '<button class="chip'+(ONLY?' on':'')+'" id="c-only">Только моё</button></div>';

  const day=[];all.forEach(i=>{const s=sched(i);occur(i,s,SEL,1).forEach(o=>day.push({i:i,s:s,o:o}))});
  day.sort((a,b)=>a.o.ts-b.o.ts);
  const rows=day.length?day.map(x=>row(x)).join(''):
    '<div class="mg-empty">В этот день релизов нет.<br><span>Расписание берётся из источника и может сдвигаться.</span></div>';

  const up=[];all.forEach(i=>{const s=sched(i);occur(i,s,new Date(),21).forEach(o=>{if(o.ts>Date.now())up.push({i:i,s:s,o:o})})});
  up.sort((a,b)=>a.o.ts-b.o.ts);
  const hero=up[0]?'<div class="c-hero" data-id="'+esc(up[0].i.id)+'"><span>Ближайший релиз</span>'+
    '<b>'+esc(up[0].i.t)+'</b><i>'+(UNIT[up[0].i.type]||'Глава')+' '+up[0].o.n+' · через '+left(up[0].o.ts)+'</i></div>':'';

  const paused=items().filter(i=>{const s=sched(i);return s.status!=='ongoing'}).filter(i=>!ONLY||LIB[i.id]);
  const pz=paused.length?'<div class="sechead"><span>Без расписания</span><span class="mut">'+paused.length+'</span></div>'+
    paused.map(i=>{const s=sched(i);return '<div class="c-row flat" data-id="'+esc(i.id)+'">'+
      '<div class="h-cov" style="'+(i.g?'background:'+i.g:'')+'"></div><div class="h-i"><b>'+esc(i.t)+'</b>'+
      '<span class="'+(s.status==='hiatus'?'w':'')+'">'+(s.status==='hiatus'?'Хиатус — выпуск приостановлен':'Завершён · '+s.num+' всего')+'</span></div></div>'}).join(''):'';

  box.innerHTML=chips+strip+hero+
    '<div class="sechead"><span>'+SEL.toLocaleDateString('ru',{weekday:'long',day:'numeric',month:'long'})+'</span>'+
    '<span class="mut">'+day.length+'</span></div>'+rows+pz+
    '<div class="sp-note">Даты расчётные: строятся по среднему интервалу выпусков источника. Точное время публикации сайты почти никогда не отдают.</div>';
  bind();
}
function row(x){
  const st=state(x.o,x.s),it=x.i;
  const lbl=st==='out'?'вышло':st==='late'?'задержка':'ожидается';
  return '<div class="c-row '+st+'" data-id="'+esc(it.id)+'">'+
    '<div class="c-time">'+new Date(x.o.ts).toLocaleTimeString('ru',{hour:'2-digit',minute:'2-digit'})+'</div>'+
    '<div class="h-cov" style="'+(it.g?'background:'+it.g:'')+'"></div>'+
    '<div class="h-i"><b>'+esc(it.t)+'</b><span>'+(UNIT[it.type]||'Глава')+' '+x.o.n+
    (x.s.every===14?' · раз в 2 недели':' · еженедельно')+'</span></div>'+
    '<span class="c-badge">'+lbl+'</span></div>';
}
function left(ts){const m=Math.round((ts-Date.now())/60000);
  if(m<60)return m+' мин';const h=Math.floor(m/60);if(h<24)return h+' ч';return Math.floor(h/24)+' дн '+(h%24)+' ч'}
function bind(){
  document.querySelectorAll('#cal [data-d]').forEach(b=>b.onclick=()=>{SEL=new Date(+b.dataset.d);render()});
  document.querySelectorAll('#cal [data-f]').forEach(b=>b.onclick=()=>{F=b.dataset.f;render()});
  const o=document.getElementById('c-only');if(o)o.onclick=()=>{ONLY=!ONLY;render()};
  document.querySelectorAll('#cal [data-id]').forEach(r=>r.onclick=()=>location.href='title.html?id='+encodeURIComponent(r.dataset.id));
}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',render):render();
})();
