(function(){
var h=new Date().getHours();
document.getElementById('hi').textContent=
  h<5?'Доброй ночи':h<12?'Доброе утро':h<18?'Добрый день':'Добрый вечер';
try{var a=JSON.parse(localStorage.getItem('acct')||'null');
  if(a&&a.name)document.getElementById('nm').textContent=a.name}catch(e){}

var last=App.cont();
var fr=App.fr(App.fidOf(last.id));
document.getElementById('hero').innerHTML=
  '<i class="hc" style="background:'+fr.bg+'"></i><div class="hb">'+
  '<span class="hs">Продолжить</span><b>'+fr.t+'</b>'+
  '<span class="hs">'+last.sub+' · '+last.src+'</span>'+
  '<div class="progress" style="margin-top:2px"><i style="width:'+last.prog+'%"></i></div>'+
  '<button class="go">Продолжить</button></div>';
document.getElementById('hero').querySelector('.go').onclick=function(){
  App.mark(last);location.href=App.read(last)};

function cnt(k){try{return (JSON.parse(localStorage.getItem(k)||'[]')).length}catch(e){return 0}}
var dl=0;try{dl=(JSON.parse(localStorage.getItem('dlq')||'[]'))
  .filter(function(t){return t.st!=='ready'}).length}catch(e){}
document.getElementById('quick').innerHTML=
 '<a href="updates.html"><i>↻</i>Обновления<em>'+(Feed.unread()||'—')+'</em></a>'+
 '<a href="calendar.html"><i>▦</i>Календарь<em>сегодня</em></a>'+
 '<a href="downloads.html"><i>⤓</i>Загрузки<em>'+(dl||'—')+'</em></a>'+
 '<a href="stats.html"><i>◆</i>Прогресс<em>серия</em></a>';

function rail(t,ttl,link,f){
  var list=DB.items.filter(f);
  if(!list.length)return '';
  return '<div class="hm-sec"><h3>'+ttl+'</h3><a href="'+link+'">Все ›</a></div>'+
    '<div class="hm-rail">'+list.map(function(x){
      var f2=App.fr(App.fidOf(x.id));
      return '<a href="title.html?id='+x.id+'"><div class="cv" style="background:'+f2.bg+'">'+
        (x.badge&&x.badge!=='off'&&x.badge!=='err'?'<span class="bd">'+x.badge+'</span>':'')+
        (x.prog?'<span class="pr"><i style="width:'+x.prog+'%"></i></span>':'')+
        '</div><div class="nm">'+f2.t+'</div></a>'}).join('')+'</div>';
}
document.getElementById('body').innerHTML=
  rail('r','Продолжить чтение','index.html',function(x){return x.prog>0&&x.prog<100})+
  rail('m','Манга','index.html?f=manga',function(x){return x.type==='manga'})+
  rail('a','Аниме','index.html?f=anime',function(x){return x.type==='anime'})+
  rail('n','Ранобэ','index.html?f=novel',function(x){return x.type==='novel'});
})();
