// Search.jsx — единый экран поиска (Текст / Скриншот / Голос)
function Search({ onBack, onOpenTitle, onOpenAdvanced, activeFilters }) {
  const [tab, setTab] = React.useState('text');
  const [query, setQuery] = React.useState('');
  const [shot, setShot] = React.useState(null); // null | 'analyzing' | 'done'
  const [drag, setDrag] = React.useState(false);
  const filterCount = activeFilters || 0;

  const recents = [
    { q: 'клинок души', t: 'сегодня' },
    { q: 'приключения фэнтези', t: 'вчера' },
    { q: 'MAPPA 2024', t: 'вчера' },
    { q: 'Юки Кадзи', t: '3 дня назад' }
  ];

  const suggestions = [
    { it: DB.items[0], score: 96 },
    { it: DB.items[1], score: 88 },
    { it: DB.items[4], score: 74 },
    { it: DB.items[5], score: 62 }
  ];

  // Screenshot matches after "analysis" completes
  const shotMatches = shot === 'done' ? [
    { it: DB.items[0], score: 98, reason: 'Кадр 12:04 · совпадение объектов' },
    { it: DB.items[1], score: 71, reason: 'Похожий стиль обложки' },
    { it: DB.items[4], score: 42, reason: 'Стиль сцены' }
  ] : [];

  const startShotAnalysis = () => {
    setShot('analyzing');
    setTimeout(() => setShot('done'), 2000);
  };

  return (
    <div className="screen-scroll sr-scroll">
      <AppBar back={onBack} title="Поиск" actions={[
        { icon: 'menu', onClick: () => onOpenAdvanced && onOpenAdvanced() }
      ]} />

      {/* Tabs */}
      <div className="sr-tabs">
        <button className={"sr-tab" + (tab === 'text' ? ' on' : '')}
                onClick={() => setTab('text')}>
          <Icon name="search" className="xs" /> Текст
        </button>
        <button className={"sr-tab" + (tab === 'shot' ? ' on' : '')}
                onClick={() => setTab('shot')}>
          <svg viewBox="0 0 24 24" className="icn xs">
            <rect x="3" y="6" width="18" height="14" rx="2"/>
            <circle cx="12" cy="13" r="4"/>
            <path d="M8 6l2-3h4l2 3"/>
          </svg>
          Скриншот
        </button>
        <button className={"sr-tab" + (tab === 'voice' ? ' on' : '')}
                onClick={() => setTab('voice')}>
          <svg viewBox="0 0 24 24" className="icn xs">
            <rect x="9" y="3" width="6" height="12" rx="3"/>
            <path d="M5 11a7 7 0 0 0 14 0M12 18v3"/>
          </svg>
          Голос
        </button>
      </div>

      {/* Text search */}
      {tab === 'text' && (
        <>
          <div className="sr-inputbar">
            <Icon name="search" className="sm" />
            <input
              placeholder="Название, автор, студия..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoFocus
            />
            <button className={"sr-fl" + (filterCount > 0 ? ' active' : '')}
                    onClick={onOpenAdvanced}>
              Фильтры
              {filterCount > 0 && <span className="sr-fl-count">{filterCount}</span>}
            </button>
          </div>

          {/* Trending chips */}
          <div className="sr-chip-row">
            {['Новинки 2026','Тёмное фэнтези','Слайс','MAPPA','Романтика','Психология'].map(x => (
              <button key={x} className="chip" onClick={() => setQuery(x)}>{x}</button>
            ))}
          </div>

          {/* Recent */}
          {!query && (
            <>
              <div className="ch-sechead" style={{ padding: '8px 16px 6px' }}>Недавние запросы</div>
              <div className="sr-recent">
                {recents.map((r, i) => (
                  <div key={i} className="sr-recent-item" onClick={() => setQuery(r.q)}>
                    <Icon name="refresh" className="sm" />
                    <span className="rq">{r.q}</span>
                    <span className="rt">{r.t}</span>
                  </div>
                ))}
              </div>
            </>
          )}

          {/* Live suggestions */}
          {query && (
            <>
              <div className="ch-sechead" style={{ padding: '8px 16px 6px' }}>
                Найдено: {suggestions.length}
              </div>
              <div className="sr-match">
                {suggestions.map(({ it, score }) => {
                  const fr = DB.fr[DB.fidOf[it.id]];
                  return (
                    <div key={it.id} className="sr-match-row"
                         onClick={() => onOpenTitle && onOpenTitle(it)}>
                      <div className="sr-match-cv" style={{ background: fr.bg }}></div>
                      <div className="sr-match-t">
                        <b>{fr.t}</b>
                        <span>{DB.typeName[it.type]} · {it.src} · {it.y}</span>
                      </div>
                      <span className={"sr-match-score" + (score < 70 ? ' mid' : '')}>
                        {score}%
                      </span>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </>
      )}

      {/* Screenshot search */}
      {tab === 'shot' && (
        <>
          {shot === null && (
            <div className={"sr-shot" + (drag ? ' drag' : '')}
                 onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
                 onDragLeave={() => setDrag(false)}
                 onDrop={(e) => { e.preventDefault(); setDrag(false); startShotAnalysis(); }}>
              <div className="sr-shot-glyph">
                <svg viewBox="0 0 24 24" className="icn" style={{ width: 26, height: 26, strokeWidth: 1.8 }}>
                  <rect x="3" y="6" width="18" height="14" rx="2"/>
                  <circle cx="12" cy="13" r="4"/>
                  <path d="M8 6l2-3h4l2 3"/>
                </svg>
              </div>
              <h4>Поиск по кадру</h4>
              <p>
                Загрузите скриншот — ИИ определит тайтл по кадру,<br/>
                стилю рисовки или сцене.
              </p>
              <div className="sr-shot-actions">
                <button className="sr-shot-btn primary" onClick={startShotAnalysis}>
                  <svg viewBox="0 0 24 24" className="icn">
                    <rect x="3" y="6" width="18" height="14" rx="2"/>
                    <circle cx="12" cy="13" r="4"/>
                  </svg>
                  Камера
                </button>
                <button className="sr-shot-btn" onClick={startShotAnalysis}>
                  <svg viewBox="0 0 24 24" className="icn">
                    <rect x="3" y="4" width="18" height="16" rx="2"/>
                    <circle cx="8" cy="9" r="1.5"/>
                    <path d="m3 17 5-5 4 4 3-3 6 6"/>
                  </svg>
                  Из галереи
                </button>
              </div>
              <div className="sr-shot-hint">
                Или перетащите файл сюда · JPG · PNG · WEBP
              </div>
            </div>
          )}

          {shot === 'analyzing' && (
            <>
              <div className="sr-preview">
                <div className="sr-preview-img"></div>
                <div className="sr-preview-t">
                  <div className="k">ИИ анализирует</div>
                  <b>screenshot_042.jpg</b>
                  <span>Поиск совпадений в базе...</span>
                </div>
                <button className="sr-preview-cancel" onClick={() => setShot(null)}>×</button>
              </div>
              <div className="ch-sechead" style={{ padding: '8px 16px 6px' }}>Стадия анализа</div>
              <div className="sr-match">
                {['Извлечение объектов','Сравнение стиля рисовки','Поиск в 6 источниках','Оценка совпадений'].map((s, i) => (
                  <div key={i} className="sr-recent-item" style={{ background: 'rgba(124,92,255,.04)' }}>
                    <span className="rt" style={{ color: i < 3 ? 'var(--ok)' : 'var(--acc-2)' }}>
                      {i < 3 ? '✓' : '···'}
                    </span>
                    <span className="rq">{s}</span>
                  </div>
                ))}
              </div>
            </>
          )}

          {shot === 'done' && (
            <>
              <div className="sr-preview">
                <div className="sr-preview-img" style={{ animation: 'none', background: 'linear-gradient(150deg,#3B2A6B,#141821)' }}></div>
                <div className="sr-preview-t">
                  <div className="k" style={{ color: 'var(--ok)' }}>Готово</div>
                  <b>screenshot_042.jpg</b>
                  <span>Найдено 3 совпадения</span>
                </div>
                <button className="sr-preview-cancel" onClick={() => setShot(null)}>×</button>
              </div>
              <div className="ch-sechead" style={{ padding: '8px 16px 6px' }}>Совпадения</div>
              <div className="sr-match">
                {shotMatches.map(({ it, score, reason }) => {
                  const fr = DB.fr[DB.fidOf[it.id]];
                  return (
                    <div key={it.id} className="sr-match-row"
                         onClick={() => onOpenTitle && onOpenTitle(it)}>
                      <div className="sr-match-cv" style={{ background: fr.bg }}></div>
                      <div className="sr-match-t">
                        <b>{fr.t}</b>
                        <span>{reason}</span>
                      </div>
                      <span className={"sr-match-score" + (score < 70 ? ' mid' : '')}>{score}%</span>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </>
      )}

      {/* Voice */}
      {tab === 'voice' && (
        <div className="sr-voice">
          <div className="sr-voice-mic">
            <svg viewBox="0 0 24 24" style={{ width: 40, height: 40, fill: 'currentColor' }}>
              <rect x="9" y="3" width="6" height="12" rx="3"/>
              <path d="M5 11a7 7 0 0 0 14 0M12 18v3" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
            </svg>
          </div>
          <div className="sr-voice-hint">
            <b>Слушаю...</b>
            <span>Скажите название или произнесите цитату из тайтла</span>
          </div>
          <div className="sr-voice-eq">
            <i></i><i></i><i></i><i></i><i></i><i></i><i></i>
          </div>
          <div style={{ fontSize: 11, color: 'var(--on-surface-variant)', textAlign: 'center', marginTop: 6 }}>
            «...если клинок молчит...»
          </div>
        </div>
      )}
    </div>
  );
}

window.Search = Search;
