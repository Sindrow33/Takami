(function(){
const K='acct',S='sess';
const esc=s=>String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
function dig(s){let h=5381;for(let i=0;i<s.length;i++)h=((h<<5)+h+s.charCodeAt(i))>>>0;return h.toString(36)}
const ld=k=>{try{return JSON.parse(localStorage.getItem(k))}catch(e){return null}};
const sv=(k,v)=>localStorage.setItem(k,JSON.stringify(v));
const A={
  user(){return ld(S)},
  guest(){return !ld(S)},
  signup(n,e,p){const a=ld(K)||{};if(a[e])return{err:'Аккаунт с такой почтой уже есть'};
    a[e]={n:n,h:dig(p+e),at:Date.now()};sv(K,a);sv(S,{n:n,e:e,at:Date.now()});return{ok:1}},
  login(e,p){const a=ld(K)||{};if(!a[e])return{err:'Аккаунт не найден'};
    if(a[e].h!==dig(p+e))return{err:'Неверный пароль'};sv(S,{n:a[e].n,e:e,at:Date.now()});return{ok:1}},
  out(){localStorage.removeItem(S)}
};
window.Auth=A;

function strength(p){
  let s=0;if(p.length>=8)s++;if(p.length>=12)s++;
  if(/[a-zа-я]/.test(p)&&/[A-ZА-Я]/.test(p))s++;if(/\d/.test(p))s++;if(/[^\w\s]/.test(p))s++;
  return Math.min(4,s)}
const SL=['слишком короткий','слабый','средний','хороший','надёжный'];
const okMail=v=>/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v);

let M='login',busy=false;
function render(){
  const box=document.getElementById('au');if(!box)return;
  const u=A.user();
  if(u){box.innerHTML=
    '<div class="au-me stg"><div class="au-av">'+esc(u.n[0].toUpperCase())+'</div>'+
    '<b>'+esc(u.n)+'</b><span>'+esc(u.e)+'</span>'+
    '<div class="au-sync"><i class="au-ok">✓</i>Синхронизация включена<em>библиотека · прогресс · закладки</em></div>'+
    '<div class="au-sync"><i>○</i>Не синхронизируется<em>история · загруженные файлы · настройки устройства</em></div>'+
    '<button class="mg-go" onclick="location.href=\'index.html\'">К библиотеке</button>'+
    '<button class="st-a dn" id="au-out">Выйти из аккаунта</button></div>';
    document.getElementById('au-out').onclick=()=>{A.out();render()};return}

  const tabs='<div class="au-tabs"><i class="au-ink" style="transform:translateX('+(M==='login'?0:100)+'%)"></i>'+
    '<button data-m="login" class="'+(M==='login'?'on':'')+'">Вход</button>'+
    '<button data-m="reg" class="'+(M==='reg'?'on':'')+'">Регистрация</button></div>';
  const f=(id,lb,tp,ac)=>'<label class="au-f"><input id="'+id+'" type="'+tp+'" placeholder=" " autocomplete="'+ac+'">'+
    '<span>'+lb+'</span><em class="au-e"></em></label>';
  const body=M==='login'
   ? f('e','Почта','email','username')+f('p','Пароль','password','current-password')+
     '<button class="au-link" id="au-forgot">Забыли пароль?</button>'
   : f('n','Имя','text','nickname')+f('e','Почта','email','username')+f('p','Пароль','password','new-password')+
     '<div class="au-str"><div class="au-sb"><i></i><i></i><i></i><i></i></div><span>введите пароль</span></div>'+
     f('p2','Повторите пароль','password','new-password')+
     '<label class="au-chk"><input type="checkbox" id="au-tos"><span>Соглашаюсь с правилами сообщества: без пиратских ссылок в комментариях и без травли</span></label>';

  box.innerHTML='<div class="au-hero"><div class="au-logo">◈</div><b>'+(M==='login'?'С возвращением':'Создать аккаунт')+'</b>'+
    '<span>Аккаунт нужен только для синхронизации между устройствами. Читать можно и без него.</span></div>'+
    tabs+'<form class="au-form stg" id="au-form" novalidate>'+body+
    '<button class="mg-go" id="au-go" type="submit">'+(M==='login'?'Войти':'Зарегистрироваться')+'</button></form>'+
    '<div class="au-or"><i></i>или<i></i></div>'+
    '<button class="au-ghost" id="au-guest">Продолжить без аккаунта</button>'+
    '<div class="sp-note">Прототип хранит данные в браузере — это не настоящая авторизация. В сборке вход уходит на сервер, пароль на устройстве не сохраняется.</div>';
  bind();
}
function err(id,msg){
  const l=document.getElementById(id).parentNode;
  l.classList.toggle('bad',!!msg);l.querySelector('.au-e').textContent=msg||'';return !msg}
function bind(){
  document.querySelectorAll('[data-m]').forEach(b=>b.onclick=()=>{M=b.dataset.m;render()});
  const p=document.getElementById('p');
  if(p&&M==='reg')p.oninput=()=>{
    const s=strength(p.value),w=document.querySelector('.au-str');
    w.querySelectorAll('i').forEach((x,i)=>{x.className=i<s?'l'+s:''});
    w.querySelector('span').textContent=p.value?SL[s]:'введите пароль'};
  const g=document.getElementById('au-guest');
  if(g)g.onclick=()=>{location.href='index.html'};
  const fg=document.getElementById('au-forgot');
  if(fg)fg.onclick=e=>{e.preventDefault();
    const v=(document.getElementById('e')||{}).value||'';
    toast(okMail(v)?'Письмо отправлено на '+v:'Введите почту — пришлём ссылку')};
  const fm=document.getElementById('au-form');
  if(fm)fm.onsubmit=ev=>{ev.preventDefault();if(busy)return;
    const E=document.getElementById('e').value.trim(),P=document.getElementById('p').value;
    let ok=err('e',okMail(E)?'':'Похоже на опечатку в адресе');
    ok=err('p',P.length>=8?'':'Минимум 8 символов')&&ok;
    if(M==='reg'){
      ok=err('n',document.getElementById('n').value.trim().length>=2?'':'Как к вам обращаться?')&&ok;
      ok=err('p2',document.getElementById('p2').value===P?'':'Пароли не совпадают')&&ok;
      if(!document.getElementById('au-tos').checked){ok=false;toast('Нужно принять правила')}}
    const btn=document.getElementById('au-go');
    if(!ok){Anim.shake(fm);return}
    busy=true;btn.innerHTML='<i class="spin ld"></i>';
    setTimeout(()=>{
      const r=M==='login'?A.login(E,P):A.signup(document.getElementById('n').value.trim(),E,P);
      busy=false;
      if(r.err){btn.textContent=M==='login'?'Войти':'Зарегистрироваться';Anim.shake(fm);toast(r.err);return}
      toast(M==='login'?'С возвращением':'Аккаунт создан');
      setTimeout(render,500);
    },900)};
}
function toast(m){let t=document.getElementById('sp-toast');
 if(!t){t=document.createElement('div');t.id='sp-toast';t.className='sp-toast';document.body.appendChild(t)}
 t.textContent=m;t.classList.add('on');clearTimeout(t._x);t._x=setTimeout(()=>t.classList.remove('on'),2200)}

function badge(){
  const h=document.querySelector('.appbar');if(!h||document.getElementById('au-b'))return;
  const u=A.user(),b=document.createElement('button');b.id='au-b';b.className='ib au-b';
  b.innerHTML=u?'<i class="au-av sm">'+esc(u.n[0].toUpperCase())+'</i>':'<i class="au-av sm gh">?</i>';
  b.onclick=()=>location.href='auth.html';h.appendChild(b);
}
function boot(){render();if(!document.getElementById('au'))badge()}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',boot):boot();
})();
