function Library({ onGo, onOpenTitle }) {
  const [filter, setFilter] = React.useState('all');
  const [toast, setToast] = React.useState(null);
  const tt = (m) => { setToast(m); clearTimeout(tt._h); tt._h = setTimeout(() => setToast(null), 1500); };
  const filterMap = { all: null, manga: 'manga', anime: 'anime', novel: 'novel' };

  // Группируем по франшизам, чтобы показать multi-format стек
  const franchises = Object.keys(DB.fr).map(fid => {
    const fr = DB.fr[fid];
    const items = fr.items.map(id => DB.items.find(x => x.id === id));
    const shown = filterMap[filter] ? items.filter(x => x.type === filterMap[filter]) : items;
    if (!shown.length) return null;
    const head = shown[0];
    const multi = items.length > 1;
    return { fid, fr, items, head, shown, multi };
  }).filter(Boolean);

  return (
    <div className="screen-scroll">
      <AppBar
        title="Библиотека"
        actions={[
          { icon: 'search', onClick: () => onGo && onGo('search') },
          { icon: 'menu',   onClick: () => tt('Сортировка · Вид · Обновить всё') }
        ]}
      />
      <div className="filter-tabs">
        {['all','manga','anime','novel'].map(k => (
          <button key={k} className={"chip" + (filter === k ? ' on' : '')}
                  onClick={() => setFilter(k)}>
            {k === 'all' ? 'Всё' : DB.typeName[k]}
          </button>
        ))}
      </div>
      <div className="grid">
        {franchises.map(({ fid, fr, items, head, multi }) => (
          <a key={fid} className={"card" + (multi ? ' multi' : '')}
             onClick={() => onOpenTitle ? onOpenTitle(head) : (onGo && onGo('reader'))}>
            <div className="cover" style={{ background: fr.bg }}>
              {multi && <span className="stack">{items.length} формата</span>}
              {head.badge === 'err' ? <span className="badge err">!</span>
                : head.badge === 'off' ? <span className="badge off">↓</span>
                : head.badge ? <span className="badge">{head.badge}</span> : null}
            </div>
            <div className="nm">{fr.t}</div>
            <div className="sub">{head.sub}</div>
            <div className="progress"><i style={{ width: head.prog + '%' }}></i></div>
            {multi && (
              <div className="fmts">
                {['manga','anime','novel'].map(k => {
                  const has = items.some(x => x.type === k);
                  const active = has && head.type === k;
                  return (
                    <span key={k} className={active ? 'on' : has ? '' : 'miss'}>
                      {DB.short[k]}
                    </span>
                  );
                })}
              </div>
            )}
          </a>
        ))}
      </div>
      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Library = Library;
