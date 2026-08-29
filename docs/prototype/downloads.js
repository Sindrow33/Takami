(function(){
var K='dlq', FILT=0, box=document.getElementById('q'), tick=null;
function ls(){try{return JSON.parse(localStorage.getItem(K)||'null')}catch(e){return null}}
function save(v){localStorage.setItem(K,JSON.stringify(v))}
function cap(){return (window.Cfg&&Cfg.get&&+Cfg.get('cacheMb'))||2048}

function seed(){
  var q=[],n=0;
  DB.items.forEach(function(it){
    var base=parseInt(String(it.sub).replace(/[^0-9]+/g,''),10)||10;
    for(var k=0;k<3;k++){
      var num=base+k, st=n===0?'downloading':n<2?'queued':n===2?'error':n<5?'ready':'paused';
      q.push({k:it.id+'-'+num,id:it.id,n:num,st:st,p:st==='downloading'?18:st==='ready'?100:st==='paused'?46:0,
              mb:st==='ready'?40+((num*7)%60):0,size:40+((num*7)%60),rd:0});
      n++}});
  return q;
}
var Q=ls()||seed(); save(Q);

function used(){var s=0;Q.forEach(function(t){s+=t.mb});return s}
function lbl(t){var it=App.item(t.id);return (it.type==='anime'?'Эп. ':'Гл. ')+t.n}
function stName(s){return {queued:'в очереди',downloading:'скачивается',paused:'пауза',
  error:'ошибка сети',ready:'готово'}[s]}

function drawCap(){
  var u=used(),c=cap(),pc=Math.min(100,Math.round(u/c*100));
  document.getElementById('capb').style.width=pc+'%';
  document.getElementById('capb').className=pc>90?'over':'';
  document.getElementById('capt').textContent=u+' МБ из '+c+' МБ · '+
    Q.filter(function(t){return t.st==='ready'}).length+' готово';
}
function draw(){
  var rows=Q.filter(function(t){
    if(FILT==='act')return t.st==='downloading'||t.st==='queued'||t.st==='paused';
    if(FILT==='ready')return t.st==='ready';
    if(FILT==='err')return t.st==='error';
    return true});
  box.innerHTML=rows.length?rows.map(function(t){
    var fr=App.fr(App.fidOf(t.id));
    return '<div class="dl-row '+t.st+'" data-k="'+t.k+'">'+
      '<i class="dl-cov" style="background:'+fr.bg+'"></i>'+
      '<div class="dl-bd"><b>'+fr.t+'</b><span>'+lbl(t)+' · '+App.item(t.id).src+'</span>'+
      '<div class="progress"><i style="width:'+t.p+'%"></i></div>'+
      '<em>'+stName(t.st)+(t.st==='downloading'?' · '+t.p+'%':
        t.st==='ready'?' · '+t.mb+' МБ':'')+'</em></div>'+
      '<button class="dl-act" data-a="'+(t.st==='error'?'retry':t.st==='ready'?'del':
        t.st==='paused'?'go':'stop')+'" data-k="'+t.k+'">'+
      (t.st==='error'?'↻':t.st==='ready'?'✕':t.st==='paused'?'▶':'⏸')+'</button></div>'}).join('')
    :'<div class="sr-none">Очередь пуста<span>Добавьте главы с карточки тайтла</span></div>';
  box.querySelectorAll('.dl-act').forEach(function(b){b.onclick=function(){act(b.dataset.a,b.dataset.k)}});
  drawCap();
}
function find(k){return Q.filter(function(t){return t.k===k})[0]}
function act(a,k){
  var t=find(k); if(!t)return;
  if(a==='stop'){t.st='paused';App.toast('Пауза')}
  else if(a==='go'){t.st='downloading';App.toast('Продолжаю')}
  else if(a==='retry'){t.st='queued';t.p=0;App.toast('Повтор загрузки')}
  else if(a==='del'){var i=Q.indexOf(t);Q.splice(i,1);
    App.toast('Удалено · '+t.mb+' МБ освобождено',function(){Q.splice(i,0,t);save(Q);draw()})}
  save(Q);draw();
}
function step(){
  var run=Q.filter(function(t){return t.st==='downloading'});
  if(!run.length){
    var nx=Q.filter(function(t){return t.st==='queued'})[0];
    if(nx)nx.st='downloading';
  }
  Q.forEach(function(t){
    if(t.st!=='downloading')return;
    t.p+=6+Math.floor(Math.random()*9);
    if(t.p>=100){t.p=100;t.st='ready';t.mb=t.size}});
  save(Q);draw();
}
document.getElementById('pauseAll').onclick=function(){
  var any=Q.some(function(t){return t.st==='downloading'||t.st==='queued'});
  Q.forEach(function(t){
    if(any&&(t.st==='downloading'||t.st==='queued'))t.st='paused';
    else if(!any&&t.st==='paused')t.st='queued'});
  save(Q);draw();App.toast(any?'Все на паузе':'Загрузка возобновлена');
};
document.getElementById('clean').onclick=function(){
  var was=Q.slice(),f=used();
  Q=Q.filter(function(t){return t.st!=='ready'});save(Q);draw();
  App.toast('Освобождено '+(f-used())+' МБ',function(){Q=was;save(Q);draw()});
};
document.getElementById('add5').onclick=function(){
  var it=DB.items[0],base=Q.length?Math.max.apply(null,Q.map(function(t){return t.n}))+1:50;
  for(var i=0;i<5;i++)Q.push({k:it.id+'-'+(base+i),id:it.id,n:base+i,st:'queued',p:0,mb:0,
    size:40+((base+i)*7)%60,rd:0});
  save(Q);draw();App.toast('5 глав в очереди');
};
App.chips(document.getElementById('f'),function(n){
  FILT={'Все':0,'Активные':'act','Готово':'ready','Ошибки':'err'}[n];draw()});
draw(); tick=setInterval(step,900);
})();
