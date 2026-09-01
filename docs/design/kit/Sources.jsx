function Sources({ onBack }) {
  const [tab, setTab] = React.useState('installed');
  const [enabled, setEnabled] = React.useState(
    DB.sources.reduce((acc, s) => ({ ...acc, [s[0]]: !!s[3] }), {})
  );
  const [toast, setToast] = React.useState(null);
  const [addOpen, setAddOpen] = React.useState(false);
  const [url, setUrl] = React.useState('');
  const t = (m) => { setToast(m); clearTimeout(t._h); t._h = setTimeout(() => setToast(null), 1600); };

  return (
    <div className="screen-scroll">
      <AppBar
        back={onBack} title="Источники"
        actions={[{ icon: 'plus', onClick: () => setAddOpen(true) }]}
      />
      <div className="filter-tabs">
        <button className={"chip" + (tab === 'installed' ? ' on' : '')}
                onClick={() => setTab('installed')}>Установленные</button>
        <button className={"chip" + (tab === 'catalog' ? ' on' : '')}
                onClick={() => setTab('catalog')}>Каталог</button>
        <button className={"chip" + (tab === 'updates' ? ' on' : '')}
                onClick={() => setTab('updates')}>Обновления · 2</button>
      </div>

      {addOpen && (
        <div className="pv-form" style={{ margin: '4px 16px 12px' }}>
          <div className="pv-form-head">
            <span>Добавить источник</span>
            <span className="close" onClick={() => setAddOpen(false)}>✕</span>
          </div>
          <div className="pv-field">
            <label>URL репозитория расширений</label>
            <input placeholder="https://raw.githubusercontent.com/…/index.min.json"
                   value={url} onChange={e => setUrl(e.target.value)} />
          </div>
          <div className="pv-form-actions">
            <button className="btn ghost" onClick={() => setAddOpen(false)}>Отмена</button>
            <button className="btn primary"
                    onClick={() => { setAddOpen(false); setUrl(''); t('Репозиторий подключён'); }}>
              Подключить
            </button>
          </div>
        </div>
      )}

      <div className="src-list">
        {DB.sources.map(([name, type, ver, active, status]) => {
          const isOn = enabled[name];
          return (
            <div key={name} className={"src-item" + (!active ? " off" : "")}
                 onClick={() => t(name + ' — карточка расширения')}>
              <div className="ic">{name[0]}</div>
              <div className="name">
                <b>{name}</b>
                <span>{type} · v{ver}{!active ? ' · парсер сломан' : ''}</span>
              </div>
              <div className={"dot2" + (status === 'err' ? ' bad' : status === 'warn' ? ' warn' : '')}></div>
              <div className={"toggle" + (isOn ? ' on' : '')}
                   onClick={(e) => { e.stopPropagation(); setEnabled({ ...enabled, [name]: !isOn }); t(name + (!isOn ? ' включён' : ' выключен')); }}></div>
            </div>
          );
        })}
      </div>

      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Sources = Sources;
