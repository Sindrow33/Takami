// Onboarding — 4 экрана перед первым входом.
// Splash (1.6c) → Политика с чекбоксом → Разрешения → Приветствие → onDone()
function Onboarding({ onDone }) {
  const [step, setStep] = React.useState('splash'); // splash | policy | perms | welcome
  const [agreed, setAgreed] = React.useState(false);
  const [perms, setPerms] = React.useState({ notify: false, storage: false, battery: false });

  // Splash → policy автоматически
  React.useEffect(() => {
    if (step !== 'splash') return;
    const h = setTimeout(() => setStep('policy'), 1800);
    return () => clearTimeout(h);
  }, [step]);

  const grant = (k) => setPerms(p => ({ ...p, [k]: true }));
  const allGranted = Object.values(perms).every(Boolean);

  const finish = () => {
    try { localStorage.setItem('takami:onboarded', '1'); } catch (e) {}
    onDone && onDone();
  };

  // Welcome — БЕЗ автоперехода. Пользователь нажимает кнопку сам,
  // чтобы посмотреть на приветствие сколько хочет.

  const dots = ['splash', 'policy', 'perms', 'welcome'];
  const curIdx = dots.indexOf(step);

  return (
    <div className={"onb onb-" + step} key={step}>
      <div className="onb-bg"></div>
      <div className="onb-jp" aria-hidden="true">
        <span>読</span>
        <span>観</span>
        <span>読</span>
        <span>物</span>
      </div>

      {step === 'splash' && <SplashStep />}
      {step === 'policy' && (
        <PolicyStep agreed={agreed} setAgreed={setAgreed}
                    curIdx={curIdx} dots={dots}
                    onNext={() => setStep('perms')} />
      )}
      {step === 'perms' && (
        <PermsStep perms={perms} grant={grant} allGranted={allGranted}
                   curIdx={curIdx} dots={dots}
                   onNext={() => setStep('welcome')} />
      )}
      {step === 'welcome' && <WelcomeStep onEnter={finish} />}
    </div>
  );
}

function SplashStep() {
  return (
    <div className="onb-content onb-splash">
      <div className="onb-logo">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3 14 9l6 1-4.5 4 1 6-4.5-3-4.5 3 1-6L4 10l6-1z"
                fill="rgba(255,255,255,.95)" stroke="none"/>
        </svg>
      </div>
      <h1>Takami</h1>
      <div className="onb-jptitle">高見 · 見る</div>
      <p className="onb-tag">Манга, аниме и ранобэ — в одном приложении, с общим прогрессом.</p>
      <div className="onb-load" aria-label="Загрузка"></div>
    </div>
  );
}

function PolicyStep({ agreed, setAgreed, onNext, curIdx, dots }) {
  return (
    <div className="onb-content">
      <ProgressDots curIdx={curIdx} total={dots.length - 1} />
      <h2 className="onb-h">Пара слов, прежде чем начнём</h2>
      <p className="onb-sub">
        Takami — открытый клиент. Мы уважаем вас и просим уважать наши условия.
      </p>

      <div className="onb-card">
        <b>Мы не хостим контент</b>
        <span>
          Приложение — только инструмент просмотра. Все главы, эпизоды и тексты
          загружаются с внешних источников через открытые парсеры. Права на контент
          принадлежат их владельцам.
        </span>
      </div>
      <div className="onb-card">
        <b>Встроенный VPN — для удобства</b>
        <span>
          Работает как обычный прокси-клиент. Мы не логируем трафик и не храним
          историю запросов на серверах. Ключи хранятся только на вашем устройстве.
        </span>
      </div>
      <div className="onb-card">
        <b>За контент отвечает источник</b>
        <span>
          Если парсер сломался или источник ушёл — это не вина приложения.
          Автопарсер попробует восстановиться, но чуда не гарантируем.
        </span>
      </div>

      <div className={"onb-check" + (agreed ? ' on' : '')} onClick={() => setAgreed(!agreed)}>
        <div className="onb-check-box">
          <svg viewBox="0 0 24 24"><path d="m5 12 5 5L20 7"/></svg>
        </div>
        <div className="onb-check-txt">
          Я прочитал(а) и согласен(-а). Претензий по контенту к приложению
          <em> не имею</em>.
        </div>
      </div>

      <button className="onb-cta" disabled={!agreed} onClick={onNext}>
        Продолжить
      </button>
    </div>
  );
}

