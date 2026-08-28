(function(){
const K='cfg';
const D={
 readerMode:'webtoon', keepScreen:true, volKeys:false, tapZones:true,
 autoSource:true, preferLang:'ru', minChapters:true, wifiOnly:true,
 history:true, spoilers:true, blurNsfw:true,
 notifyRelease:true, quietFrom:23, quietTo:8, groupNotify:true,
 gamify:true, freeze:true, weekStart:1,
 cacheMb:512, autoDeleteRead:false
};
let S={};try{S=Object.assign({},D,JSON.parse(localStorage.getItem(K))||{})}catch(e){S=Object.assign({},D)}
const subs=[];
const C={
 def:D,
 get(k){return S[k]},
 all(){return Object.assign({},S)},
 set(k,v){S[k]=v;localStorage.setItem(K,JSON.stringify(S));subs.forEach(f=>{try{f(k,v)}catch(e){}});
   if(k==='history')localStorage.setItem('histOff',v?'0':'1');},
 on(f){subs.push(f)},
 reset(){S=Object.assign({},D);localStorage.setItem(K,JSON.stringify(S));subs.forEach(f=>f('*',null))},
 dump(){const o={};for(let i=0;i<localStorage.length;i++){const k=localStorage.key(i);o[k]=localStorage.getItem(k)}return o}
};
if(localStorage.getItem('histOff')===null)localStorage.setItem('histOff',S.history?'0':'1');
window.Cfg=C;
})();
