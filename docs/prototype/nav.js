(function(){
const me=document.currentScript;
let tab=(me&&me.dataset&&me.dataset.tab)||'';
if(['news','sources','history','article','chars','char','seiyuu','comments','migrate'].indexOf(tab)>=0)tab='more';

function hs(s){s=String(s);let h=2166136261;for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
function rng(sd){let x=sd||1;return()=>{x^=x<<13;x^=x>>>17;x^=x<<5;x>>>=0;return x/4294967296}}
function todayLeft(){
  const a=(window.DB&&DB.items)||[];const now=new Date();let n=0;
  a.forEach((x,i)=>{
    const r=rng(hs(String(x.id||x.t||i))+31),st=r();
    if(st>=.24)  {const wd=Math.floor(r()*7),h=10+Math.floor(r()*12),m=r()<.5?0:30,ev=r()<.78?7:14;
      if(now.getDay()!==wd)return;
      if(ev===14&&Math.floor(Date.now()/864e5)%2)return;
      const t=new Date();t.setHours(h,m,0,0);
      if(+t>Date.now())n++;}
  });
  return n;
}

const T=[
 {k:'home',    h:'home.html',     l:'Главная',    i:'⌂'},
 {k:'library', h:'index.html',    l:'Библиотека', i:'▤'},
  {k:'fab',     h:'swipe.html',    l:'',           i:'✦'},
 {k:'calendar',h:'calendar.html', l:'Календарь',  i:'▦'},
 {k:'more',    h:'',              l:'Ещё',        i:'⋯'}
];
const MORE=[
 {h:'updates.html', l:'Обновления',s:'новые главы и серии'},
 {h:'news.html',    l:'Новости',   s:'анонсы, сезоны, статьи'},
 {h:'history.html', l:'История',   s:'что и когда читали'},
 {h:'downloads.html',l:'Загрузки',  s:'очередь и офлайн'},
 {h:'sources.html', l:'Источники', s:'парсеры и их состояние'},
 {h:'auth.html',    l:'Аккаунт',   s:'вход и синхронизация'},
 {h:'stats.html',   l:'Прогресс',  s:'серии, статистика, достижения'},
 {h:'settings.html',l:'Настройки', s:'чтение, источники, приватность'},
 {h:'hub.html',     l:'Все экраны',s:'карта прототипа'}
];

const bar=document.createElement('nav');bar.className='tabbar';
const cnt=todayLeft();
bar.innerHTML=T.map(t=>{
  if(t.k==='fab')return '<button class="tb fab" data-h="'+t.h+'"><i>'+t.i+'</i></button>';
  const b=(t.k==='calendar'&&cnt)?'<em class="tb-b">'+cnt+'</em>':'';
  return '<button class="tb'+(tab===t.k?' on':'')+'" data-k="'+t.k+'" data-h="'+t.h+'">'+
    '<i>'+t.i+'</i>'+b+'<span>'+t.l+'</span></button>';
}).join('');
document.body.appendChild(bar);

function sheet(){
  const w=document.createElement('div');w.className='sp-scrim';
  w.innerHTML='<div class="sp-sheet"><div class="sp-h">Ещё</div>'+
    MORE.map(m=>'<button class="mr" data-h="'+m.h+'"><b>'+m.l+'</b><span>'+m.s+'</span></button>').join('')+
    '<button class="sp-close">Закрыть</button></div>';
  document.body.appendChild(w);
  w.onclick=e=>{if(e.target===w||e.target.classList.contains('sp-close'))w.remove()};
  w.querySelectorAll('.mr').forEach(b=>b.onclick=()=>location.href=b.dataset.h);
}
bar.querySelectorAll('.tb').forEach(b=>b.onclick=()=>{
  if(b.dataset.k==='more')return sheet();
  if(b.dataset.h)location.href=b.dataset.h;
});
if(document.body.classList.contains('immersive'))bar.style.display='none';
window.App&&(App.navBar=bar);
})();
