window.Feed=(function(){
function hs(s){s=String(s);var h=2166136261;for(var i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
function rng(sd){var x=sd||1;return function(){x^=x<<13;x^=x>>>17;x^=x<<5;x>>>=0;return x/4294967296}}
function ls(k){try{return JSON.parse(localStorage.getItem(k)||'[]')}catch(e){return[]}}
function all(){
  var out=[];
  (window.DB&&DB.items||[]).forEach(function(it){
    if(it.broken)return;
    var r=rng(hs(it.id+'u')+7),n=2+Math.floor(r()*4),t=Date.now()-Math.floor(r()*7)*36e5;
    var num=parseInt(String(it.sub).replace(/[^0-9]+/g,''),10)||10;
    for(var k=0;k<n;k++){out.push({key:it.id+'-'+num,id:it.id,num:num,t:t});
      num--;t-=(5+Math.floor(r()*40))*36e5}});
  return out.sort(function(a,b){return b.t-a.t});
}
return {all:all,unread:function(){var rd=ls('urd');
  return all().filter(function(e){return rd.indexOf(e.key)<0}).length}};
})();
