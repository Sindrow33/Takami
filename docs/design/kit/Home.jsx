// Home — обновлённая главная (v3).
// Ключевые изменения:
// 1) Крупный hero-continue c обложкой на всю ширину, стеклянная плашка снизу.
// 2) Компактный ряд «быстрых действий» без коробок — как рельс глифов.
// 3) Новый блок «Новости аниме» — горизонтальный рельс новостных карточек.
// 4) Рельсы тайтлов сохранены, но каждая карточка получила rating/type-иконку.
function Home({ onGo, onOpenTitle }) {
  const cont = DB.items.find(x => x.prog > 0 && x.prog < 100 && !x.broken) || DB.items[0];
  const fr = DB.fr[DB.fidOf[cont.id]];

  const rails = [
    { t: 'Продолжить', sub: 'вы читаете', link: 'library', f: (x) => x.prog > 0 && x.prog < 100 && !x.broken },
    { t: 'Манга',      sub: 'популярное',  link: 'library', f: (x) => x.type === 'manga' },
    { t: 'Аниме',      sub: 'сейчас идёт', link: 'library', f: (x) => x.type === 'anime' },
    { t: 'Ранобэ',     sub: 'подборка',    link: 'library', f: (x) => x.type === 'novel' },
  ];

  const hour = new Date().getHours();
  const hi = hour < 5 ? 'Доброй ночи' : hour < 12 ? 'Доброе утро' : hour < 18 ? 'Добрый день' : 'Добрый вечер';

  const dateStr = new Date().toLocaleDateString('ru', { weekday: 'long', day: 'numeric', month: 'long' });

  const [newsIdx, setNewsIdx] = React.useState(0);
  const [toast, setToast] = React.useState(null);
  const t = (m) => { setToast(m); clearTimeout(t._h); t._h = setTimeout(() => setToast(null), 1600); };

  const typeIcon = (type) => type === 'anime' ? 'play' : type === 'novel' ? 'bookOpen' : 'book';

  return (
    <div className="screen-scroll hm3">
      {/* Верхняя приветственная шапка — без бордюров, лёгкая */}
      <header className="hm3-top">
        <div className="hm3-top-l">
          <span className="hm3-date">{dateStr}</span>
          <h1 className="hm3-hi">{hi}<span>,&nbsp;Читатель</span></h1>
        </div>
        <div className="hm3-top-r">
          {(() => { const AI = window.AiIndicator; return AI ? <AI /> : null; })()}
          <button className="hm3-ico" title="Поиск" aria-label="Поиск"
                  onClick={() => onGo && onGo('search')}><Icon name="search" /></button>
          <button className="hm3-ico" title="Настройки" aria-label="Настройки"
                  onClick={() => onGo && onGo('settings')}><Icon name="settings" /></button>
        </div>
      </header>

      {/* Hero-continue: обложка на всю ширину, glass-плитка снизу */}
      <div className="hm3-hero" onClick={() => onOpenTitle && onOpenTitle(cont)}
           role="button" aria-label={"Открыть " + fr.t}>
        <i className="hm3-hero-cv" style={{ background: fr.bg }}></i>
        <span className="hm3-hero-tag">Продолжить · {cont.type === 'anime' ? 'аниме' : cont.type === 'novel' ? 'ранобэ' : 'манга'}</span>
        <div className="hm3-hero-glass">
          <div className="hm3-hero-txt">
            <b>{fr.t}</b>
            <span>{cont.sub} · {cont.src}</span>
            <div className="hm3-hero-bar">
              <i style={{ width: cont.prog + '%' }}></i>
              <em className="tnum">{cont.prog}%</em>
            </div>
          </div>
          <button className="hm3-hero-btn"
                  onClick={(e) => { e.stopPropagation(); onGo && onGo(cont.type === 'anime' ? 'player' : 'reader'); }}>
            <Icon name={cont.type === 'anime' ? 'play' : 'book'} />
            {cont.type === 'anime' ? 'Смотреть' : 'Читать'}
          </button>
        </div>
      </div>

      {/* Быстрые действия — без коробок, глифы + подпись */}
      <div className="hm3-quick">
        <button className="hm3-qk" onClick={() => onGo && onGo('library')}>
          <span className="g"><Icon name="refresh" /></span><em>Обновления</em><b>3</b>
        </button>
        <button className="hm3-qk" onClick={() => onGo && onGo('calendar')}>
          <span className="g"><Icon name="calendar" /></span><em>Календарь</em><b>сегодня</b>
        </button>
        <button className="hm3-qk" onClick={() => onGo && onGo('search')}>
          <span className="g"><Icon name="search" /></span><em>Поиск</em><b>по кадру</b>
        </button>
        <button className="hm3-qk" onClick={() => onGo && onGo('swipes')}>
          <span className="g"><Icon name="swipes" /></span><em>Свайпы</em><b>подбор</b>
        </button>
      </div>

      {/* Новости аниме — крупная карусель */}
      <div className="hm3-sec">
        <div className="hm3-sechead">
          <div className="hm3-sechead-t">
            <h3>Новости аниме</h3>
            <span>что происходит в индустрии</span>
          </div>
          <a onClick={() => t('Открываем ленту новостей аниме-индустрии')}>Все&nbsp;›</a>
        </div>
        <div className="hm3-news"
             onScroll={(e) => {
               const el = e.currentTarget;
               const w = el.firstChild ? el.firstChild.offsetWidth + 12 : 1;
               setNewsIdx(Math.round(el.scrollLeft / w));
             }}>
          {DB.news.map((n, i) => (
            <article key={n.id} className={"hm3-news-card tone-" + n.tone}
                     onClick={() => t('Открываем: ' + n.t)}>
              <i className="hm3-news-cv" style={{ background: n.bg }}></i>
              <span className={"hm3-news-cat tone-" + n.tone}>{n.cat}</span>
              <div className="hm3-news-txt">
                <b>{n.t}</b>
                <span>{n.sub}</span>
              </div>
              <div className="hm3-news-meta">
                <em>{n.src}</em>
                <span>·</span>
                <em>{n.tm}</em>
              </div>
            </article>
          ))}
        </div>
        <div className="hm3-news-dots">
          {DB.news.map((_, i) => (
            <i key={i} className={i === newsIdx ? 'on' : ''}></i>
          ))}
        </div>
      </div>

      {/* Рельсы тайтлов */}
      {rails.map((rail, idx) => {
        const list = DB.items.filter(rail.f);
        if (!list.length) return null;
        return (
          <div className="hm3-sec" key={idx}>
            <div className="hm3-sechead">
              <div className="hm3-sechead-t">
                <h3>{rail.t}</h3>
                <span>{rail.sub}</span>
              </div>
              <a onClick={() => onGo && onGo(rail.link)}>Все&nbsp;›</a>
            </div>
            <div className="hm3-rail">
              {list.map(x => {
                const f2 = DB.fr[DB.fidOf[x.id]];
                const showBadge = x.badge && x.badge !== 'off' && x.badge !== 'err';
                return (
                  <a key={x.id} className="hm3-card"
                     onClick={() => onOpenTitle ? onOpenTitle(x) : (onGo && onGo('reader'))}>
                    <div className="hm3-card-cv" style={{ background: f2.bg }}>
                      <span className="hm3-card-type"><Icon name={typeIcon(x.type)} /></span>
                      {showBadge && <span className="hm3-card-bd">{x.badge}</span>}
                      {x.prog > 0 && x.prog < 100 && (
                        <span className="hm3-card-pr"><i style={{ width: x.prog + '%' }}></i></span>
                      )}
                    </div>
                    <div className="hm3-card-nm">{f2.t}</div>
                    {x.r > 0 && <div className="hm3-card-r">★ {x.r.toFixed(1)}</div>}
                  </a>
                );
              })}
            </div>
          </div>
        );
      })}

      <div style={{ height: 20 }}></div>

      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Home = Home;
