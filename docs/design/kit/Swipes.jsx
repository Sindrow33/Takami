// Swipes — Tinder-подобный подбор тайтлов (манга / аниме / ранобэ).
// Свайп влево = «мимо», вправо = «в библиотеку». Механика на pointer-событиях.
function Swipes({ onBack }) {
  const [filter, setFilter] = React.useState('all');
  const [toast, setToast] = React.useState(null);
  const [likes, setLikes] = React.useState(0);
  const [nopes, setNopes] = React.useState(0);

  // Расширенный пул с большим количеством карточек
  const pool = React.useMemo(() => ([
    { id: 's1', type: 'manga', t: 'Клинок души',   au: 'Такэда Р.',  y: 2023, r: 8.7, g: ['Экшен','Сёнэн','Фэнтези'],
      bg: 'linear-gradient(150deg,#3B2A6B,#141821)', d: 'Юный мечник ищет клинок, способный резать саму судьбу.' },
    { id: 's2', type: 'anime', t: 'Полночный экспресс', au: 'Studio Kagura', y: 2026, r: 8.1, g: ['Мистика','Триллер'],
      bg: 'linear-gradient(150deg,#123A4B,#141821)', d: 'Поезд, который ходит только между 3:14 и 3:15 ночи.' },
    { id: 's3', type: 'novel', t: 'Хроники серой башни', au: 'Игараси М.', y: 2021, r: 7.9, g: ['Тёмное фэнтези','Магия'],
      bg: 'linear-gradient(150deg,#4B2740,#141821)', d: 'Башня, которую нельзя пройти — только дописать до конца.' },
    { id: 's4', type: 'manga', t: 'Кафе на краю мира', au: 'Аоки Ю.', y: 2022, r: 9.1, g: ['Слайс','Комедия'],
      bg: 'linear-gradient(150deg,#1F4636,#141821)', d: 'Тихое место, куда заходят только те, кто заблудился всерьёз.' },
    { id: 's5', type: 'anime', t: 'Гроза над Хакодате', au: 'Studio Tenshi', y: 2026, r: 0, g: ['Драма','Историческое'],
      bg: 'linear-gradient(150deg,#4A3A16,#141821)', d: 'Три сестры, одна тайна и лето, которого не будет снова.' },
    { id: 's6', type: 'novel', t: 'Оборотная сторона гор', au: 'Сато К.', y: 2024, r: 8.4, g: ['Приключения','Эпик'],
      bg: 'linear-gradient(150deg,#25325A,#141821)', d: 'Проводник обещает вывести — но не туда, куда шли.' },
    { id: 's7', type: 'manga', t: 'Стеклянные крылья', au: 'Хаяма И.', y: 2025, r: 8.9, g: ['Романтика','Сэйнэн'],
      bg: 'linear-gradient(150deg,#4A1F1F,#141821)', d: 'Художница и её натурщик — которого не существует.' },
  ]), []);

  const items = React.useMemo(
    () => pool.filter(x => filter === 'all' || x.type === filter),
    [pool, filter]
  );
  const [index, setIndex] = React.useState(0);
  React.useEffect(() => { setIndex(0); }, [filter]);

  const showToast = (kind, name) => {
    setToast({ kind, name });
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => setToast(null), 1800);
  };

  const advance = (dir) => {
    const cur = items[index];
    if (!cur) return;
    if (dir === 'right') {
      setLikes(v => v + 1);
      showToast('like', cur.t);
    } else {
      setNopes(v => v + 1);
      showToast('nope', cur.t);
    }
    setIndex(i => i + 1);
  };

  const reset = () => setIndex(0);

  return (
    <div className="sw-root">
      <AppBar
        back={onBack}
        title="Свайпы"
        actions={[{ icon: 'info', onClick: () => setToast({ kind: 'like', name: 'Свайп влево — мимо, вправо — в библиотеку' }) }]}
      />

      <div className="sw-sub">
        <span>Влево — мимо, вправо — в библиотеку</span>
        <em className="sw-count">
          <i className="sw-c-like">♥ {likes}</i>
          <i className="sw-c-nope">✕ {nopes}</i>
        </em>
      </div>

      <div className="filter-tabs sw-tabs">
        {['all','manga','anime','novel'].map(k => (
          <button key={k} className={"chip" + (filter === k ? ' on' : '')}
                  onClick={() => setFilter(k)}>
            {k === 'all' ? 'Всё' : DB.typeName[k]}
          </button>
        ))}
      </div>

      <div className="sw-stage">
        {index >= items.length ? (
          <div className="sw-empty">
            <div className="sw-empty-glyph"><Icon name="spark2" /></div>
            <b>Пока это всё</b>
            <span>Здесь появятся новые тайтлы, когда парсеры их подтянут.</span>
            <button className="sw-empty-btn" onClick={reset}>Пройти заново</button>
          </div>
        ) : (
          items
            .slice(index, index + 3)
            .reverse()
            .map((item, idxInStack) => {
              const depth = items.slice(index, index + 3).length - 1 - idxInStack;
              const isTop = depth === 0;
              return (
                <SwipeCard
                  key={item.id}
                  item={item}
                  depth={depth}
                  isTop={isTop}
                  onDecide={advance}
                />
              );
            })
        )}
      </div>

      <div className="sw-actions">
        <button className="sw-btn sw-btn-no"
                onClick={() => index < items.length && advance('left')}
                aria-label="Мимо">
          <svg viewBox="0 0 24 24" className="icn"><path d="M6 6l12 12M18 6L6 18"/></svg>
        </button>
        <button className="sw-btn sw-btn-info" aria-label="Подробнее"
                onClick={() => setToast({ kind: 'like', name: 'Открываем страницу тайтла' })}>
          <svg viewBox="0 0 24 24" className="icn">
            <circle cx="12" cy="12" r="9"/><path d="M12 8h.01M11 12h1v5h1"/>
          </svg>
        </button>
        <button className="sw-btn sw-btn-yes"
                onClick={() => index < items.length && advance('right')}
                aria-label="В библиотеку">
          <svg viewBox="0 0 24 24" className="icn"><path d="M12 21s-8-5.5-8-11a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 5.5-8 11-8 11z" fill="currentColor" stroke="none"/></svg>
        </button>
      </div>

      {toast && (
        <div className={"sw-toast " + toast.kind}>
          <b>{toast.kind === 'like' ? 'В библиотеку' : 'Пропущено'}</b>
          <span>{toast.name}</span>
        </div>
      )}
    </div>
  );
}

