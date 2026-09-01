// AI Indicator — индикатор обучаемости автопарсера
// Мини-график активности + процент. Тап → раскрывается bottom-sheet с деталями.
function AiIndicator() {
  const [open, setOpen] = React.useState(false);
  const [pct, setPct] = React.useState(72);

  // Немного анимируем процент (обучение идёт)
  React.useEffect(() => {
    const h = setInterval(() => {
      setPct(p => {
        const next = p + (Math.random() > 0.6 ? 1 : 0);
        return Math.min(99, next);
      });
    }, 8000);
    return () => clearInterval(h);
  }, []);

  return (
    <>
      <button
        className="ai-hdr"
        onClick={() => setOpen(true)}
        aria-label={`ИИ автопарсер · обучен на ${pct}%`}
        title={`ИИ автопарсер · ${pct}%`}
      >
        <span className="ai-hdr-ico" aria-hidden="true">
          <Icon name="brain" />
        </span>
        <span className="ai-hdr-chart" aria-hidden="true">
          <i></i><i></i><i></i><i></i><i></i>
        </span>
        <span className="ai-hdr-pct">{pct}%</span>
      </button>

      {open && <AiIndicatorSheet pct={pct} onClose={() => setOpen(false)} />}
    </>
  );
}

function AiIndicatorSheet({ pct, onClose }) {
  const c = 2 * Math.PI * 38;
  const dashOffset = c * (1 - pct / 100);

  const stats = [
    { l: 'Источников', v: '14', hint: 'парсеров активно' },
    { l: 'Самопочинок', v: '38', hint: 'за 30 дней', tone: 'ok' },
    { l: 'Точность', v: '96%', hint: 'по последним 500 запросам', tone: 'ok' },
    { l: 'Аномалий', v: '2', hint: 'ждут разметки', tone: 'warn' },
  ];

  const log = [
    { t: '3 мин', m: 'ReadManga · сменилась структура кнопок глав',    k: 'w' },
    { t: '18 мин', m: 'AniLibria · автовосстановление плейлиста',       k: 'ok' },
    { t: '1 ч',   m: 'RanobeLib · обновлена модель парсинга v2.14',    k: 't' },
    { t: '3 ч',   m: 'Shikimori · синхронизация трекера',              k: 'ok' },
    { t: '7 ч',   m: 'MintManga · перебалансирован таймаут (12→14 c)', k: 't' },
  ];

  return (
    <div className="ai-sheet-wrap" onClick={onClose}>
      <div className="ai-sheet-scrim"></div>
      <div className="ai-sheet" onClick={(e) => e.stopPropagation()}>
        <div className="ai-sheet-h">
          <span className="ai-hdr-ico"><Icon name="brain" /></span>
          <div>
            <b>Автопарсер · обучаемость</b>
            <span>Самовосстанавливающийся движок. Учится на каждом запросе.</span>
          </div>
          <button className="ai-sheet-close" onClick={onClose} aria-label="Закрыть">
            <Icon name="close" />
          </button>
        </div>

        <div className="ai-sheet-progress">
          <div className="ai-ring">
            <svg viewBox="0 0 92 92">
              <defs>
                <linearGradient id="ai-grad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stopColor="#A78BFA" />
                  <stop offset="100%" stopColor="#5B3BE8" />
                </linearGradient>
              </defs>
              <circle className="bg" cx="46" cy="46" r="38" />
              <circle
                className="fg"
                cx="46" cy="46" r="38"
                strokeDasharray={c}
                strokeDashoffset={dashOffset}
              />
            </svg>
            <div className="ai-ring-num">{pct}<em>%</em></div>
          </div>
          <div className="ai-sheet-prog-txt">
            <b>Средний уровень уверенности</b>
            <span>
              Модель учится на успешных и провальных парсингах,
              подстраивает селекторы и таймауты. Данные не покидают устройство.
            </span>
          </div>
        </div>

        <div className="ai-sheet-stats">
          {stats.map((s, i) => (
            <div key={i} className={"ai-stat" + (s.tone ? ' ' + s.tone : '')}>
              <b>{s.v}</b>
              <span>{s.l} · {s.hint}</span>
            </div>
          ))}
        </div>

        <div className="ai-sheet-log">
          {log.map((l, i) => (
            <div key={i}>
              <span className="log-t">[{l.t}]</span>{' '}
              <span className={l.k === 'ok' ? 'log-ok' : l.k === 'w' ? 'log-w' : ''}>{l.m}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

window.AiIndicator = AiIndicator;
