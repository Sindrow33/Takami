// Settings.jsx v2 — расширенные настройки по мотивам Tadami.
// Ридер/плеер настройки живут ВНУТРИ ридера/плеера, здесь их нет.
function Settings({ onBack, onGoSources, onGoDonate, onGoProxy, focusAi }) {
  const ai = useAiStore();
  const [aiKey, setAiKey] = React.useState(ai.apiKey || '');
  const [aiProvider, setAiProvider] = React.useState(ai.provider || 'openai');
  const [aiModel, setAiModel] = React.useState(ai.model || 'gpt-4o-mini');
  const [aiEndpoint, setAiEndpoint] = React.useState(ai.endpoint || '');
  const [aiShow, setAiShow] = React.useState(false);
  const [aiTesting, setAiTesting] = React.useState(false);
  const [aiTestResult, setAiTestResult] = React.useState(null); // null | 'ok' | 'fail'
  const aiRef = React.useRef(null);
  const providers = window.AiStore.providers;
  const currentProv = providers[aiProvider];
  const isDirty = aiKey !== (ai.apiKey || '') || aiProvider !== (ai.provider || 'openai') ||
                  aiModel !== (ai.model || 'gpt-4o-mini') || aiEndpoint !== (ai.endpoint || '');

  // Прокрутить к AI-секции, если открыли из ридера/плеера
  React.useEffect(() => {
    if (focusAi && aiRef.current) {
      setTimeout(() => {
        const scr = aiRef.current.closest('.st-scroll');
        if (scr) {
          const y = aiRef.current.offsetTop - 80;
          scr.scrollTo({ top: y, behavior: 'smooth' });
        }
      }, 120);
    }
  }, [focusAi]);

  const [cfg, setCfg] = React.useState({
    // Внешний вид
    theme: 'dark',
    accent: 'violet',
    reducedMotion: false,
    // Библиотека
    libLayout: 'grid',
    coverBadges: true,
    showUnread: true,
    sortBy: 'updated',
    // Источники
    autoSource: true,
    preferLang: 'ru',
    wifiOnly: true,
    // Загрузки
    dlLimit: '3',
    dlAuto: true,
    dlLocation: 'internal',
    // Уведомления
    notifyRelease: true,
    groupNotify: true,
    dndNight: true,
    // Приватность / безопасность
    history: true,
    spoilers: true,
    blurNsfw: false,
    pin: false,
    biometric: false,
    // Трекеры
    trackMal: false,
    trackAnilist: true,
    trackShiki: false,
    // Продвинутые
    cacheLimit: 512,
    dohProvider: 'cloudflare',
    verboseLogs: false,
  });
  const set = (k, v) => setCfg(c => ({ ...c, [k]: v }));
  const [toast, setToast] = React.useState(null);
  React.useEffect(() => {
    if (!toast) return;
    const h = setTimeout(() => setToast(null), 1500);
    return () => clearTimeout(h);
  }, [toast]);

  return (
    <div className="screen-scroll st-scroll stg">
      <AppBar back={onBack} title="Настройки" actions={[
        { icon: 'search', onClick: () => setToast('Введите название настройки для быстрого перехода') }
      ]} />

      {/* Профиль */}
      <div className="st-profile">
        <div className="st-avatar">
          <img src="assets/logo.jpg" alt="" />
        </div>
        <div className="st-profile-t">
          <b>Читатель</b>
          <span>Гость · вход не выполнен</span>
        </div>
        <button className="st-profile-btn" onClick={() => setToast('Открываем экран входа')}>Войти</button>
      </div>

      {/* --- Внешний вид --- */}
      <SectionHead title="Внешний вид" />
      <div className="st-g">
        <SegRow label="Тема"
                value={cfg.theme}
                options={[['dark','Тёмная'],['light','Светлая'],['auto','Система']]}
                onChange={v => set('theme', v)} />
        <SegRow label="Цвет акцента"
                value={cfg.accent}
                options={[['violet','Aurora'],['cyan','Cyan'],['amber','Amber']]}
                onChange={v => set('accent', v)} />
        <SwitchRow label="Уменьшенная анимация"
                   sub="Оставит только базовые переходы"
                   value={cfg.reducedMotion} onChange={v => set('reducedMotion', v)} />
      </div>

      {/* --- Библиотека --- */}
      <SectionHead title="Библиотека" />
      <div className="st-g">
        <SegRow label="Вид"
                value={cfg.libLayout}
                options={[['grid','Сетка'],['compact','Комп.'],['list','Список']]}
                onChange={v => set('libLayout', v)} />
        <SegRow label="Сортировка"
                value={cfg.sortBy}
                options={[['updated','Обновл.'],['added','Добавл.'],['title','Назв.']]}
                onChange={v => set('sortBy', v)} />
        <SwitchRow label="Бейджи на обложках"
                   sub="Число новых глав, отметки NEW, err и off"
                   value={cfg.coverBadges} onChange={v => set('coverBadges', v)} />
        <SwitchRow label="Показывать непрочитанные"
                   value={cfg.showUnread} onChange={v => set('showUnread', v)} />
        <ActionRow label="Категории" right="4 категории" onClick={() => setToast('Категории: Читаю · Планы · Заброшено · Любимые')} />
      </div>

      {/* --- Источники --- */}
      <SectionHead title="Источники" />
      <div className="st-g">
        <SwitchRow label="Автовыбор источника"
                   sub="Берётся живой источник с наибольшим номером главы"
                   value={cfg.autoSource} onChange={v => set('autoSource', v)} />
        <SegRow label="Приоритет языка"
                value={cfg.preferLang}
                options={[['ru','Русский'],['en','English'],['any','Любой']]}
                onChange={v => set('preferLang', v)} />
        <ActionRow label="Управление источниками"
                   right="6 установлено"
                   onClick={onGoSources} />
      </div>

      {/* --- Загрузки --- */}
      <SectionHead title="Загрузки" />
      <div className="st-g">
        <SwitchRow label="Только по Wi-Fi"
                   value={cfg.wifiOnly} onChange={v => set('wifiOnly', v)} />
        <SwitchRow label="Автозагрузка новых глав"
                   sub="Для тайтлов в библиотеке"
                   value={cfg.dlAuto} onChange={v => set('dlAuto', v)} />
        <SegRow label="Одновременных загрузок"
                value={cfg.dlLimit}
                options={[['1','1'],['3','3'],['5','5']]}
                onChange={v => set('dlLimit', v)} />
        <SegRow label="Куда сохранять"
                value={cfg.dlLocation}
                options={[['internal','Внутр.'],['sd','SD'],['ask','Спраш.']]}
                onChange={v => set('dlLocation', v)} />
        <ActionRow label="Управление загрузками" right="6 глав · 42 МБ" onClick={() => setToast('Открываем список активных загрузок')} />
      </div>

      {/* --- Отслеживание (Tadami-style tracker section) --- */}
      <SectionHead title="Отслеживание" />
      <div className="st-g">
        <TrackerRow label="MyAnimeList" sub="MAL · рейтинг, прогресс, планы"
                    connected={cfg.trackMal}
                    onChange={v => set('trackMal', v)} />
        <TrackerRow label="AniList" sub="Синхронизация двусторонняя"
                    connected={cfg.trackAnilist}
                    onChange={v => set('trackAnilist', v)} account="reader_42"
                    onOpen={() => {}} />
        <TrackerRow label="Shikimori" sub="Шикимори · русскоязычный трекер"
                    connected={cfg.trackShiki}
                    onChange={v => set('trackShiki', v)} />
      </div>

      {/* --- ИИ-ассистент — общий ключ на читалку/плеер/ранобэ --- */}
      <SectionHead title="ИИ-ассистент" />
      <div className="st-g" ref={aiRef}>
        <div className="ai-card">
          <div className="ai-head">
            <div className="ai-badge">AI</div>
            <div className="ai-head-t">
              <b>Общий ключ API</b>
              <span>Используется в ридере (перевод и объяснение), плеере (ИИ-озвучка) и в ранобэ. Ключ хранится только на устройстве.</span>
            </div>
            <span className={"ai-status " + (window.AiStore.isReady() ? 'ok' : 'off')}>
              {window.AiStore.isReady() ? 'готов' : 'нет ключа'}
            </span>
          </div>

          <div className="ai-prov-row">
            {Object.entries(providers).map(([k, p]) => (
              <button key={k} className={aiProvider === k ? 'on' : ''}
                      onClick={() => {
                        setAiProvider(k);
                        if (p.models.length) setAiModel(p.models[0]);
                        setAiTestResult(null);
                      }}>
                {p.n}
                <em>{p.tag}</em>
              </button>
            ))}
          </div>

          <div className="ai-input">
            <input type={aiShow ? 'text' : 'password'}
                   value={aiKey}
                   placeholder={currentProv.hint}
                   onChange={e => { setAiKey(e.target.value); setAiTestResult(null); }}
                   spellCheck={false}
                   autoCapitalize="off"
                   autoCorrect="off" />
            <button title={aiShow ? 'Скрыть' : 'Показать'}
                    onClick={() => setAiShow(!aiShow)}>
              <Icon name={aiShow ? 'eyeOff' : 'eye'} />
            </button>
            <button title="Вставить из буфера"
                    onClick={async () => {
                      try {
                        const txt = await navigator.clipboard.readText();
                        if (txt) { setAiKey(txt.trim()); setAiTestResult(null); }
                      } catch (e) {
                        setToast('Нет доступа к буферу');
                      }
                    }}>
              <Icon name="paste" />
            </button>
          </div>

          {aiProvider === 'custom' && (
            <div className="ai-input">
              <input type="text"
                     value={aiEndpoint}
                     placeholder="https://ваш-сервер.tld/v1"
                     onChange={e => { setAiEndpoint(e.target.value); setAiTestResult(null); }}
                     spellCheck={false}
                     autoCapitalize="off"
                     autoCorrect="off" />
            </div>
          )}

          {currentProv.models.length > 0 && (
            <div className="st-r st-r-col" style={{ padding: '2px 0', borderTop: 'none' }}>
              <div className="st-i" style={{ paddingBottom: 6 }}>
                <b style={{ fontSize: 12 }}>Модель</b>
              </div>
              <div className="st-seg">
                {currentProv.models.map(m => (
                  <button key={m} className={aiModel === m ? 'on' : ''}
                          onClick={() => setAiModel(m)}
                          style={{ fontSize: 10.5 }}>{m}</button>
                ))}
              </div>
            </div>
          )}

          <div className="ai-actions">
            <button className="ai-btn-test"
                    disabled={!aiKey.trim() || aiTesting}
                    onClick={() => {
                      setAiTesting(true);
                      setAiTestResult(null);
                      // Симуляция — прототип
                      setTimeout(() => {
                        setAiTesting(false);
                        const ok = aiKey.length > 10;
                        setAiTestResult(ok ? 'ok' : 'fail');
                        setToast(ok ? 'Ключ работает' : 'Ключ отклонён');
                      }, 900);
                    }}>
              {aiTesting ? 'Проверяем…' : aiTestResult === 'ok' ? '✓ Работает' : aiTestResult === 'fail' ? '✕ Ошибка' : 'Проверить'}
            </button>
            <button className="ai-btn-save"
                    disabled={!isDirty}
                    onClick={() => {
                      window.AiStore.set({ apiKey: aiKey.trim(), provider: aiProvider, model: aiModel, endpoint: aiEndpoint });
                      setToast('Сохранено · ключ доступен во всех модулях');
                      setAiTestResult(null);
                    }}>
              Сохранить
            </button>
            {ai.apiKey && (
              <button className="ai-btn-clear"
                      title="Удалить ключ"
                      onClick={() => {
                        window.AiStore.clear();
                        setAiKey(''); setAiProvider('openai'); setAiModel('gpt-4o-mini'); setAiEndpoint('');
                        setToast('Ключ удалён');
                      }}>✕</button>
            )}
          </div>

          <div className="ai-features">
            <b>Где используется ключ</b>
            <div className="ai-feat">
              <span className="ai-feat-i"><Icon name="book" /></span>
              <span>Ридер манги · перевод пузырей, объяснение сцены</span>
              <em>ридер</em>
            </div>
            <div className="ai-feat">
              <span className="ai-feat-i"><Icon name="bookOpen" /></span>
              <span>Ранобэ · саммари главы, глоссарий имён</span>
              <em>ридер</em>
            </div>
            <div className="ai-feat">
              <span className="ai-feat-i"><Icon name="play" /></span>
              <span>Плеер · нейросетевая озвучка скачанных эпизодов</span>
              <em>плеер</em>
            </div>
          </div>
        </div>
      </div>

      {/* --- Уведомления --- */}
      <SectionHead title="Уведомления" />
      <div className="st-g">
        <SwitchRow label="Оповещать о выходе"
                   sub="По расчётному расписанию из календаря"
                   value={cfg.notifyRelease} onChange={v => set('notifyRelease', v)} />
        <SwitchRow label="Группировать в одно уведомление"
                   value={cfg.groupNotify} onChange={v => set('groupNotify', v)} />
        <SwitchRow label="Не беспокоить ночью"
                   sub="С 23:00 до 08:00"
                   value={cfg.dndNight} onChange={v => set('dndNight', v)} />
      </div>

      {/* --- Приватность --- */}
      <SectionHead title="Приватность" />
      <div className="st-g">
        <SwitchRow label="Вести историю чтения"
                   value={cfg.history} onChange={v => set('history', v)} />
        <SwitchRow label="Скрывать спойлеры"
                   sub="Биографии персонажей и комментарии размываются до нажатия"
                   value={cfg.spoilers} onChange={v => set('spoilers', v)} />
        <SwitchRow label="Размывать обложки 18+"
                   value={cfg.blurNsfw} onChange={v => set('blurNsfw', v)} />
        <ActionRow label="Очистить историю" danger onClick={() => setToast('История чтения очищена')} />
      </div>

      {/* --- Безопасность --- */}
      <SectionHead title="Безопасность" />
      <div className="st-g">
        <SwitchRow label="PIN-код на вход"
                   sub={cfg.pin ? 'Установлен · четыре цифры' : 'Не установлен'}
                   value={cfg.pin} onChange={v => set('pin', v)} />
        <SwitchRow label="Биометрия"
                   sub="Отпечаток или Face Unlock вместо PIN"
                   value={cfg.biometric} onChange={v => set('biometric', v)} />
        <ActionRow label="Заблокировать сейчас" onClick={() => setToast('Приложение заблокировано · разблокируйте PIN-ом')} />
      </div>

      {/* --- Proxy / VPN --- новый экран */}
      <SectionHead title="Сеть" />
      <div className="st-g">
        <ActionRow label="Proxy / VPN"
                   sub="Aurora VPN · Amsterdam · WireGuard"
                   right="вкл"
                   onClick={onGoProxy} />
        <SegRow label="DoH DNS"
                value={cfg.dohProvider}
                options={[['off','Off'],['cloudflare','CF'],['google','Google']]}
                onChange={v => set('dohProvider', v)} />
      </div>

      {/* --- Хранилище --- */}
      <SectionHead title="Хранилище" />
      <div className="st-g">
        <ActionRow label="Кеш изображений" right="184 МБ" onClick={() => setToast('184 МБ · 1 247 файлов')} />
        <SegRow label="Лимит кеша"
                value={String(cfg.cacheLimit)}
                options={[['256','256'],['512','512'],['1024','1024']]}
                onChange={v => set('cacheLimit', +v)} />
        <ActionRow label="Очистить кеш" onClick={() => setToast('Кеш очищен · 184 МБ освобождено')} />
      </div>

      {/* --- Резервное копирование --- */}
      <SectionHead title="Резервное копирование" />
      <div className="st-g">
        <ActionRow label="Создать резервную копию"
                   sub="Прогресс, категории, источники, настройки"
                   onClick={() => setToast('Создаём резервную копию…')} />
        <ActionRow label="Восстановить из файла" onClick={() => setToast('Выберите .bak файл')} />
        <ActionRow label="Автобэкапы"
                   right="раз в неделю"
                   onClick={() => setToast('Автобэкапы каждое воскресенье в 03:00')} />
      </div>

      {/* --- Продвинутые --- */}
      <SectionHead title="Продвинутые" />
      <div className="st-g">
        <SwitchRow label="Подробные логи"
                   sub="Для отправки в поддержку"
                   value={cfg.verboseLogs} onChange={v => set('verboseLogs', v)} />
        <ActionRow label="Экспорт логов" onClick={() => setToast('Логи готовятся · takami-log.txt')} />
        <ActionRow label="Сбросить все настройки" danger onClick={() => setToast('Настройки сброшены к значениям по умолчанию')} />
      </div>

      {/* --- ПОДДЕРЖАТЬ РАЗРАБОТКУ --- перенесена в самый низ */}

      {/* --- О ПРИЛОЖЕНИИ --- */}
      <SectionHead title="О приложении" />
      <div className="st-about">
        <div className="st-about-head">
          <div className="st-about-logo">
            <img src="assets/logo.jpg" alt="Takami" />
          </div>
          <div>
            <b>Takami</b>
            <span>Клиент-читалка для манги, аниме и ранобэ</span>
          </div>
        </div>

        <div className="st-about-rows">
          <div className="st-about-row">
            <span>Версия</span>
            <b className="tnum">1.2.0 (build 68)</b>
          </div>
          <div className="st-about-row">
            <span>Платформа</span>
            <b>Android · Kotlin / Compose</b>
          </div>
          <div className="st-about-row">
            <span>Лицензия</span>
            <b>Apache 2.0</b>
          </div>
        </div>

        <div className="st-about-links">
          <a className="st-about-link"
             onClick={() => window.open('https://github.com/Sindrow33/Takami','_blank','noopener')}>
            <span className="st-al-i"><Icon name="github" /></span>
            <span>GitHub</span>
            <em>github.com/Sindrow33/Takami</em>
          </a>
          <a className="st-about-link" onClick={() => setToast('Открываем Telegram-канал')}>
            <span className="st-al-i"><Icon name="telegram" /></span>
            <span>Telegram-канал</span>
            <em>@takami_app</em>
          </a>
          <a className="st-about-link" onClick={() => setToast('Откроется форма обратной связи')}>
            <span className="st-al-i"><Icon name="edit" /></span>
            <span>Обратная связь</span>
            <em>Сообщить о проблеме</em>
          </a>
          <a className="st-about-link" onClick={() => setToast('Проверяем обновления… актуальная версия')}>
            <span className="st-al-i"><Icon name="refresh" /></span>
            <span>Проверить обновления</span>
            <em>Актуальная версия</em>
          </a>
        </div>

        <div className="st-about-credit">
          Идейный предок — Aniyomi / Mihon / Tadami. Палитра адаптирована из Tadami (Apache-2.0).
        </div>
      </div>

      {/* --- Поддержка проекта в самом низу настроек --- */}
      {(() => { const SB = window.SupportButton; return SB ? <SB /> : null; })()}

      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

// --- атомы ---
function SectionHead({ title }) {
  return <div className="st-sechead">{title}</div>;
}

function SwitchRow({ label, sub, value, onChange }) {
  return (
    <div className="st-r" onClick={() => onChange(!value)}>
      <div className="st-i">
        <b>{label}</b>
        {sub && <span>{sub}</span>}
      </div>
      <div className={"toggle" + (value ? ' on' : '')}></div>
    </div>
  );
}

function SegRow({ label, sub, value, options, onChange }) {
  return (
    <div className="st-r st-r-col">
      <div className="st-i">
        <b>{label}</b>
        {sub && <span>{sub}</span>}
      </div>
      <div className="st-seg">
        {options.map(([v, l]) => (
          <button key={v} className={value === v ? 'on' : ''}
                  onClick={() => onChange(v)}>{l}</button>
        ))}
      </div>
    </div>
  );
}

function ActionRow({ label, sub, right, danger, onClick }) {
  return (
    <div className={"st-r st-r-act" + (danger ? ' danger' : '')} onClick={onClick}>
      <div className="st-i">
        <b>{label}</b>
        {sub && <span>{sub}</span>}
      </div>
      {right && <span className="st-r-right">{right}</span>}
      <span className="st-r-chev">›</span>
    </div>
  );
}

function TrackerRow({ label, sub, account, connected, onChange, onOpen }) {
  return (
    <div className="st-r" onClick={() => onOpen ? onOpen() : onChange(!connected)}>
      <div className="st-i">
        <b>{label}</b>
        <span>{connected && account ? `Подключён · ${account}` : sub}</span>
      </div>
      {connected ? (
        <span style={{
          fontSize: 11, padding: '4px 10px', borderRadius: 999,
          background: 'rgba(61,214,140,.14)', color: 'var(--ok)',
          border: '1px solid rgba(61,214,140,.28)', fontWeight: 500
        }}>Подключён</span>
      ) : (
        <span style={{
          fontSize: 11, padding: '4px 10px', borderRadius: 999,
          background: 'rgba(124,92,255,.14)', color: 'var(--acc-2)',
          border: '1px solid rgba(124,92,255,.28)', fontWeight: 500
        }}>Подключить</span>
      )}
    </div>
  );
}

window.Settings = Settings;
