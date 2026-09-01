// AdvancedSearch.jsx — sheet-панель расширенного поиска
// Отдаёт кол-во активных фильтров через onApply({count, ...})
function AdvancedSearch({ onClose, onApply, initial }) {
  const [state, setState] = React.useState(() => initial || {
    types: [],           // manga / anime / novel
    genres: {},          // { "Экшен": 'inc' | 'exc' }
    seiyuu: [],          // ids
    studios: [],
    status: [],          // ongoing / finished / hiatus
    ageRating: [],
    yearFrom: 2000,
    yearTo: 2026,
    rating: 0,           // min rating * 10
  });

  const toggleGenre = (g) => setState(s => {
    const cur = s.genres[g];
    const next = { ...s.genres };
    if (cur === 'inc') next[g] = 'exc';
    else if (cur === 'exc') delete next[g];
    else next[g] = 'inc';
    return { ...s, genres: next };
  });

  const toggleArr = (key, v) => setState(s => {
    const arr = s[key] || [];
    return { ...s, [key]: arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v] };
  });

  // Count active
  const count =
    state.types.length +
    Object.keys(state.genres).length +
    state.seiyuu.length +
    state.studios.length +
    state.status.length +
    state.ageRating.length +
    (state.yearFrom !== 2000 || state.yearTo !== 2026 ? 1 : 0) +
    (state.rating > 0 ? 1 : 0);

  const reset = () => setState({
    types: [], genres: {}, seiyuu: [], studios: [], status: [], ageRating: [],
    yearFrom: 2000, yearTo: 2026, rating: 0
  });

  return (
    <>
      <div className="scrim" onClick={onClose}></div>
      <div className="sheet">
        <div className="sheet-grip"></div>
        <div className="sheet-head">
          <h3>Расширенный поиск</h3>
          <button className="close" onClick={onClose}>✕</button>
        </div>

        <div className="sheet-body stg">
          {/* Тип */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Тип</span>
              {state.types.length > 0 && <em>{state.types.length}</em>}
            </div>
            <div className="fs-chips">
              {[['manga','Манга'],['anime','Аниме'],['novel','Ранобэ']].map(([k, l]) => (
                <button key={k}
                        className={"fs-chip" + (state.types.includes(k) ? ' on' : '')}
                        onClick={() => toggleArr('types', k)}>{l}</button>
              ))}
            </div>
          </div>

          {/* Жанр — trip-state (нейтр / вкл / искл) */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Жанр <em style={{ marginLeft: 6, color: 'var(--on-surface-variant)' }}>клик — вкл, ещё раз — искл.</em></span>
              {Object.keys(state.genres).length > 0 && <em>{Object.keys(state.genres).length}</em>}
            </div>
            <div className="fs-chips">
              {DB.genres.map(g => {
                const st = state.genres[g];
                return (
                  <button key={g}
                          className={"fs-chip" + (st === 'inc' ? ' on' : st === 'exc' ? ' exc' : '')}
                          onClick={() => toggleGenre(g)}>{g}</button>
                );
              })}
            </div>
          </div>

          {/* Год */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Год выпуска</span>
              <em>{state.yearFrom} — {state.yearTo}</em>
            </div>
            <div className="fs-year">
              <div className="fs-year-val">{state.yearFrom}</div>
              <input type="range" min="1980" max="2026" value={state.yearFrom}
                     onChange={e => setState(s => ({ ...s, yearFrom: Math.min(+e.target.value, s.yearTo) }))} />
            </div>
            <div className="fs-year">
              <div className="fs-year-val">{state.yearTo}</div>
              <input type="range" min="1980" max="2026" value={state.yearTo}
                     onChange={e => setState(s => ({ ...s, yearTo: Math.max(+e.target.value, s.yearFrom) }))} />
            </div>
          </div>

          {/* Сэйю */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Сэйю</span>
              {state.seiyuu.length > 0 && <em>{state.seiyuu.length}</em>}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {DB.seiyuu.map(s => {
                const on = state.seiyuu.includes(s.id);
                return (
                  <div key={s.id} className="fs-seiyuu-row" onClick={() => toggleArr('seiyuu', s.id)}>
                    <div className="av">{s.n.charAt(0)}</div>
                    <div className="t">
                      <b>{s.n}</b>
                      <span>{s.jp} · {s.roles} ролей</span>
                    </div>
                    <div className={"fs-check" + (on ? ' on' : '')}>✓</div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Студия */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Студия</span>
              {state.studios.length > 0 && <em>{state.studios.length}</em>}
            </div>
            <div className="fs-chips">
              {DB.studios.map(s => (
                <button key={s}
                        className={"fs-chip" + (state.studios.includes(s) ? ' on' : '')}
                        onClick={() => toggleArr('studios', s)}>{s}</button>
              ))}
            </div>
          </div>

          {/* Статус */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Статус выпуска</span>
              {state.status.length > 0 && <em>{state.status.length}</em>}
            </div>
            <div className="fs-chips">
              {[['ongoing','Онгоинг'],['finished','Завершён'],['hiatus','На паузе'],['announced','Анонс']].map(([k, l]) => (
                <button key={k}
                        className={"fs-chip" + (state.status.includes(k) ? ' on' : '')}
                        onClick={() => toggleArr('status', k)}>{l}</button>
              ))}
            </div>
          </div>

          {/* Рейтинг */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Минимальный рейтинг</span>
              <em>{state.rating > 0 ? '★ ' + (state.rating / 10).toFixed(1) : 'любой'}</em>
            </div>
            <div className="fs-year">
              <div className="fs-year-val">
                {state.rating > 0 ? (state.rating / 10).toFixed(1) : '—'}
              </div>
              <input type="range" min="0" max="100" step="5" value={state.rating}
                     onChange={e => setState(s => ({ ...s, rating: +e.target.value }))} />
            </div>
          </div>

          {/* Возрастной рейтинг */}
          <div className="fs-grp">
            <div className="fs-grp-head">
              <span>Возрастной рейтинг</span>
              {state.ageRating.length > 0 && <em>{state.ageRating.length}</em>}
            </div>
            <div className="fs-chips">
              {DB.ageRatings.map(r => (
                <button key={r}
                        className={"fs-chip" + (state.ageRating.includes(r) ? ' on' : '')}
                        onClick={() => toggleArr('ageRating', r)}>{r}</button>
              ))}
            </div>
          </div>
        </div>

        <div className="sheet-foot">
          <button className="btn ghost" onClick={reset}>Сбросить</button>
          <button className="btn primary" onClick={() => onApply && onApply({ ...state, count })}>
            Показать · {count > 0 ? count + ' фильтр' + (count > 4 ? 'ов' : count > 1 ? 'а' : '') : 'без фильтров'}
          </button>
        </div>
      </div>
    </>
  );
}

window.AdvancedSearch = AdvancedSearch;
