// Player.jsx v2 — горизонтальный плеер с полным набором контролов.
// Все настройки — в самом плеере, никакого дублирования в общих настройках.
function Player({ onBack, downloaded, onGoAiSettings }) {
  const ai = useAiStore();
  const aiReady = window.AiStore.isReady();
  const [playing, setPlaying] = React.useState(true);
  const [t, setT] = React.useState(750);
  const total = 1440;
  const fmt = (s) => Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
  const showSkip = t > 60 && t < 180;

  // sheet: null | 'subs' | 'audio' | 'quality' | 'speed' | 'settings'
  const [sheet, setSheet] = React.useState(null);

  const [subs, setSubs] = React.useState('ru');       // off / ru / en
  const [audio, setAudio] = React.useState(aiReady ? 'ru-ai' : 'ru-vo');  // ja / ru-ai / ru-dub / ru-vo
  const [quality, setQuality] = React.useState('1080p');
  const [speed, setSpeed] = React.useState(1);
  const [locked, setLocked] = React.useState(false);
  const [pip, setPip] = React.useState(false);
  const [keepAwake, setKeepAwake] = React.useState(true);
  const [cast, setCast] = React.useState(false);

  // По условию: ИИ-озвучка доступна только для скачанных эпизодов
  const isDownloaded = downloaded !== false; // default true в прототипе

  const subsList = [
    { k: 'off', n: 'Отключены' },
    { k: 'ru',  n: 'Русские (Anilibria)' },
    { k: 'en',  n: 'English (Official)' },
    { k: 'ru-forced', n: 'Русские · только надписи' }
  ];
  const audioList = [
    { k: 'ja',     n: 'Оригинал',                      hint: 'Японский · 5.1' },
    { k: 'ru-dub', n: 'Русский · Anidub',              hint: 'Дубляж · 2.0' },
    { k: 'ru-vo',  n: 'Русский · Anilibria',           hint: 'Многоголосый · 2.0' },
    { k: 'ru-ai',  n: 'Русский · ИИ-озвучка',          hint: 'Neural TTS · только для скачанных', ai: true }
  ];
  const qualities = ['4K','1080p','720p','480p','auto'];
  const speeds = [0.5, 0.75, 1, 1.25, 1.5, 2];

  return (
    <>
      <div className="pl-h-stage">
        <div className="pl-h-video">Видео</div>
      </div>

      <div className="pl-h-ov"></div>

      {/* Top bar */}
      {!locked && (
        <div className="pl-h-top">
          <button className="pl-btn" onClick={onBack} aria-label="Назад">
            <Icon name="back" />
          </button>
          <div className="pl-h-info">
            <b>Эпизод 7 · «Название серии»</b>
            <span>Аниме сериал · AniLibria {isDownloaded && '· ⤓ скачано'}</span>
          </div>
          <div className="pl-h-top-actions">
            <button className={"pl-btn" + (cast ? ' on' : '')}
                    onClick={() => setCast(!cast)} title="Cast">
              <svg viewBox="0 0 24 24" className="icn">
                <path d="M3 8V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-7"/>
                <path d="M3 12a9 9 0 0 1 9 9M3 16a5 5 0 0 1 5 5M3 20h.01"/>
              </svg>
            </button>
            <button className={"pl-btn" + (pip ? ' on' : '')}
                    onClick={() => setPip(!pip)} title="Pip">
              <svg viewBox="0 0 24 24" className="icn">
                <rect x="3" y="5" width="18" height="14" rx="2"/>
                <rect x="12" y="12" width="8" height="6" rx="1" fill="currentColor" stroke="none"/>
              </svg>
            </button>
            <button className={"pl-btn" + (keepAwake ? ' on' : '')}
                    onClick={() => setKeepAwake(!keepAwake)} title="Держать экран">
              <svg viewBox="0 0 24 24" className="icn">
                <circle cx="12" cy="12" r="4"/>
                <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4"/>
              </svg>
            </button>
            <button className="pl-btn" onClick={() => setSheet('settings')} title="Настройки">
              <Icon name="settings" />
            </button>
          </div>
        </div>
      )}

      {/* Center controls */}
      {!locked && (
        <div className="pl-h-mid">
          <button className="pl-h-ic" onClick={() => setT(Math.max(0, t - 10))} title="-10">
            <svg viewBox="0 0 24 24" className="icn">
              <path d="M9 14l-4-4 4-4"/>
              <path d="M5 10h9a5 5 0 0 1 5 5v0a5 5 0 0 1-5 5H9"/>
              <text x="10" y="19" fontSize="7" fontFamily="system-ui" fontWeight="700" fill="currentColor" stroke="none">10</text>
            </svg>
          </button>
          <button className="pl-h-ic" title="Пред."><Icon name="prev" /></button>
          <button className="pl-h-ic pl-h-play" onClick={() => setPlaying(!playing)}>
            <Icon name={playing ? 'pause' : 'play'} />
          </button>
          <button className="pl-h-ic" title="След."><Icon name="next" /></button>
          <button className="pl-h-ic" onClick={() => setT(Math.min(total, t + 10))} title="+10">
            <svg viewBox="0 0 24 24" className="icn">
              <path d="M15 14l4-4-4-4"/>
              <path d="M19 10h-9a5 5 0 0 0-5 5v0a5 5 0 0 0 5 5h5"/>
              <text x="8" y="19" fontSize="7" fontFamily="system-ui" fontWeight="700" fill="currentColor" stroke="none">10</text>
            </svg>
          </button>
        </div>
      )}

      {/* Locked overlay */}
      {locked && (
        <button className="pl-btn"
                style={{
                  position: 'absolute', left: '50%', top: '50%',
                  transform: 'translate(-50%, -50%)',
                  zIndex: 40, width: 48, height: 48
                }}
                onClick={() => setLocked(false)}>
          <svg viewBox="0 0 24 24" className="icn">
            <rect x="5" y="10" width="14" height="10" rx="2"/>
            <path d="M8 10V7a4 4 0 0 1 8 0v3"/>
          </svg>
        </button>
      )}

      {/* Skip opening */}
      {showSkip && !locked && (
        <button className="pl-h-skip" onClick={() => setT(180)}>
          <Icon name="arrowR" className="xs" />
          Пропустить опенинг
        </button>
      )}

      {/* Bottom controls */}
      {!locked && (
        <div className="pl-h-bot">
          <div className="pl-h-scrub">
            <span>{fmt(t)}</span>
            <input type="range" min="0" max={total} value={t}
                   onChange={e => setT(+e.target.value)} />
            <span>{fmt(total)}</span>
          </div>

          <div className="pl-h-bot-row">
            <div className="pl-h-quicks">
              <button className={"pl-h-q" + (subs !== 'off' ? ' on' : '')}
                      onClick={() => setSheet('subs')}>
                <svg viewBox="0 0 24 24" className="icn">
                  <rect x="3" y="6" width="18" height="12" rx="2"/>
                  <path d="M7 14h4M14 14h3M7 11h2M12 11h5"/>
                </svg>
                Сабы · {subs === 'off' ? 'выкл' : subs.toUpperCase()}
              </button>
              <button className={"pl-h-q" + (audio.startsWith('ru') ? ' on' : '')}
                      onClick={() => setSheet('audio')}>
                <svg viewBox="0 0 24 24" className="icn">
                  <path d="M12 3v18M8 7v10M4 10v4M16 5v14M20 8v8"/>
                </svg>
                Озвучка · {audio === 'ja' ? 'ориг' : audio === 'ru-ai' ? 'ИИ' : 'RU'}
              </button>
              <button className={"pl-h-q" + (audio === 'ru-ai' && isDownloaded ? ' on' : audio === 'ru-ai' && !isDownloaded ? ' disabled' : '')}
                      onClick={() => {
                        if (!isDownloaded) return;
                        setSheet('audio');
                      }}
                      title={isDownloaded ? '' : 'Доступно только для скачанных серий'}>
                <svg viewBox="0 0 24 24" className="icn">
                  <path d="M12 2 4 6v6c0 5 3.5 9 8 10 4.5-1 8-5 8-10V6z"/>
                  <text x="9" y="14" fontSize="6" fontFamily="system-ui" fontWeight="700" fill="currentColor" stroke="none">AI</text>
                </svg>
                ИИ TTS
              </button>
            </div>

            <div className="pl-h-quicks">
              <button className="pl-h-q" onClick={() => setSheet('speed')}>
                ×{speed}
              </button>
              <button className="pl-h-q" onClick={() => setSheet('quality')}>
                {quality}
              </button>
              <button className="pl-h-q" onClick={() => setLocked(true)} title="Замок жестов">
                <svg viewBox="0 0 24 24" className="icn">
                  <rect x="5" y="10" width="14" height="10" rx="2"/>
                  <path d="M8 10V7a4 4 0 0 1 8 0v3"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Right-side sheet */}
      {sheet && (
        <div className="pl-sheet">
          <div className="pl-sheet-head">
            <h4>
              {sheet === 'subs'    && 'Субтитры'}
              {sheet === 'audio'   && 'Аудио · озвучка'}
              {sheet === 'quality' && 'Качество'}
              {sheet === 'speed'   && 'Скорость'}
              {sheet === 'settings' && 'Плеер'}
            </h4>
            <button onClick={() => setSheet(null)}>✕</button>
          </div>

          {sheet === 'subs' && (
            <div className="pl-sub-list">
              {subsList.map(s => (
                <div key={s.k} className={"pl-sub-item" + (subs === s.k ? ' on' : '')}
                     onClick={() => setSubs(s.k)}>
                  <span className="dot"></span>
                  <span className="t">{s.n}</span>
                </div>
              ))}
            </div>
          )}

          {sheet === 'audio' && (
            <div className="pl-sub-list">
              {audioList.map(a => {
                const noDl = a.ai && !isDownloaded;
                const noKey = a.ai && !aiReady;
                const disabled = noDl || noKey;
                const hint = a.ai && noDl ? 'Скачайте серию · нужен общий ИИ-ключ'
                           : a.ai && noKey ? 'Добавьте API-ключ в настройках'
                           : a.hint;
                return (
                  <div key={a.k}
                       className={"pl-sub-item" + (audio === a.k ? ' on' : '') + (a.ai ? ' ai' : '') + (disabled ? ' locked' : '')}
                       onClick={() => !disabled && setAudio(a.k)}>
                    <span className="dot"></span>
                    <span className="t">
                      <div style={{ fontWeight: 500 }}>{a.n}</div>
                      <div style={{ fontSize: 10, color: 'var(--on-surface-variant)', marginTop: 2 }}>{hint}</div>
                    </span>
                    {a.ai && (disabled
                      ? <span className="icn-lock">🔒</span>
                      : <span className="badge-ai">AI</span>)}
                  </div>
                );
              })}
              {!aiReady && (
                <div className="pl-ai-cta" onClick={() => onGoAiSettings && onGoAiSettings()}>
                  <div className="pl-ai-cta-i">AI</div>
                  <div className="pl-ai-cta-t">
                    <b>Настроить ИИ-ключ</b>
                    <span>Общий ключ на плеер и читалку</span>
                  </div>
                  <span className="pl-ai-cta-arr">›</span>
                </div>
              )}
              {aiReady && !isDownloaded && (
                <div style={{ fontSize: 10, color: 'var(--warn)', textAlign: 'center', marginTop: 6, lineHeight: 1.4 }}>
                  Скачайте серию, чтобы использовать ИИ-озвучку
                </div>
              )}
            </div>
          )}

          {sheet === 'quality' && (
            <div className="pl-sub-list">
              {qualities.map(q => (
                <div key={q} className={"pl-sub-item" + (quality === q ? ' on' : '')}
                     onClick={() => setQuality(q)}>
                  <span className="dot"></span>
                  <span className="t">{q}</span>
                </div>
              ))}
            </div>
          )}

          {sheet === 'speed' && (
            <div className="pl-sub-list">
              {speeds.map(s => (
                <div key={s} className={"pl-sub-item" + (speed === s ? ' on' : '')}
                     onClick={() => setSpeed(s)}>
                  <span className="dot"></span>
                  <span className="t">×{s} {s === 1 ? '· обычная' : ''}</span>
                </div>
              ))}
            </div>
          )}

          {sheet === 'settings' && (
            <div className="pl-sub-list">
              <div className="rd-tog" onClick={() => setKeepAwake(!keepAwake)} style={{ padding: '6px 4px' }}>
                <div><b>Держать экран включённым</b></div>
                <div className={"toggle" + (keepAwake ? ' on' : '')}></div>
              </div>
              <div className="rd-tog" style={{ padding: '6px 4px' }}>
                <div><b>Автопрощай опенинг/эндинг</b><span>Найдено по звуковой сигнатуре</span></div>
                <div className="toggle on"></div>
              </div>
              <div className="rd-tog" style={{ padding: '6px 4px' }}>
                <div><b>Автопереход к след. серии</b></div>
                <div className="toggle on"></div>
              </div>
              <div className="rd-tog" style={{ padding: '6px 4px' }}>
                <div><b>Ускоренный жест</b><span>Удержание — 2×</span></div>
                <div className="toggle"></div>
              </div>
              <div style={{ fontSize: 10, color: 'var(--on-surface-variant)', marginTop: 8, lineHeight: 1.4 }}>
                Всё, что не в плеере, — не настраивается. Так проще держать в одной руке.
              </div>
            </div>
          )}
        </div>
      )}
    </>
  );
}

window.Player = Player;
