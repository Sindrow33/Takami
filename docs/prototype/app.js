window.App={
 q:(k,d)=>new URLSearchParams(location.search).get(k)||d,
 item(id){return DB.items.find(x=>x.id==id)||DB.items[0]},
 char(id){return DB.chars.find(x=>x.id==id)||DB.chars[0]},
 read(it){return DB.reader[it.type]+"?id="+it.id},
 fidOf(id){return DB.fidOf[id]},
 fr(f){return DB.fr[f]},
 frItems(f){return DB.fr[f].items.map(App.item)},
 frOfItem(id){return DB.fr[DB.fidOf[id]]},
 back(){history.length>1?history.back():location.href="index.html"},
 sheet(v){var s=document.getElementById('sh'),c=document.getElementById('sc');
  s&&s.classList.toggle('open',!!v);c&&c.classList.toggle('open',!!v)},
 _tt:null,
 toast(msg,undoFn){var t=document.getElementById('toast');
  if(!t){t=document.createElement('div');t.id='toast';t.className='toast';
   t.innerHTML='<span id="tmsg"></span><span style="flex:1"></span>'+
    '<button class="chip" id="tundo">Отменить</button>';document.body.appendChild(t)}
  t.querySelector('#tmsg').textContent=msg;
  var b=t.querySelector('#tundo');b.style.display=undoFn?'':'none';
  b.onclick=function(){undoFn&&undoFn();t.classList.remove('on')};
  t.classList.add('on');clearTimeout(App._tt);
  App._tt=setTimeout(function(){t.classList.remove('on')},2600)},
 chips(box,fn){box.onclick=function(e){var c=e.target.closest('.chip');if(!c)return;
  [].forEach.call(box.children,function(x){x.removeAttribute('aria-selected')});
  c.setAttribute('aria-selected','true');fn&&fn(c.textContent.trim())}},
 nav(cur){var it=[["library","index.html","Библиотека","▤",0],
  ["updates","updates.html","Обновления","↻",0],["fab","swipe.html","","＋",0],
  ["news","news.html","Новости","✦",1],["sources","sources.html","Источники","⛭",0]];
  document.body.insertAdjacentHTML('beforeend','<nav class="tabbar">'+it.map(function(x){
   return x[0]==='fab'?'<a class="fab" href="'+x[1]+'"><div class="fabbtn">＋</div></a>':
   '<a class="'+(cur===x[0]?'active':'')+'" href="'+x[1]+'"><span class="ico">'+x[3]+
   '</span>'+(x[4]?'<span class="dot"></span>':'')+'<span>'+x[2]+'</span></a>'})
   .join('')+'</nav>')}
};
