// SupportButton — широкая кнопка «Поддержать» в самом низу настроек.
// Разворачивает список ссылок на сервисы поддержки.
function SupportButton() {
  const [open, setOpen] = React.useState(false);
  const [toast, setToast] = React.useState(null);

  const links = [
    { k: 'boosty',   n: 'Boosty',       s: 'Ежемесячная подписка · подписки от 100 ₽',
      color: 'linear-gradient(145deg,#FF5A26,#E23000)', letter: 'B' },
    { k: 'tribute',  n: 'Tribute',       s: 'Через Telegram · разовые донаты',
      color: 'linear-gradient(145deg,#38B6FF,#0079E5)', letter: 'T' },
    { k: 'patreon',  n: 'Patreon',       s: 'Ежемесячная подписка · в USD',
      color: 'linear-gradient(145deg,#FF6249,#E23A20)', letter: 'P' },
    { k: 'kofi',     n: 'Ko-fi',         s: 'Одноразовые донаты · чашка кофе',
      color: 'linear-gradient(145deg,#FF5E5B,#D93A3F)', letter: 'K' },
    { k: 'crypto',   n: 'Криптовалюта',  s: 'BTC · ETH · TON · USDT',
      color: 'linear-gradient(145deg,#F7931A,#B87513)', letter: '₿' },
  ];

  const handleMove = (e) => {
    const el = e.currentTarget;
    const r = el.getBoundingClientRect();
    el.style.setProperty('--mx', (e.clientX - r.left) + 'px');
    el.style.setProperty('--my', (e.clientY - r.top) + 'px');
  };

  const open_ = (k, n) => {
    setToast('Открываем ' + n + ' …');
    clearTimeout(open_._h);
    open_._h = setTimeout(() => setToast(null), 1500);
  };

  return (
    <div className="st-support">
      <button
        className="st-support-btn"
        onClick={() => setOpen(v => !v)}
        onMouseMove={handleMove}
      >
        <svg className="heart-ico" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M12 21s-7.5-5-9.5-10.5A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 4.5C19.5 16 12 21 12 21z"/>
        </svg>
        <span>Поддержать разработку</span>
        <svg viewBox="0 0 24 24"
             style={{ width: 16, height: 16, marginLeft: 'auto', stroke: 'currentColor',
                      strokeWidth: 2, fill: 'none', strokeLinecap: 'round', strokeLinejoin: 'round',
                      transform: open ? 'rotate(180deg)' : 'none',
                      transition: 'transform .28s cubic-bezier(.2,.8,.2,1)' }}>
          <path d="m6 9 6 6 6-6"/>
        </svg>
      </button>

      {open && (
        <div className="st-support-list">
          {links.map(l => (
            <div key={l.k} className="st-support-link" onClick={() => open_(l.k, l.n)}>
              <div className="st-support-link-ico" style={{ background: l.color }}>
                {l.letter}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <b>{l.n}</b>
                <span>{l.s}</span>
              </div>
              <span className="st-support-arrow">›</span>
            </div>
          ))}
        </div>
      )}

      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.SupportButton = SupportButton;
