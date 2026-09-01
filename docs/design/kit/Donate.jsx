// Donate.jsx — экран поддержки через Tribute.
// В интерфейсе — только Tribute (никаких Boosty/DonationAlerts/крипты).
// Две точки входа:
//   • t.me/tribute/app?startapp=dP7R — открыть внутри Telegram (Mini-App)
//   • web.tribute.tg/d/P7R           — открыть в браузере (для тех, у кого TG нет)
// Обе ссылки — прямо на страницу поддержки автора.
function Donate({ onBack }) {
  const LINK_TG  = 'https://t.me/tribute/app?startapp=dP7R';
  const LINK_WEB = 'https://web.tribute.tg/d/P7R';

  // Пользователь может ввести свою сумму, выбрать пресет или подписку
  const [amount, setAmount]     = React.useState(500);
  const [preset, setPreset]     = React.useState('500');   // 100 | 300 | 500 | 1000 | custom
  const [mode, setMode]         = React.useState('once');  // once | monthly
  const [copied, setCopied]     = React.useState(null);
  const [toast, setToast]       = React.useState(null);
  const showToast = (m) => { setToast(m); clearTimeout(showToast._h); showToast._h = setTimeout(() => setToast(null), 1800); };

  const presets = [
    { k: '100',    v: 100,    label: '100 ₽',    sub: 'кофе' },
    { k: '300',    v: 300,    label: '300 ₽',    sub: 'бенто' },
    { k: '500',    v: 500,    label: '500 ₽',    sub: 'популярное', pop: 1 },
    { k: '1000',   v: 1000,   label: '1000 ₽',   sub: 'щедро' }
  ];

  const goal = 80000;
  const raised = 54200;
  const supporters = 148;
  const pct = Math.min(100, Math.round((raised / goal) * 100));

  const openTG  = () => { showToast('Открываем Tribute в Telegram…'); setTimeout(() => window.open(LINK_TG,  '_blank', 'noopener'), 350); };
  const openWeb = () => { showToast('Открываем Tribute в браузере…'); setTimeout(() => window.open(LINK_WEB, '_blank', 'noopener'), 350); };

  const copy = async (text, key) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(key);
      setTimeout(() => setCopied(null), 1400);
    } catch (e) {
      showToast('Не удалось скопировать');
    }
  };

  const changePreset = (k, v) => {
    setPreset(k);
    if (v != null) setAmount(v);
  };

  return (
    <div className="screen-scroll dn2-scroll stg">
      <AppBar back={onBack} title="Поддержать" actions={[{ icon: 'menu', onClick: () => showToast('Поделиться · В избранное · Пожаловаться') }]} />

      {/* Hero */}
      <div className="dn2-hero">
        <div className="dn2-hero-glow"></div>
        <div className="dn2-hero-mark">
          <div className="dn2-hero-mark-inner">♥</div>
        </div>
        <h2>Takami живёт на донатах</h2>
        <div className="dn2-hero-sub">
          Без рекламы, без подписок в самом приложении, без трекеров.
          Один способ поддержать — <b>Tribute</b>. Быстро, безопасно, работает в Telegram.
        </div>

        {/* Progress */}
        <div className="dn2-progress">
          <div className="dn2-progress-labels">
            <div className="raised">
              <b className="tnum">{raised.toLocaleString('ru')} ₽</b>
              <em>из {goal.toLocaleString('ru')} ₽ · август</em>
            </div>
            <div className="goal tnum">{pct}%</div>
          </div>
          <div className="dn2-progress-bar">
            <i style={{ width: pct + '%' }}></i>
          </div>
          <div className="dn2-stats">
            <span>Поддержали: <b className="tnum">{supporters}</b></span>
            <span>·</span>
            <span>Осталось: <b>2 дня</b></span>
          </div>
        </div>
      </div>

      {/* Once / Monthly */}
      <div className="dn2-mode">
        <button className={"dn2-mode-b" + (mode === 'once' ? ' on' : '')} onClick={() => setMode('once')}>
          <b>Разовый</b>
          <span>один платёж</span>
        </button>
        <button className={"dn2-mode-b" + (mode === 'monthly' ? ' on' : '')} onClick={() => setMode('monthly')}>
          <b>Ежемесячный</b>
          <span>можно отменить в любой момент</span>
        </button>
      </div>

      {/* Amount presets */}
      <div className="dn2-sechead">
        <span>Сумма</span>
        <em>{mode === 'monthly' ? 'в месяц' : 'разово'}</em>
      </div>
      <div className="dn2-presets">
        {presets.map(p => (
          <button key={p.k}
                  className={"dn2-preset" + (preset === p.k ? ' on' : '')}
                  onClick={() => changePreset(p.k, p.v)}>
            {p.pop && <span className="dn2-preset-pop">★</span>}
            <b>{p.label}</b>
            <span>{p.sub}</span>
          </button>
        ))}
      </div>
      <div className={"dn2-custom" + (preset === 'custom' ? ' on' : '')}
           onClick={() => setPreset('custom')}>
        <label>Или своя сумма</label>
        <div className="dn2-custom-row">
          <input type="number" inputMode="numeric" min="10" step="10"
                 value={amount}
                 onFocus={() => setPreset('custom')}
                 onChange={e => setAmount(Math.max(10, Number(e.target.value) || 0))} />
          <span>₽</span>
        </div>
      </div>

      {/* Payment CTA — Tribute только */}
      <div className="dn2-sechead">
        <span>Оплата через Tribute</span>
        <em>без комиссии для проекта</em>
      </div>
      <div className="dn2-cta">
        <button className="dn2-cta-b primary" onClick={openTG}>
          <span className="ic tg">✈</span>
          <div className="t">
            <b>Открыть в Telegram</b>
            <span>Tribute Mini-App · привычно, быстро</span>
          </div>
          <span className="ar">›</span>
        </button>
        <button className="dn2-cta-b" onClick={openWeb}>
          <span className="ic web">◐</span>
          <div className="t">
            <b>Открыть в браузере</b>
            <span>если Telegram не установлен</span>
          </div>
          <span className="ar">›</span>
        </button>

        {/* Копируемые ссылки — на случай, если нужно переслать */}
        <div className="dn2-links">
          <div className="dn2-link">
            <span className="k">Telegram</span>
            <code>t.me/tribute/app?startapp=dP7R</code>
            <button onClick={() => copy(LINK_TG, 'tg')} className="cp">
              {copied === 'tg' ? '✓' : '⧉'}
            </button>
          </div>
          <div className="dn2-link">
            <span className="k">Веб</span>
            <code>web.tribute.tg/d/P7R</code>
            <button onClick={() => copy(LINK_WEB, 'web')} className="cp">
              {copied === 'web' ? '✓' : '⧉'}
            </button>
          </div>
        </div>
      </div>

      {/* Why Tribute */}
      <div className="dn2-why">
        <div className="dn2-why-head">Почему только Tribute</div>
        <div className="dn2-why-list">
          <div className="dn2-why-r">
            <span className="ic">⛨</span>
            <div>
              <b>Один провайдер — меньше поверхность</b>
              <span>Не нужно доверять карту нескольким сервисам</span>
            </div>
          </div>
          <div className="dn2-why-r">
            <span className="ic"><Icon name="spark2" /></span>
            <div>
              <b>Работает из Telegram</b>
              <span>Тот же аккаунт, что вы уже используете</span>
            </div>
          </div>
          <div className="dn2-why-r">
            <span className="ic">◇</span>
            <div>
              <b>Российские и зарубежные карты</b>
              <span>СБП, Visa/MC, Apple/Google Pay — на стороне Tribute</span>
            </div>
          </div>
          <div className="dn2-why-r">
            <span className="ic"><Icon name="refresh" /></span>
            <div>
              <b>Подписку можно отменить в один клик</b>
              <span>Прямо из чата с Tribute в Telegram</span>
            </div>
          </div>
        </div>
      </div>

      {/* Supporters wall */}
      <div className="dn2-sechead">
        <span>Стена благодарности</span>
        <em>· {supporters} за август</em>
      </div>
      <div className="dn2-wall">
        {DB.donate.thanks.map((n, i) => {
          const cls = n.includes('Gold') ? 'gold' : n.includes('Silver') ? 'silver' : '';
          return <span key={i} className={"dn2-wall-chip " + cls}>{n}</span>;
        })}
        <span className="dn2-wall-chip" style={{ opacity: .55 }}>+ ещё {supporters - DB.donate.thanks.length}</span>
      </div>

      <div className="dn2-legal">
        Tribute — сервис приёма донатов и подписок. Оплата и договорные отношения — на его стороне.
        Мы не храним данные карт.
      </div>

      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Donate = Donate;
