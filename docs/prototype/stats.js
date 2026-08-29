(function(){
const esc=s=>String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
const log=()=>{try{return JSON.parse(localStorage.getItem('histLog'))||[]}catch(e){return[]}};
const dk=ts=>{const d=new Date(ts);return d.getFullYear()+'-'+d.getMonth()+'-'+d.getDate()};
function items(){const a=(window.DB&&DB.items)||[];const m={};a.forEach((x,i)=>{
  const id=String(x.id||x.t||i);m[id]={t:x.t||x.title||('Тайтл '+(i+1)),type:x.type||x.kind||'manga'}});return m}

function agg(){
  const l=log(),M=items(),o={ch:0,min:0,days:{},type:{manga:0,anime:0,ranobe:0},titles:{},night:0,max:0};
  l.forEach(e=>{
    const n=Math.max(1,(e.to||0)-(e.from||0)+1),m=Math.max(1,Math.round((e.last-e.ts)/60000));
    o.ch+=n;o.min+=m;const k=dk(e.last);o.days[k]=(o.days[k]||0)+n;
    const t=(M[e.id]||{}).type||'manga';o.type[t]=(o.type[t]||0)+n;
    o.titles[e.id]=(o.titles[e.id]||0)+n;
    const h=new Date(e.last).getHours();if(h>=0&&h<5)o.night+=n;
  });
  Object.keys(o.days).forEach(k=>{if(o.days[k]>o.max)o.max=o.days[k]});
  return o;
}
function streak(days){
  const has=k=>!!days[k],fr=Cfg.get('freeze');let n=0,skips=0,cur=new Date();cur.setHours(0,0,0,0);
  if(!has(dk(+cur)))cur=new Date(+cur-864e5);
  for(let i=0;i<400;i++){
    if(has(dk(+cur))){n++}
    else if(fr&&skips<2&&n>0){skips++}
    else break;
    cur=new Date(+cur-864e5);
  }
  return{n:n,skips:skips};
}
const ACH=[
 {id:'first', l:'Первая глава',      s:'Начать читать',              g:a=>Math.min(a.ch,1),  m:1},
 {id:'ten',   l:'Разогрев',          s:'50 глав всего',              g:a=>a.ch,              m:50},
 {id:'hund',  l:'Постоянство',       s:'300 глав всего',             g:a=>a.ch,              m:300},
 {id:'mara',  l:'Марафон',           s:'10 глав за один день',       g:a=>a.max,             m:10},
 {id:'night', l:'Ночной читатель',   s:'20 глав между 00:00 и 05:00',g:a=>a.night,           m:20},
 {id:'poly',  l:'Всеядный',          s:'Манга, аниме и ранобэ',      g:a=>['manga','anime','ranobe'].filter(t=>a.type[t]>0).length, m:3},
 {id:'deep',  l:'Погружение',        s:'50 глав одного тайтла',      g:a=>Math.max(0,...Object.values(a.titles)), m:50},
 {id:'time',  l:'Сутки в тайтлах',   s:'24 часа чтения',             g:a=>Math.round(a.min/60), m:24}
];
function lvl(xp){let L=1,need=100,left=xp;while(left>=need){left-=need;L++;need=Math.round(need*1.35)}return{L:L,in:left,need:need}}

function render(){
  const box=document.getElementById('st');if(!box)return;
  const a=agg(),M=items(),gm=Cfg.get('gamify');
  if(!a.ch){box.innerHTML='<div class="mg-empty">Пока нечего показывать.<br><span>Статистика считается из истории чтения — она полностью локальная.</span></div>';return}
  const s=streak(a.days),xp=a.ch*10+a.min,L=lvl(xp);
  const wk=[];for(let i=6;i>=0;i--){const d=new Date(Date.now()-i*864e5);wk.push({d:d,n:a.days[dk(+d)]||0})}
  const mx=Math.max(1,...wk.map(w=>w.n));

  const hero=gm?'<div class="g-hero"><div class="g-lv"><b>'+L.L+'</b><span>уровень</span></div>'+
   '<div class="g-xp"><div class="g-bar"><i style="width:'+Math.round(L.in/L.need*100)+'%"></i></div>'+
   '<span>'+L.in+' / '+L.need+' XP до следующего</span></div></div>'+
   '<div class="g-streak"><b>'+s.n+' дн.</b><span>серия чтения'+(s.skips?' · заморозка использована '+s.skips+'/2':'')+'</span>'+
   '<i>Пропуск дня не обнуляет прогресс</i></div>':
   '<div class="sp-note" style="margin:0 0 12px">Серии и уровни выключены в настройках. Ниже — только цифры.</div>';

  const chart='<div class="sechead"><span>Неделя</span><span class="mut">'+wk.reduce((x,w)=>x+w.n,0)+' гл.</span></div>'+
   '<div class="g-chart">'+wk.map(w=>'<div class="g-col"><i style="height:'+Math.round(w.n/mx*72)+'px"></i>'+
   '<span>'+['вс','пн','вт','ср','чт','пт','сб'][w.d.getDay()]+'</span><em>'+(w.n||'')+'</em></div>').join('')+'</div>';

  const tot='<div class="g-grid">'+
   [['Глав всего',a.ch],['Часов',Math.round(a.min/60*10)/10],['Тайтлов',Object.keys(a.titles).length],
    ['Лучший день',a.max+' гл.']].map(x=>'<div class="g-c"><b>'+x[1]+'</b><span>'+x[0]+'</span></div>').join('')+'</div>';

  const tt=Object.entries(a.type).filter(x=>x[1]>0),sum=tt.reduce((x,y)=>x+y[1],0);
  const split='<div class="sechead"><span>По формату</span></div><div class="g-split">'+
   tt.map(x=>'<i class="'+x[0]+'" style="flex:'+x[1]+'" title="'+x[0]+'"></i>').join('')+'</div>'+
   '<div class="g-leg">'+tt.map(x=>'<span><i class="'+x[0]+'"></i>'+
   ({manga:'Манга',anime:'Аниме',ranobe:'Ранобэ'}[x[0]])+' · '+Math.round(x[1]/sum*100)+'%</span>').join('')+'</div>';

  const top=Object.entries(a.titles).sort((x,y)=>y[1]-x[1]).slice(0,5);
  const tl='<div class="sechead"><span>Больше всего читали</span></div>'+top.map(x=>
   '<div class="h-row" data-id="'+esc(x[0])+'"><div class="h-cov"></div><div class="h-i"><b>'+
   esc((M[x[0]]||{}).t||x[0])+'</b><span>'+x[1]+' гл.</span></div></div>').join('');

  const ac=gm?'<div class="sechead"><span>Достижения</span><span class="mut">'+
   ACH.filter(x=>x.g(a)>=x.m).length+' / '+ACH.length+'</span></div>'+
   ACH.map(x=>{const v=Math.min(x.g(a),x.m),done=v>=x.m;
    return '<div class="g-a'+(done?' on':'')+'"><div class="g-ic">'+(done?'✦':'○')+'</div>'+
    '<div class="h-i"><b>'+esc(x.l)+'</b><span>'+esc(x.s)+'</span>'+
    (done?'':'<div class="progress"><i style="width:'+Math.round(v/x.m*100)+'%"></i></div>')+'</div>'+
    (done?'':'<span class="mut">'+v+'/'+x.m+'</span>')+'</div>'}).join(''):'';

  box.innerHTML=hero+tot+chart+split+tl+ac+
   '<div class="sp-note">Всё считается на устройстве из истории. Ничего никуда не отправляется, сравнения с другими пользователями нет намеренно.</div>';
  box.querySelectorAll('[data-id]').forEach(r=>r.onclick=()=>location.href='title.html?id='+encodeURIComponent(r.dataset.id));
}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',render):render();
})();
