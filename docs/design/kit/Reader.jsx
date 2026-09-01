// Reader.jsx v2 — свиток по умолчанию + встроенные настройки чтения
// Настройки открываются шестерёнкой или отдельным FAB, все контролы — в самой читалке.
// ИИ-функции (перевод, объяснение) используют общий ключ из AiStore.
function Reader({ onBack, onGoAiSettings }) {
  const ai = useAiStore();
  const aiReady = window.AiStore.isReady();
  const [uiOpen, setUiOpen] = React.useState(true);
  const [settingsOpen, setSettingsOpen] = React.useState(false);
  const [page, setPage] = React.useState(3);
  const total = 18;

  // Все настройки — локально в ридере
  const [cfg, setCfg] = React.useState({
    mode: 'webtoon',      // webtoon | ltr | rtl | double
    direction: 'ltr',
    brightness: 85,
    autoScroll: false,
    autoScrollSpeed: 3,
    crop: true,
    tint: 'off',          // off | sepia | dark
    fontSize: 16,
    lineHeight: 1.6,
    columnWidth: 'medium',
    keepAwake: true,
    volumeKeys: false,
    tapZones: true,
  });
  const set = (k, v) => setCfg(c => ({ ...c, [k]: v }));

  // Полосы «страниц» — просто как в v1, но крупнее
  const heights = [180, 260, 210, 280, 200, 250, 230, 190, 270, 200, 260, 220];

  const tintBg = {
    off: '#000',
    sepia: '#2A1F14',
    dark: '#0A0A0A'
  }[cfg.tint];

  return (
    <>
      <div className="reader-tap" onClick={() => setUiOpen(!uiOpen)}></div>

      <div className="reader-pages"
           style={{
             background: tintBg,
             filter: `brightness(${cfg.brightness / 100})`
           }}>
        {heights.map((h, i) => (
          <div key={i} className="pg" style={{ height: h + 'px' }}>{i + 1}</div>
        ))}
      </div>

      {/* Top bar */}
      {uiOpen && !settingsOpen && (
        <div className="reader-top">
          <div className="row">
            <button className="ic-btn" onClick={(e) => { e.stopPropagation(); onBack && onBack(); }}>
              <Icon name="back" />
            </button>
            <div className="info">
              <b>Глава 42</b>
              <span>Тайтл с длинным названием</span>
            </div>
            <button className="ic-btn"
                    onClick={(e) => { e.stopPropagation(); setSettingsOpen(true); }}>
              <Icon name="settings" />
            </button>
          </div>
        </div>
      )}

      {/* Bottom bar */}
      {uiOpen && !settingsOpen && (
        <div className="reader-bot">
          <input
            type="range" className="reader-slider" min="1" max={total} value={page}
            onChange={(e) => setPage(+e.target.value)}
            onClick={(e) => e.stopPropagation()}
          />
          <div className="reader-nav" onClick={(e) => e.stopPropagation()}>
            <button className="mini-chip"
                    onClick={() => setPage(p => Math.max(1, p - 1))}>
              <Icon name="arrowL" className="xs" /> Пред.
            </button>
            <span style={{ fontSize: 11, color: 'var(--on-surface-variant)', alignSelf: 'center' }}>
              {page} / {total}
            </span>
            <button className="mini-chip"
                    onClick={() => setPage(p => Math.min(total, p + 1))}>
              След. <Icon name="arrowR" className="xs" />
            </button>
          </div>
        </div>
      )}

      {/* Persistent auto-scroll FAB (visible when webtoon + UI hidden) */}
      {!settingsOpen && cfg.mode === 'webtoon' && (
        <button className={"rd-fab" + (cfg.autoScroll ? ' on' : '')}
                onClick={(e) => { e.stopPropagation(); set('autoScroll', !cfg.autoScroll); }}
                aria-label="Автоскролл"
                title="Автопрокрутка">
          {cfg.autoScroll ? '⏸' : '▷'}
        </button>
      )}

      {/* Settings sheet — внутри читалки */}
      {settingsOpen && (
        <div className="rd-sheet" onClick={(e) => e.stopPropagation()}>
          <div className="rd-sheet-head">
            <h4>Настройки чтения</h4>
            <button onClick={() => setSettingsOpen(false)}>✕</button>
          </div>

          {/* ИИ-подсказка: общий ключ из AiStore */}
          <div className={"rd-ai-hint" + (aiReady ? '' : ' warn')}>
            <div className="rd-ai-hint-i">AI</div>
            <div className="rd-ai-hint-t">
              <b>{aiReady ? 'ИИ-перевод включён' : 'Перевод и объяснение — через ИИ'}</b>
              <span>
                {aiReady
                  ? `${window.AiStore.providers[ai.provider].n} · ${ai.model}. Долгий тап по пузырю — перевести.`
                  : 'Добавьте общий API-ключ в настройках. Он работает и в плеере (ИИ-озвучка).'}
              </span>
            </div>
            <button onClick={() => onGoAiSettings && onGoAiSettings()}>
              {aiReady ? 'Изменить' : 'Настроить'}
            </button>
          </div>

          <div className="rd-row">
            <div className="rd-row-lbl"><b>Режим</b></div>
            <div className="rd-seg">
              {[['webtoon','Свиток'],['ltr','Стр. →'],['rtl','Стр. ←'],['double','Разворот']].map(([v, l]) => (
                <button key={v} className={cfg.mode === v ? 'on' : ''}
                        onClick={() => set('mode', v)}>{l}</button>
              ))}
            </div>
          </div>

          <div className="rd-row">
            <div className="rd-row-lbl"><b>Яркость</b><em>{cfg.brightness}%</em></div>
            <input type="range" className="rd-slider" min="20" max="100" value={cfg.brightness}
                   onChange={e => set('brightness', +e.target.value)} />
          </div>

          {cfg.mode === 'webtoon' && (
            <div className="rd-row">
              <div className="rd-row-lbl"><b>Автоскролл</b><em>{cfg.autoScroll ? 'вкл' : 'выкл'}</em></div>
              <div className="rd-seg">
                {[1, 2, 3, 4, 5].map(v => (
                  <button key={v} className={cfg.autoScrollSpeed === v ? 'on' : ''}
                          onClick={() => { set('autoScrollSpeed', v); set('autoScroll', true); }}>
                    ×{v}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="rd-row">
            <div className="rd-row-lbl"><b>Тон страницы</b></div>
            <div className="rd-seg">
              {[['off','Обычный'],['sepia','Сепия'],['dark','Ночной']].map(([v, l]) => (
                <button key={v} className={cfg.tint === v ? 'on' : ''}
                        onClick={() => set('tint', v)}>{l}</button>
              ))}
            </div>
          </div>

          {/* Только для ранобэ — оставляем видимыми контролы, потому что кит показывает всё */}
          <div className="rd-row">
            <div className="rd-row-lbl"><b>Размер шрифта</b><em>{cfg.fontSize} px</em></div>
            <div className="rd-seg">
              {[13, 15, 17, 19, 22].map(v => (
                <button key={v} className={cfg.fontSize === v ? 'on' : ''}
                        onClick={() => set('fontSize', v)}>{v}</button>
              ))}
            </div>
          </div>

          <div className="rd-row">
            <div className="rd-row-lbl"><b>Ширина колонки</b></div>
            <div className="rd-seg">
              {[['narrow','Узкая'],['medium','Средняя'],['wide','Широкая']].map(([v, l]) => (
                <button key={v} className={cfg.columnWidth === v ? 'on' : ''}
                        onClick={() => set('columnWidth', v)}>{l}</button>
              ))}
            </div>
          </div>

          <div className="rd-row">
            <div className="rd-tog" onClick={() => set('crop', !cfg.crop)}>
              <div><b>Обрезать поля</b>
                <span>Автообрезка белых краёв</span>
              </div>
              <div className={"toggle" + (cfg.crop ? ' on' : '')}></div>
            </div>
            <div className="rd-tog" onClick={() => set('keepAwake', !cfg.keepAwake)}>
              <div><b>Держать экран включённым</b></div>
              <div className={"toggle" + (cfg.keepAwake ? ' on' : '')}></div>
            </div>
            <div className="rd-tog" onClick={() => set('volumeKeys', !cfg.volumeKeys)}>
              <div><b>Листать кнопками громкости</b></div>
              <div className={"toggle" + (cfg.volumeKeys ? ' on' : '')}></div>
            </div>
            <div className="rd-tog" onClick={() => set('tapZones', !cfg.tapZones)}>
              <div><b>Зоны тапа для листания</b></div>
              <div className={"toggle" + (cfg.tapZones ? ' on' : '')}></div>
            </div>
          </div>
        </div>
      )}

      <div className="reader-pill">{page} / {total}</div>
    </>
  );
}

window.Reader = Reader;
