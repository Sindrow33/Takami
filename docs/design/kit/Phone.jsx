// Phone frame + status bar + AppBar + TabBar
function Phone({ label, immersive, landscape, children }) {
  return (
    <div style={{ position: 'relative' }}>
      <div className={"phone" + (landscape ? ' landscape' : '')}>
        {!landscape && (
          <div className="phone-cap">
            <span>9:41</span>
            <div className="r">
              <span>••</span>
              <span>▲</span>
              <div className="bat"></div>
            </div>
          </div>
        )}
        <div className={"screen" + (immersive ? " immersive" : "")}>
          {children}
        </div>
      </div>
      {label && <div className="phone-caption">{label}</div>}
    </div>
  );
}

// TabBar: ink + halo, идентичный preview/18-tabbar.html
function TabBar({ active, onNav, badge, fabLoading }) {
  const barRef = React.useRef(null);
  const inkRef = React.useRef(null);
  const haloRef = React.useRef(null);
  const tabRefs = React.useRef({});

  const tabs = [
    { k: 'home',     icon: 'home',     label: 'Главная' },
    { k: 'library',  icon: 'library',  label: 'Библиотека' },
    { k: 'fab',      icon: 'spark',    label: '' },
    { k: 'calendar', icon: 'calendar', label: 'Календарь' },
    { k: 'more',     icon: 'settings', label: 'Настройки' },
  ];

  const place = React.useCallback(() => {
    const bar = barRef.current, ink = inkRef.current, halo = haloRef.current;
    if (!bar || !ink || !halo) return;
    const activeK = active === 'more' ? 'more' : active;
    const el = tabRefs.current[activeK];
    if (!el) { halo.style.opacity = '0'; return; }
    const br = bar.getBoundingClientRect();
    const tr = el.getBoundingClientRect();
    const cx = tr.left - br.left + tr.width / 2;
    ink.style.left  = (cx - ink.offsetWidth / 2)  + 'px';
    halo.style.left = (cx - halo.offsetWidth / 2) + 'px';
    halo.style.opacity = '1';
  }, [active]);

  React.useLayoutEffect(() => { place(); }, [place]);
  React.useEffect(() => {
    const r = () => place();
    window.addEventListener('resize', r);
    return () => window.removeEventListener('resize', r);
  }, [place]);

  return (
    <nav className="tabbar" ref={barRef}>
      <span className="ink" ref={inkRef}></span>
      <span className="halo" ref={haloRef}></span>

      {tabs.map(t => {
        if (t.k === 'fab') {
          return (
            <div key="fab" className="tab fab-slot"
                 ref={el => { if (el) tabRefs.current['fab'] = el; }}>
              <button
                className={"fab" + (fabLoading ? ' loading' : '')}
                aria-label="Свайпы"
                aria-busy={fabLoading || undefined}
                disabled={fabLoading}
                onClick={() => onNav && onNav('fab')}
              >
                <span className="fab-ring" aria-hidden="true"></span>
                <span className="fab-icn">
                  <Icon name="swipes" />
                </span>
                <span className="fab-spinner" aria-hidden="true">
                  <svg viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10" />
                  </svg>
                </span>
              </button>
            </div>
          );
        }
        const isCal = t.k === 'calendar' && badge;
        return (
          <button
            key={t.k}
            className={"tab" + (active === t.k ? ' on' : '')}
            onClick={() => onNav && onNav(t.k)}
            ref={el => { if (el) tabRefs.current[t.k] = el; }}>
            <span className="ic"><Icon name={t.icon} /></span>
            {isCal && <em className="tb-b">{badge}</em>}
            <span className="lbl">{t.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

function AppBar({ back, title, actions, greeting }) {
  return (
    <header className="appbar">
      {back && <button className="ic-btn" onClick={back}><Icon name="back" /></button>}
      {greeting ? (
        <div className="greet">
          <div className="hi">{greeting.hi}</div>
          <div className="nm">{greeting.nm}</div>
        </div>
      ) : (
        <h1>{title}</h1>
      )}
      {(actions || []).map((a, i) => (
        <button key={i} className={"ic-btn" + (a.on ? " on" : "")} onClick={a.onClick}>
          <Icon name={a.icon} />
        </button>
      ))}
    </header>
  );
}

Object.assign(window, { Phone, TabBar, AppBar });
