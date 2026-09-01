// Calendar — расписание выхода глав и эпизодов.
// Логика взята из _src/prototype/calendar.js: детерминированное расписание
// по хэшу id → wd/hour/every. Здесь упрощено, без Random-jitter'а на каждый рендер.
function Calendar({ onOpenTitle }) {
  const [filter, setFilter] = React.useState('all');
  const [onlyMine, setOnlyMine] = React.useState(false);

  // Стабильный посевной генератор — чтобы расписание не мигало между рендерами
  const hash = (s) => {
    s = String(s);
    let h = 2166136261;
    for (let i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 16777619); }
    return h >>> 0;
  };
  const seeded = (sd) => {
    let x = sd || 1;
    return () => { x ^= x << 13; x ^= x >>> 17; x ^= x << 5; x >>>= 0; return x / 4294967296; };
  };

  const scheduleOf = (item) => {
    const r = seeded(hash(item.id) + 31);
    const st = r();
    const status = st < 0.12 ? 'finished' : st < 0.24 ? 'hiatus' : 'ongoing';
    const wd = Math.floor(r() * 7);
    const hour = 10 + Math.floor(r() * 12);
    const min = r() < 0.5 ? 0 : 30;
    const every = r() < 0.78 ? 7 : 14;
    const num = 5 + Math.floor(r() * 120);
    const late = r() < 0.18;
    return { wd, hour, min, every, status, num, late };
  };

  // 14 дней, начиная с понедельника этой недели
  const strip = React.useMemo(() => {
    const mon = new Date();
    mon.setHours(0, 0, 0, 0);
    mon.setDate(mon.getDate() - ((mon.getDay() + 6) % 7));
    return Array.from({ length: 14 }, (_, i) => new Date(+mon + i * 86400000));
  }, []);

  const [selected, setSelected] = React.useState(() => {
    const d = new Date(); d.setHours(0, 0, 0, 0); return +d;
  });

  const items = DB.items.filter(i => filter === 'all' || i.type === filter);

  // Для дня — генерируем все релизы, попадающие на этот день
  const releasesFor = (dayTs) => {
    const day = new Date(dayTs);
    const out = [];
    items.forEach(it => {
      const s = scheduleOf(it);
      if (s.status !== 'ongoing') return;
      if (day.getDay() !== s.wd) return;
      const time = new Date(day);
      time.setHours(s.hour, s.min, 0, 0);
      out.push({ item: it, s, time: +time, chap: s.num });
    });
    return out.sort((a, b) => a.time - b.time);
  };

  // Счётчики точек под днями — теперь по типам (anime/manga/novel)
  const typesFor = React.useMemo(() => {
    const c = {};
    strip.forEach(d => {
      const rels = releasesFor(+d);
      const set = new Set();
      rels.forEach(r => set.add(r.item.type));
      // order: anime, manga, novel — стабильная слева-направо
      const arr = ['anime','manga','novel'].filter(t => set.has(t));
      c[+d] = arr;
    });
    return c;
  }, [strip, filter]);

  // Ближайший релиз для hero
  const nearest = React.useMemo(() => {
    const now = Date.now();
    let best = null;
    for (let d = 0; d < 14; d++) {
      const dayTs = +strip[0] + d * 86400000;
      const list = releasesFor(dayTs);
      for (const r of list) {
        if (r.time > now && (!best || r.time < best.time)) best = r;
      }
    }
    return best;
  }, [strip, filter]);

  const paused = items.map(it => ({ it, s: scheduleOf(it) })).filter(x => x.s.status !== 'ongoing');

  const dayReleases = releasesFor(selected);
  const monthNames = ['января','февраля','марта','апреля','мая','июня','июля','августа','сентября','октября','ноября','декабря'];
  const weekdays = ['воскресенье','понедельник','вторник','среда','четверг','пятница','суббота'];
  const selDate = new Date(selected);

  const timeLeft = (ts) => {
    const m = Math.round((ts - Date.now()) / 60000);
    if (m < 60) return m + ' мин';
    const h = Math.floor(m / 60);
    if (h < 24) return h + ' ч';
    return Math.floor(h / 24) + ' дн';
  };

  const stateOf = (ts, s) => {
    const now = Date.now();
    if (ts > now) return 'soon';
    if (s.late && now - ts < 3 * 86400000) return 'late';
    return 'out';
  };
  const stateLabel = { out: 'вышло', late: 'задержка', soon: 'ожидается' };

  const [toast, setToast] = React.useState(null);
  const tt = (m) => { setToast(m); clearTimeout(tt._h); tt._h = setTimeout(() => setToast(null), 1500); };

  return (
    <div className="screen-scroll">
      <AppBar
        title="Календарь"
        actions={[
          { icon: 'refresh', onClick: () => tt('Расписание обновлено · +2 релиза') },
          { icon: 'menu',    onClick: () => tt('Показать только: моя библиотека / все источники') }
        ]}
      />

      {nearest && (
        <div className="cal-hero" onClick={() => onOpenTitle && onOpenTitle(nearest.item)}>
          <div className="cal-hero-cv" style={{ background: DB.fr[DB.fidOf[nearest.item.id]].bg }}></div>
          <div className="cal-hero-t">
            <span className="cal-hero-lbl">Ближайший релиз</span>
            <b>{nearest.item.t}</b>
            <span className="cal-hero-sub">
              {nearest.item.type === 'anime' ? 'Эпизод' : 'Глава'} {nearest.chap} · через {timeLeft(nearest.time)}
            </span>
          </div>
          <div className="cal-hero-time">
            {new Date(nearest.time).toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' })}
          </div>
        </div>
      )}

      <div className="filter-tabs cal-filters">
        {['all','manga','anime','novel'].map(k => (
          <button key={k} className={"chip" + (filter === k ? ' on' : '')}
                  onClick={() => setFilter(k)}>
            {k === 'all' ? 'Всё' : DB.typeName[k]}
          </button>
        ))}
        <button className={"chip" + (onlyMine ? ' on' : '')}
                onClick={() => setOnlyMine(v => !v)}>
          Только моё
        </button>
      </div>

      <div className="cal-legend" aria-hidden="true">
        <span><i className="tt-anime"></i>аниме</span>
        <span><i className="tt-manga"></i>манга</span>
        <span><i className="tt-novel"></i>ранобэ</span>
      </div>

      <div className="cal-strip">
        {strip.map(d => {
          const dts = +d;
          const isSel = dts === selected;
          const isToday = (() => {
            const t = new Date(); t.setHours(0,0,0,0); return +t === dts;
          })();
          const types = typesFor[dts] || [];
          const wd = ['вс','пн','вт','ср','чт','пт','сб'][d.getDay()];
          const typeLabels = types.map(t => t === 'anime' ? 'аниме' : t === 'manga' ? 'манга' : 'ранобэ').join(', ');
          return (
            <button key={dts}
                    className={"cal-day" + (isSel ? ' on' : '') + (isToday ? ' today' : '')}
                    onClick={() => setSelected(dts)}>
              <span className="cal-day-wd">{wd}</span>
              <b className="cal-day-n">{d.getDate()}</b>
              {types.length > 0
                ? <em className="cal-day-dots" aria-label={'релизы: ' + typeLabels}>
                    {types.map(t => <i key={t} className={"tt-" + t}></i>)}
                  </em>
                : <em className="cal-day-dots off" aria-label="релизов нет"></em>}
            </button>
          );
        })}
      </div>

      <div className="cal-sechead">
        <span>
          {weekdays[selDate.getDay()]}, {selDate.getDate()} {monthNames[selDate.getMonth()]}
        </span>
        <em>{dayReleases.length}</em>
      </div>

      <div className="cal-list">
        {dayReleases.length === 0 && (
          <div className="cal-empty">
            <b>В этот день релизов нет</b>
            <span>Расписание берётся из источника и может сдвигаться</span>
          </div>
        )}
        {dayReleases.map((r, i) => {
          const st = stateOf(r.time, r.s);
          return (
            <div key={i} className={"cal-row " + st}
                 onClick={() => onOpenTitle && onOpenTitle(r.item)}>
              <div className="cal-time tnum">
                {new Date(r.time).toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' })}
              </div>
              <div className="cal-cv" style={{ background: DB.fr[DB.fidOf[r.item.id]].bg }}></div>
              <div className="cal-info">
                <b>{r.item.t}</b>
                <span>
                  {r.item.type === 'anime' ? 'Эпизод' : 'Глава'} {r.chap}
                  {' · '}
                  {r.s.every === 14 ? 'раз в 2 недели' : 'еженедельно'}
                </span>
              </div>
              <span className={"cal-badge " + st}>{stateLabel[st]}</span>
            </div>
          );
        })}
      </div>

      {paused.length > 0 && (
        <>
          <div className="cal-sechead">
            <span>Без расписания</span>
            <em>{paused.length}</em>
          </div>
          <div className="cal-list">
            {paused.map(({ it, s }) => (
              <div key={it.id} className="cal-row flat"
                   onClick={() => onOpenTitle && onOpenTitle(it)}>
                <div className="cal-cv" style={{ background: DB.fr[DB.fidOf[it.id]].bg }}></div>
                <div className="cal-info">
                  <b>{it.t}</b>
                  <span className={s.status === 'hiatus' ? 'warn' : ''}>
                    {s.status === 'hiatus'
                      ? 'Хиатус — выпуск приостановлен'
                      : `Завершён · ${s.num} всего`}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <div className="cal-note">
        Даты расчётные: строятся по среднему интервалу выпусков источника.
        Точное время публикации сайты почти никогда не отдают.
      </div>
      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Calendar = Calendar;
