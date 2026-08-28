(function(){
document.addEventListener('pointerdown',function(e){
  const t=e.target.closest('button,.card,.h-row,.c-row,.chrow,.mr,.sp-src');
  if(!t||t.classList.contains('no-rip'))return;
  t.classList.add('rip');
  const r=t.getBoundingClientRect(),d=Math.max(r.width,r.height),s=document.createElement('span');
  s.className='rp';s.style.width=s.style.height=d+'px';
  s.style.left=(e.clientX-r.left-d/2)+'px';s.style.top=(e.clientY-r.top-d/2)+'px';
  t.appendChild(s);setTimeout(()=>s.remove(),540);
},{passive:true});

window.Anim={
  count(el,to,ms){const from=0,t0=performance.now();ms=ms||600;
    (function f(t){const p=Math.min(1,(t-t0)/ms),e=1-Math.pow(1-p,3);
      el.textContent=Math.round(from+(to-from)*e);if(p<1)requestAnimationFrame(f)})(t0)},
  stagger(sel){document.querySelectorAll(sel).forEach(e=>e.classList.add('stg'))},
  shake(el){el.classList.remove('shake');void el.offsetWidth;el.classList.add('shake')}
};
function boot(){
  Anim.stagger('.grid,.mg-list,.st-g');
  const io=window.IntersectionObserver&&new IntersectionObserver(es=>es.forEach(x=>{
    if(x.isIntersecting){x.target.style.animation='fadeUp var(--d-slow) var(--ease-out) both';io.unobserve(x.target)}
  }),{rootMargin:'0px 0px -8% 0px'});
  if(io)document.querySelectorAll('.sechead').forEach(e=>io.observe(e));
}
document.readyState==='loading'?document.addEventListener('DOMContentLoaded',boot):boot();
})();