function PermsStep({ perms, grant, allGranted, onNext, curIdx, dots }) {
  const items = [
    { k: 'notify',  icon: 'bell',    t: 'Уведомления',
      s: 'О выходе новых глав и эпизодов из вашей библиотеки.' },
    { k: 'storage', icon: 'folder',  t: 'Доступ к хранилищу',
      s: 'Для оффлайн-загрузки глав и эпизодов, экспорта бэкапов.' },
    { k: 'battery', icon: 'battery', t: 'Без экономии заряда',
      s: 'Чтобы автопарсер и загрузки не отключались в фоне.' },
  ];

  return (
    <div className="onb-content">
      <ProgressDots curIdx={curIdx} total={dots.length - 1} />
      <h2 className="onb-h">Нужны разрешения</h2>
      <p className="onb-sub">
        Дайте согласие сейчас, потом настройки можно поменять в системе.
      </p>

      {items.map(it => (
        <div key={it.k}
             className={"onb-perm" + (perms[it.k] ? ' on' : '')}
             onClick={() => grant(it.k)}>
          <div className="onb-perm-ico"><Icon name={it.icon} /></div>
          <div className="onb-perm-t">
            <b>{it.t}</b>
            <span>{it.s}</span>
          </div>
          <div className="onb-perm-btn">{perms[it.k] ? 'Выдано' : 'Разрешить'}</div>
        </div>
      ))}

      <div style={{ flex: 1 }}></div>

      <button className="onb-cta" onClick={onNext}>
        {allGranted ? 'Отлично, дальше' : 'Пропустить и продолжить'}
      </button>
      {!allGranted && (
        <div className="onb-skip" onClick={onNext}>
          Некоторые функции могут работать некорректно
        </div>
      )}
    </div>
  );
}

function WelcomeStep({ onEnter }) {
  return (
    <div className="onb-content onb-welcome">
      <div className="onb-w-scene">
        {/* Задний ореол — светящееся кольцо позади персонажа */}
        <div className="onb-w-halo"></div>
        <div className="onb-w-halo-2"></div>

        {/* Летающие искры вокруг */}
        <div className="onb-w-sparkles" aria-hidden="true">
          <span></span><span></span><span></span>
          <span></span><span></span><span></span>
          <span></span><span></span>
        </div>

        {/* Полупрозрачные японские иероглифы за девушкой */}
        <div className="onb-w-kana" aria-hidden="true">
          <span>お</span>
          <span>帰</span>
          <span>り</span>
        </div>

        {/* Сама девушка — оживляется многослойной анимацией */}
        <div className="onb-w-girl">
          <img src="assets/welcome-girl.png" alt="" draggable="false" />
        </div>

        {/* Речевой пузырёк */}
        <div className="onb-w-bubble">
          <span className="onb-w-bubble-jp">お帰りなさいませ</span>
          <b className="onb-w-bubble-txt">Добро пожаловать,<br/>хозяин!</b>
          <i className="onb-w-bubble-tail"></i>
        </div>
      </div>

      <div className="onb-w-footer">
        <p className="onb-w-msg">Всё готово. Приятного чтения.</p>
        <button className="onb-cta onb-w-cta" onClick={onEnter}>
          Войти в приложение
        </button>
      </div>
    </div>
  );
}

function ProgressDots({ curIdx, total }) {
  // total = 3 (policy, perms, welcome); curIdx смещаем: splash=0 - не показываем
  const shift = 1; // splash не учитываем
  const cur = curIdx - shift;
  return (
    <div className="onb-dots" aria-hidden="true">
      {Array.from({ length: total }).map((_, i) => (
        <i key={i} className={i === cur ? 'on' : i < cur ? 'done' : ''}></i>
      ))}
    </div>
  );
}

window.Onboarding = Onboarding;