function SwipeCard({ item, depth, isTop, onDecide }) {
  const cardRef = React.useRef(null);
  const [dx, setDx] = React.useState(0);
  const [dy, setDy] = React.useState(0);
  const [dragging, setDragging] = React.useState(false);
  const [flying, setFlying] = React.useState(null); // 'left' | 'right' | null
  const startRef = React.useRef(null);

  const onDown = (e) => {
    if (!isTop || flying) return;
    const p = e.touches ? e.touches[0] : e;
    startRef.current = { x: p.clientX, y: p.clientY };
    setDragging(true);
  };
  const onMove = (e) => {
    if (!dragging || !startRef.current) return;
    const p = e.touches ? e.touches[0] : e;
    setDx(p.clientX - startRef.current.x);
    setDy(p.clientY - startRef.current.y);
  };
  const onUp = () => {
    if (!dragging) return;
    setDragging(false);
    const threshold = 90;
    if (dx > threshold) {
      setFlying('right');
      setTimeout(() => onDecide('right'), 240);
    } else if (dx < -threshold) {
      setFlying('left');
      setTimeout(() => onDecide('left'), 240);
    } else {
      setDx(0); setDy(0);
    }
  };

  React.useEffect(() => {
    if (!dragging) return;
    const mv = (e) => onMove(e);
    const up = () => onUp();
    window.addEventListener('mousemove', mv);
    window.addEventListener('mouseup', up);
    window.addEventListener('touchmove', mv, { passive: true });
    window.addEventListener('touchend', up);
    return () => {
      window.removeEventListener('mousemove', mv);
      window.removeEventListener('mouseup', up);
      window.removeEventListener('touchmove', mv);
      window.removeEventListener('touchend', up);
    };
  });

  const rot = Math.max(-18, Math.min(18, dx / 12));
  const yesOp = Math.max(0, Math.min(1, dx / 100));
  const noOp = Math.max(0, Math.min(1, -dx / 100));

  let transform;
  if (flying === 'right') transform = 'translate(600px,' + (dy) + 'px) rotate(28deg)';
  else if (flying === 'left') transform = 'translate(-600px,' + (dy) + 'px) rotate(-28deg)';
  else if (isTop) transform = `translate(${dx}px, ${dy}px) rotate(${rot}deg)`;
  else transform = `translateY(${depth * 10}px) scale(${1 - depth * 0.04})`;

  const style = {
    transform,
    transition: dragging ? 'none' : 'transform .28s cubic-bezier(.2,.8,.2,1), opacity .28s',
    opacity: flying ? 0 : (isTop ? 1 : (1 - depth * 0.15)),
    zIndex: 10 - depth,
    pointerEvents: isTop ? 'auto' : 'none',
  };

  return (
    <div ref={cardRef} className="sw-card" style={style}
         onMouseDown={onDown} onTouchStart={onDown}>
      <div className="sw-cover" style={{ background: item.bg }}>
        <span className="sw-type">{DB.typeName[item.type]}</span>
        {item.r > 0 && <span className="sw-rate">★ {item.r.toFixed(1)}</span>}

        <span className="sw-ov sw-ov-yes" style={{ opacity: yesOp }}>В БИБЛИОТЕКУ</span>
        <span className="sw-ov sw-ov-no"  style={{ opacity: noOp  }}>МИМО</span>
      </div>
      <div className="sw-meta">
        <h4>{item.t}</h4>
        <div className="sw-au">{item.au} · {item.y}</div>
        <p className="sw-d">{item.d}</p>
        <div className="sw-tags">
          {item.g.map(g => <span key={g} className="sw-tag">{g}</span>)}
        </div>
      </div>
    </div>
  );
}

window.Swipes = Swipes;
