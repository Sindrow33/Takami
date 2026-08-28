(function(){
const esc=s=>String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
function toast(m){let t=document.getElementById('sp-toast');
 if(!t){t=document.createElement('div');t.id='sp-toast';t.className='sp-toast';document.body.appendChild(t)}
 t.textContent=m;t.classList.add('on');clearTimeout(t._x);t._x=setTimeout(()=>t.classList.remove('on'),2200)}

const SCH=[
 {g:'Чтение',r:[
  {k:'readerMode',t:'seg',l:'Режим по умолчанию',o:[['webtoon','Лента'],['ltr','Страницы →'],['rtl','Страницы ←']],
   s:'Применяется к новым тайтлам. Для каждого тайтла режим можно переопределить в ридере.'},
  {k:'tapZones',t:'sw',l:'Зоны нажатия',s:'Края экрана листают, центр открывает панель'},
  {k:'volKeys',t:'sw',l:'Листать кнопками громкости'},
  {k:'keepScreen',t:'sw',l:'Не гасить экран во время чтения'}]},
 {g:'Источники',r:[
  {k:'autoSource',t:'sw',l:'Автовыбор источника',s:'Берётся живой источник с наибольшим номером главы. Закреплённый вручную приоритетнее.'},
  {k:'minChapters',t:'sw',l:'Прятать источники с явными пропусками'},
  {k:'preferLang',t:'seg',l:'Приоритет языка',o:[['ru','Русский'],['en','English'],['any','Любой']]},
  {k:'wifiOnly',t:'sw',l:'Загрузки только по Wi-Fi'}]},
 {g:'Уведомления',r:[
  {k:'notifyRelease',t:'sw',l:'Оповещать о выходе',s:'По расчётному расписанию из календаря. Даты приблизительные, возможны ложные срабатывания.'},
  {k:'groupNotify',t:'sw',l:'Группировать в одно уведомление'},
  {k:'quietFrom',t:'num',l:'Тихие часы с',min:0,max:23},
  {k:'quietTo',t:'num',l:'Тихие часы до',min:0,max:23}]},
 {g:'Приватность',r:[
  {k:'history',t:'sw',l:'Вести историю чтения',s:'Выключение не стирает уже записанное'},
  {k:'spoilers',t:'sw',l:'Скрывать спойлеры',s:'Биографии персонажей и комментарии размываются до нажатия'},
  {k:'blurNsfw',t:'sw',l:'Размывать обложки 18+'},
  {t:'act',l:'Очистить историю',danger:1,fn:()=>{if(confirm('Удалить всю историю?')){localStorage.setItem('histLog','[]');toast('История очищена')}}}]},
 {g:'Прогресс',r:[
  {k:'gamify',t:'sw',l:'Серии и достижения',s:'Выключите, если счётчики давят. Статистика останется, уровни и серии исчезнут.'},
  {k:'freeze',t:'sw',l:'Заморозка серии',s:'Два пропущенных дня в месяц не обнуляют серию'},
  {t:'act',l:'Открыть статистику',fn:()=>location.href='stats.html'}]},
 {g:'Хранилище',r:[
  {k:'cacheMb',t:'seg',l:'Лимит кеша',o:[[256,'256 МБ'],[512,'512 МБ'],[2048,'2 ГБ']]},
  {k:'autoDeleteRead',t:'sw',l:'Удалять прочитанные загрузки'},
  {t:'act',l:'Очистить кеш изображений',fn:()=>toast('Кеш очищен (в прототипе — заглушка)')}]},
 {g:'Данные',r:[
  {t:'act',l:'Экспорт настроек и прогресса',fn:exp},
  {t:'act',l:'Импорт из файла',fn:imp},
  {t:'act',l:'Сбросить все настройки',danger:1,fn:()=>{if(confirm('Вернуть значения по умолчанию?')){Cfg.reset();render();toast('Сброшено')}}}]}
];

function exp(){
  const b=new Blob([JSON.stringify(Cfg.dump(),null,1)],{type:'application/json'});
  const a=document.createElement('a');a.href=URL.createObjectURL(b);
  a.download='backup-'+new Date().toISOString().slice(0,10)+'.json';a.click();toast('Файл сохранён')}
function imp(){
  const i=document.createElement('input');i.type='file';i.accept='.json';
  i.onchange=()=>{const f=i.files[0];if(!f)return;const r=new FileReader();
    r.onload=()=>{try{const o=JSON.parse(r.result);Object.keys(o).forEach(k=>localStorage.setItem(k,o[k]));
      toast('Импортировано');setTimeout(()=>location.reload(),600)}catch(e){toast('Файл повреждён')}};
    r.readAsText(f)};i.click()}

function render(){
  const box=document.getElementById('set');if(!box)return;
  box.innerHTML=SCH.map(g=>'<div class="sechead"><span>'+g.g+'</span></div><div class="st-g">'+
   g.r.map(r=>row(r)).join('')+'</div>').join('')+
   '<div class="sp-note">Настройки хранятся локально. В сборке это будет DataStore, а не отдельные ключи на каждый экран.</div>';
  bind();
}
function row(r){
  const v=r.k?Cfg.get(r.k):null;
  const head='<div class="st-i"><b>'+esc(r.l)+'</b>'+(r.s?'<span>'+esc(r.s)+'</span>':'')+'</div>';
  if(r.t==='sw')return '<div class="st-r" data-k="'+r.k+'" data-t="sw">'+head+
    '<i class="sw'+(v?' on':'')+'"></i></div>';
  if(r.t==='num')return '<div class="st-r">'+head+'<div class="st-n"><button data-n="'+r.k+'" data-d="-1">−</button>'+
    '<b>'+String(v).padStart(2,'0')+':00</b><button data-n="'+r.k+'" data-d="1">+</button></div></div>';
  if(r.t==='seg')return '<div class="st-c">'+head+'<div class="st-s">'+
    r.o.map(o=>'<button data-s="'+r.k+'" data-v="'+o[0]+'" class="'+(String(v)===String(o[0])?'on':'')+'">'+o[1]+'</button>').join('')+'</div></div>';
  return '<button class="st-a'+(r.danger?' dn':'')+'" data-a="'+esc(r.l)+'">'+esc(r.l)+'</button>';
}
function bind(){
  document.querySelectorAll('[data-t="sw"]').forEach(e=>e.onclick=()=>{
    const k=e.dataset.k;Cfg.set(k,!Cfg.get(k));render();
    if(k==='gamify'&&!Cfg.get(k))toast('Серии и уровни скрыты')});
  document.querySelectorAll('[data-n]').forEach(b=>b.onclick=()=>{
    const k=b.dataset.n;let v=Cfg.get(k)+ +b.dataset.d;if(v<0)v=23;if(v>23)v=0;Cfg.set(k,v);render()});
  document.querySelectorAll('[data-s]').forEach(b=>b.onclick=()=>{
    let v=b.dataset.v;if(/^\d+$/.test(v))v=+v;Cfg.set(b.dataset.s,v);render()});
  document.querySelectorAll('[data-a]').forEach(b=>b.onclick=()=>{
    SCH.forEach(g=>g.r.forEach(r=>{if(r.l===b.dataset.a&&r.fn)r.fn()}))});
}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',render):render();
})();
