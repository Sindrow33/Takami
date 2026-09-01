// Character.jsx — экран биографии персонажа
// Открывается кликом по карточке персонажа в Title.
function Character({ charId, onBack, onOpenTitle, onOpenSeiyuu }) {
  const [fav, setFav] = React.useState(false);
  const [share, setShare] = React.useState(null);
  React.useEffect(() => {
    if (!share) return;
    const h = setTimeout(() => setShare(null), 1500);
    return () => clearTimeout(h);
  }, [share]);
  const c = DB.chars.find(x => x.id === (charId || 1)) || DB.chars[0];
  const seiyuu = DB.seiyuu.find(s => s.id === c.seiyuu);

  const palettes = [
    'linear-gradient(155deg,#5B3BE8 0%,#2A1F55 60%,#141821 100%)',
    'linear-gradient(155deg,#E64C7A 0%,#5A1F35 60%,#141821 100%)',
    'linear-gradient(155deg,#00A6C0 0%,#0F3A48 60%,#141821 100%)',
    'linear-gradient(155deg,#C97A2E 0%,#4A2E15 60%,#141821 100%)',
    'linear-gradient(155deg,#3D8F6A 0%,#1F3A2E 60%,#141821 100%)',
    'linear-gradient(155deg,#7C5CFF 0%,#2A1F55 60%,#141821 100%)',
  ];
  const bg = palettes[(c.id - 1) % palettes.length];

  const appearsIn = (c.appearsIn || []).map(id => DB.items.find(x => x.id === id)).filter(Boolean);

  const info = [
    ['Возраст', c.age ? c.age : '—'],
    ['Рост',    c.height || '—'],
    ['Группа',  c.bloodType || '—'],
    ['День рожд.', c.birthday || '—'],
  ];

  return (
    <div className="screen-scroll stg" style={{ '--char-bg': bg }}>
      <AppBar back={onBack} title="" actions={[
        { icon: 'search', onClick: () => setShare('Поиск: другие тайтлы с этим персонажем') },
        { icon: 'menu',   onClick: () => setShare('Поделиться · В избранное · Пожаловаться') }
      ]} />

      {/* Portrait hero */}
      <div className="ch-hero">
        <div className="ch-portrait" style={{ background: bg }}>
          <span className="ch-p-jp">{c.jp}</span>
          {c.main ? <span className="ch-p-badge">★ Главный герой</span>
                  : <span className="ch-p-badge">{c.role}</span>}
          <span className="ch-p-glyph">{c.n.charAt(0)}</span>
          <div className="ch-p-bottom">
            <div className="ch-p-name">
              <b>{c.n}</b>
              <span>{c.jp} · {c.role}</span>
            </div>
            <button className={"ch-fav-btn" + (fav ? ' on' : '')}
                    onClick={() => setFav(!fav)}
                    aria-label="В избранное">
              <svg viewBox="0 0 24 24" className="icn">
                <path d="M12 21s-8-5.5-8-11a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 5.5-8 11-8 11z"
                      fill={fav ? 'currentColor' : 'none'}/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Quick info grid */}
      <div className="ch-info-grid">
        {info.map(([k, v]) => (
          <div key={k} className="ch-info-cell">
            <div className="k">{k}</div>
            <div className="v">{v}</div>
          </div>
        ))}
      </div>

      {/* Biography */}
      <div className="ch-sechead">Биография</div>
      <div className="ch-bio">{c.bio}</div>

      {/* Meta info */}
      <div className="ch-meta">
        {c.affiliation && (
          <div className="ch-meta-row">
            <span>Принадлежность</span>
            <b>{c.affiliation}</b>
          </div>
        )}
        {c.origin && (
          <div className="ch-meta-row">
            <span>Родом из</span>
            <b>{c.origin}</b>
          </div>
        )}
        {c.zodiac && c.zodiac !== '—' && (
          <div className="ch-meta-row">
            <span>Знак зодиака</span>
            <b>{c.zodiac}</b>
          </div>
        )}
      </div>

      {/* Seiyuu */}
      {seiyuu && (
        <>
          <div className="ch-sechead">Сэйю</div>
          <div className="ch-seiyuu" onClick={() => onOpenSeiyuu && onOpenSeiyuu(seiyuu)}>
            <div className="ch-seiyuu-av">{seiyuu.n.charAt(0)}</div>
            <div className="ch-seiyuu-t">
              <div className="k">♪ Озвучка</div>
              <b>{seiyuu.n}</b>
              <span>{seiyuu.jp} · {seiyuu.roles} ролей · с {seiyuu.y} г.</span>
            </div>
            <span className="ch-seiyuu-arrow">›</span>
          </div>
        </>
      )}

      {/* Quotes */}
      {c.quotes && c.quotes.length > 0 && (
        <>
          <div className="ch-sechead">Цитаты</div>
          {c.quotes.map((q, i) => (
            <div key={i} className="ch-quote">{q}</div>
          ))}
        </>
      )}

      {/* Appears in */}
      {appearsIn.length > 0 && (
        <>
          <div className="ch-sechead">Встречается в</div>
          <div className="ch-appears">
            {appearsIn.map(it => {
              const fr = DB.fr[DB.fidOf[it.id]];
              return (
                <div key={it.id} className="ch-app-row"
                     onClick={() => onOpenTitle && onOpenTitle(it)}>
                  <div className="ch-app-cv" style={{ background: fr.bg }}></div>
                  <div className="ch-app-t">
                    <b>{fr.t}</b>
                    <span>{DB.typeName[it.type]} · {it.y}</span>
                  </div>
                  <span className="ch-app-role">
                    {c.mainIn === it.id ? 'Главный' : 'Появляется'}
                  </span>
                </div>
              );
            })}
          </div>
        </>
      )}
      {share && <div className="st-toast">{share}</div>}
    </div>
  );
}

window.Character = Character;
