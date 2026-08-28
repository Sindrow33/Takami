(function(){
const K={log:'histLog',off:'histOff'};
const ld=k=>{try{return JSON.parse(localStorage.getItem(k))||[]}catch(e){return[]}};
const sv=(k,v)=>localStorage.setItem(k,JSON.stringify(v));
const esc=s=>String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
const GAP=6*3600e3;

function items(){const a=(window.DB&&DB.items)||[];return a.map((x,i)=>({
  id:String(x.id||x.t||i),t:x.t||x.title||('Тайтл '+(i+1)),type:x.type||x.kind||'manga',g:x.g||x.grad||''}))}
function itm(id){return items().find(x=>x.id===String(id))||{id:id,t:id,type:'manga'}}
const UNIT={manga:'гл.',ranobe:'гл.',anime:'эп.'};
const H={
  off(){return localStorage.getItem(K.off)==='1'},
  setOff(v){localStorage.setItem(K.off,v?'1':'0')},
  all(){return ld(K.log)},
  clear(){sv(K.log,[])},
  del(ts){sv(K.log,ld(K.log).filter(e=>e.ts!==ts))},
  add(id,n,src,fmt){
    if(H.off())return;
    const l=ld(K.log),now=Date.now();
    const i=l.findIndex(e=>e.id===String(id)&&e.fmt===fmt);
    if(i>=0&&now-l[i].last<GAP){const e=l.splice(i,1)[0];
      e.to=n;e.last=now;e.cnt=(e.cnt||1)+1;e.src=src||e.src;l.unshift(e)}
    else l.unshift({id:String(id),fmt:fmt,from:n,to:n,src:src||'',ts:now,last:now,cnt:1});
    sv(K.log,l.slice(0,400));
  },
  last(id,fmt){return ld(K.log).find(e=>e.id===String(id)&&(!fmt||e.fmt===fmt))||null}
};
window.Hist=H;

function dayKey(ts){const d=new Date(ts);return d.getFullYear()+'-'+d.getMonth()+'-'+d.getDate()}
function dayName(ts){const d=new Date(ts),n=new Date(),y=new Date(n-864e5);
  if(dayKey(ts)===dayKey(+n))return'Сегодня';if(dayKey(ts)===dayKey(+y))return'Вчера';
  return d.toLocaleDateString('ru',{day:'numeric',month:'long'})}
function hhmm(ts){return new Date(ts).toLocaleTimeString('ru',{hour:'2-digit',minute:'2-digit'})}
function dur(e){const m=Math.round((e.last-e.ts)/60000);return m<1?'меньше минуты':m<60?m+' мин':Math.floor(m/60)+' ч '+(m%60)+' мин'}
function toast(m){let t=document.getElementById('sp-toast');
 if(!t){t=document.createElement('div');t.id='sp-toast';t.className='sp-toast';document.body.appendChild(t)}
 t.textContent=m;t.classList.add('on');clearTimeout(t._x);t._x=setTimeout(()=>t.classList.remove('on'),2200)}

let F='all',Q='';
function render(){
  const box=document.getElementById('hist');if(!box)return;
  let l=H.all();
  if(F!=='all')l=l.filter(e=>itm(e.id).type===F);
  if(Q)l=l.filter(e=>itm(e.id).t.toLowerCase().indexOf(Q.toLowerCase())>=0);
  const head='<div class="chips">'+[['all','Всё'],['manga','Манга'],['anime','Аниме'],['ranobe','Ранобэ']]
    .map(c=>'<button class="chip'+(F===c[0]?' on':'')+'" data-f="'+c[0]+'">'+c[1]+'</button>').join('')+
    '<button class="chip'+(H.off()?' on':'')+'" id="h-inc">'+(H.off()?'Инкогнито вкл':'Инкогнито')+'</button></div>'+
    '<div class="h-search"><input id="h-q" placeholder="Поиск по истории" value="'+esc(Q)+'"></div>';
  if(!l.length){box.innerHTML=head+'<div class="mg-empty">'+(H.all().length?'Ничего не найдено.':'История пуста.<br><span>Откройте любую главу — она появится здесь.</span>')+'</div>';bind();return}
  let out='',cur='';
  l.forEach(e=>{
    const k=dayKey(e.last);if(k!==cur){cur=k;out+='<div class="sechead"><span>'+dayName(e.last)+'</span></div>'}
    const it=itm(e.id),u=UNIT[it.type]||'гл.',rng=e.from===e.to?u+' '+e.to:u+' '+e.from+'→'+e.to;
    out+='<div class="h-row" data-ts="'+e.ts+'" data-id="'+esc(e.id)+'">'+
      '<div class="h-cov" style="'+(it.g?'background:'+it.g:'')+'"></div>'+
      '<div class="h-i"><b>'+esc(it.t)+'</b>'+
      '<span>'+rng+' · '+hhmm(e.last)+(e.cnt>1?' · '+dur(e):'')+'</span>'+
      '<span class="mut">'+(e.src?esc(e.src):'источник не сохранён')+'</span></div>'+
      '<button class="h-x" data-x="'+e.ts+'">✕</button></div>'});
  box.innerHTML=head+out+'<button class="h-clear">Очистить историю</button>';bind();
}
function bind(){
  document.querySelectorAll('#hist [data-f]').forEach(b=>b.onclick=()=>{F=b.dataset.f;render()});
  const inc=document.getElementById('h-inc');if(inc)inc.onclick=()=>{H.setOff(!H.off());toast(H.off()?'Запись истории выключена':'Запись включена');render()};
  const q=document.getElementById('h-q');if(q)q.oninput=()=>{Q=q.value;const p=q.selectionStart;render();
    const n=document.getElementById('h-q');if(n){n.focus();n.setSelectionRange(p,p)}};
  document.querySelectorAll('#hist [data-x]').forEach(b=>b.onclick=e=>{e.stopPropagation();H.del(+b.dataset.x);render();toast('Запись удалена')});
  document.querySelectorAll('#hist .h-row').forEach(r=>r.onclick=()=>location.href='title.html?id='+encodeURIComponent(r.dataset.id));
  const c=document.querySelector('.h-clear');if(c)c.onclick=()=>{if(confirm('Удалить всю историю?')){H.clear();render()}};
}
function hook(){
  if(!window.SrcPick||SrcPick._hooked)return;SrcPick._hooked=1;
  const o=SrcPick.setProg;SrcPick.setProg=function(id,n){
    const p=SrcPick.pick(id),it=itm(id);H.add(id,n,p.src?p.src.name:'',it.type);return o.apply(this,arguments)};
}
function boot(){hook();render();setTimeout(hook,400)}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',boot):boot();
})();
