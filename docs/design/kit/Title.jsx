// Экран тайтла / франшизы — fork из _src/prototype/title.html
function Title({ itemId, onBack, onRead, onOpenChar }) {
  const [descOpen, setDescOpen] = React.useState(false);
  const [inLib, setInLib] = React.useState(true);
  const [toast, setToast] = React.useState(null);
  const tt = (m) => { setToast(m); clearTimeout(tt._h); tt._h = setTimeout(() => setToast(null), 1500); };

  // itemId=1 по умолчанию — франшиза с тремя форматами
  const it  = DB.items.find(x => x.id === (itemId || 1)) || DB.items[0];
  const fid = DB.fidOf[it.id];
  const fr  = DB.fr[fid];
  const sib = fr.items.map(id => DB.items.find(x => x.id === id));

  const unit = it.type === 'anime' ? 'Эпизод' : 'Глава';
  const contLabel = it.type === 'anime' ? 'эп. 8' : 'гл. 43';
  const description = fr.d || 'Одна история в трёх форматах: манга-первоисточник, аниме-экранизация и ранобэ. Описание берётся у франшизы, а главы и прогресс — у выбранного формата.';

  // Жанры — из data.js их нет для B/C/D, зафолбэком
  const genres = fr.g || ['Экшен', 'Фэнтези'];

  // Главы — генерим 8 подряд, помечая прочитанные/сломанные
  const chapters = [];
  for (let n = 45; n >= 38; n--) {
    const bad = it.broken && n === 41;
    chapters.push({
      n,
      title: `${unit} ${n}`,
      date: bad ? 'Не загружается' : `12.0${n % 9}.2026`,
      read: n < 43,
      broken: bad,
      downloaded: n > 43,
    });
  }

  return (
    <div className="screen-scroll">
      {/* AppBar */}
      <AppBar
        back={onBack}
        title=""
        actions={[
          { icon: 'search', onClick: () => tt('Поиск главы или страницы в тайтле') },
          { icon: 'menu',   onClick: () => tt('Поделиться · Скачать всё · Отслеживать · Мигрировать') }
        ]}
      />

      {/* Hero: обложка + мета + жанры */}
      <div className="t-hero" style={{ '--franchise-bg': fr.bg }}>
        <div className="cover" style={{ background: fr.bg }}></div>
        <div className="meta">
          <h2>{fr.t}</h2>
          <div className="m-sub">Автор · Выходит</div>
          <div className="m-sub">{DB.typeName[it.type]} · {it.count} · {it.y}</div>
          <div className="m-tags">
            {genres.map(g => <span key={g} className="t-tag">{g}</span>)}
          </div>
        </div>
      </div>

      {/* Format switcher */}
      {sib.length > 1 && (
        <div className="fmtbar">
          {['manga','anime','novel'].map(k => {
            const o = sib.find(x => x.type === k);
            if (!o) return (
              <a key={k} className="off">
                <div className="k">{DB.typeName[k]}</div>
                <div className="v">нет</div>
              </a>
            );
            return (
              <a key={k} className={o.id === it.id ? 'on' : ''}>
                <div className="k">{DB.typeName[k]}</div>
                <div className="v">{o.count}</div>
              </a>
            );
          })}
        </div>
      )}

      {/* Action row */}
      <div className="t-actions">
        <button
          className={"btn" + (inLib ? " in-lib" : "")}
          onClick={() => setInLib(!inLib)}
        >
          <Icon name={inLib ? 'check' : 'plus'} className="xs" />
          {inLib ? 'В библиотеке' : 'В библиотеку'}
        </button>
        <button className="btn primary" onClick={() => onRead && onRead(it)}>
          <Icon name={it.type === 'anime' ? 'play' : 'book'} className="xs" />
          Продолжить · {contLabel}
        </button>
      </div>

      {/* Source status */}
      <div className={"t-srcbar" + (it.broken ? " err" : "")}>
        <div className="sb-row">
          <span className="sb-dot"></span>
          <div className="sb-txt">
            <b>{it.src}</b>
            <span>{it.broken ? 'парсер сломан · показаны сохранённые данные' : `активен · v1.4.2 · проверен минуту назад`}</span>
          </div>
          <button className="sb-btn" onClick={() => tt('Открываем выбор источника')}>Сменить</button>
        </div>
        {it.srcUrl && (
          <a className="t-src-link"
             href={it.srcUrl}
             target="_blank"
             rel="noopener noreferrer"
             onClick={(e) => {
               // Внутри iframe-прототипа — просто toast + попытка открыть
               tt('Открываем в браузере: ' + it.src);
             }}>
            <span className="t-src-link-i">↗</span>
            <span className="t-src-link-url">{it.srcUrl.replace(/^https?:\/\//, '')}</span>
            <span className="t-src-link-arr">›</span>
          </a>
        )}
      </div>

      {/* Broken banner */}
      {it.broken && (
        <div className="t-banner">
          <Icon name="alert" className="sm" />
          <div>Часть глав не найдена у этого источника. Можно мигрировать — прогресс сохранится.</div>
        </div>
      )}

      {/* Description */}
      <p className={"t-desc" + (descOpen ? '' : ' clamp')}>
        {description}
        {!descOpen && (
          <><br/><span className="t-desc-more" onClick={() => setDescOpen(true)}>Читать полностью ›</span></>
        )}
      </p>

      {/* Characters */}
      <div className="sechead">
        <h3>Персонажи</h3>
        <a>Все {DB.chars.length} ›</a>
      </div>
      <div className="charrail">
        {DB.chars.map((c, i) => {
          // Палитра-обложка на каждого персонажа
          const palettes = [
            'linear-gradient(155deg,#5B3BE8 0%,#2A1F55 60%,#141821 100%)',
            'linear-gradient(155deg,#E64C7A 0%,#5A1F35 60%,#141821 100%)',
            'linear-gradient(155deg,#00A6C0 0%,#0F3A48 60%,#141821 100%)',
            'linear-gradient(155deg,#C97A2E 0%,#4A2E15 60%,#141821 100%)',
            'linear-gradient(155deg,#3D8F6A 0%,#1F3A2E 60%,#141821 100%)',
            'linear-gradient(155deg,#7C5CFF 0%,#2A1F55 60%,#141821 100%)',
          ];
          const bg = palettes[i % palettes.length];
          return (
            <a key={c.id} className={"c-card" + (c.main ? ' main' : '')}
               onClick={() => onOpenChar && onOpenChar(c)}>
              <div className="c-cov" style={{ background: bg }}>
                <span className="c-glyph">{c.n.charAt(0)}</span>
                {!!c.main && <span className="c-main-tag">Главный</span>}
                <span className="c-jp">{c.jp}</span>
              </div>
              <div className="c-nm">{c.n}</div>
              <div className="c-ro">{c.role}</div>
            </a>
          );
        })}
      </div>

      {/* Chapters */}
      <div className="chaps">
        <div className="sechead" style={{ padding: '4px 0' }}>
          <h3>{it.type === 'anime' ? 'Эпизоды' : 'Главы'}</h3>
          <a>Все {it.count}</a>
        </div>
        {chapters.map(ch => (
          <div
            key={ch.n}
            className={"ch" + (ch.read ? ' read' : '') + (ch.broken ? ' broken' : '')}
            onClick={() => !ch.broken && onRead && onRead(it)}
          >
            <div className="ch-i">
              <b>{ch.title}</b>
              <span>{ch.date}</span>
            </div>
            <div className={"ch-dl" + (ch.downloaded ? ' done' : '')}>
              <Icon name={ch.downloaded ? 'check' : 'download'} className="sm" />
            </div>
          </div>
        ))}
      </div>
      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Title = Title;
