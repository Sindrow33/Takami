// Proxy.jsx — Aurora VPN / Proxy.
// Функциональная модель заимствована из Karing (github.com/KaringX/karing):
//   • Три режима: Global / Rule / Direct (mode-seg)
//   • Профили подписок с обновлением и тестом URL-скорости
//   • Правила маршрутизации: домены/IP/гео/приложения
//   • DNS-настройки: fake-ip / remote / direct
//   • Sniffing, TUN, IPv6, Kill-switch, Автоподключение
//   • Диагностика и очистка кэша
// Никаких реальных запросов — прототип: у каждой кнопки есть отклик (toast/state).
function Proxy({ onBack }) {
  const [on, setOn] = React.useState(true);
  const [mode, setMode] = React.useState('rule');           // rule | global | direct
  const [servers, setServers] = React.useState(DB.proxyServers);
  const [addOpen, setAddOpen] = React.useState(false);
  const [addMode, setAddMode] = React.useState('sub');      // sub | manual | qr
  const [testing, setTesting] = React.useState(null);
  const [testingAll, setTestingAll] = React.useState(false);
  const [tab, setTab] = React.useState('servers');          // servers | rules | dns | adv
  const [dnsMode, setDnsMode] = React.useState('fakeip');   // fakeip | remote | direct
  const [prefs, setPrefs] = React.useState({
    autoOn: true, killSwitch: false, tun: true, sniff: true, ipv6: false,
    lan: false, ads: true, wifi: false
  });
  const [rules, setRules] = React.useState([
    { id: 1, kind: 'domain', v: 'mangahub.io',   act: 'proxy', on: 1 },
    { id: 2, kind: 'domain', v: 'anilibria.tv',  act: 'proxy', on: 1 },
    { id: 3, kind: 'domain', v: '*.ru',          act: 'direct', on: 1 },
    { id: 4, kind: 'geoip',  v: 'RU',            act: 'direct', on: 1 },
    { id: 5, kind: 'domain', v: 'yandex.ru',     act: 'direct', on: 1 },
    { id: 6, kind: 'ip',     v: '10.0.0.0/8',    act: 'direct', on: 1 },
    { id: 7, kind: 'app',    v: 'Telegram',      act: 'proxy', on: 0 }
  ]);
  const [toast, setToast] = React.useState(null);
  const t = (m) => { setToast(m); clearTimeout(t._h); t._h = setTimeout(() => setToast(null), 1800); };
  const togglePref = (k) => setPrefs(p => ({ ...p, [k]: !p[k] }));

  const active = servers.find(s => s.active);

  const activate = (id) => {
    setServers(list => list.map(s => ({ ...s, active: s.id === id ? 1 : 0 })));
    const sv = servers.find(s => s.id === id);
    if (sv) t('Подключаемся к ' + sv.n);
  };

  const test = (id) => {
    setTesting(id);
    setTimeout(() => {
      setServers(list => list.map(s => s.id === id
        ? { ...s, ping: 30 + Math.floor(Math.random() * 130) } : s));
      setTesting(null);
    }, 1400);
  };

  const testAll = () => {
    setTestingAll(true);
    setTimeout(() => {
      setServers(list => list.map(s => ({ ...s, ping: 30 + Math.floor(Math.random() * 130) })));
      setTestingAll(false);
      t('Скорость обновлена для ' + servers.length + ' серверов');
    }, 1600);
  };

  const [form, setForm] = React.useState({
    type: 'WireGuard', host: '', port: '', user: '', pass: '', key: '', label: '', url: ''
  });
  const submitForm = () => {
    if (addMode === 'sub') {
      if (!form.url) return t('Введите URL подписки');
      t('Загружаем подписку…');
      setTimeout(() => {
        const nextId = Math.max(0, ...servers.map(s => s.id)) + 1;
        setServers([
          ...servers,
          { id: nextId,     n: 'Tokyo · JP · sub', type: 'VMess', host: 'jp.karing.sub', ping: 78, active: 0, kind: 'sub' },
          { id: nextId + 1, n: 'Osaka · JP · sub', type: 'VLESS', host: 'osaka.karing.sub', ping: 88, active: 0, kind: 'sub' },
          { id: nextId + 2, n: 'Seoul · KR · sub', type: 'Trojan', host: 'kr.karing.sub', ping: 65, active: 0, kind: 'sub' }
        ]);
        t('Подписка добавлена · +3 сервера');
      }, 1200);
      setAddOpen(false);
      return;
    }
    if (addMode === 'qr') {
      t('QR-камера: наведите на код подписки');
      setAddOpen(false);
      return;
    }
    if (!form.host) return t('Введите хост');
    const nextId = Math.max(0, ...servers.map(s => s.id)) + 1;
    setServers([...servers, {
      id: nextId, n: form.label || form.host, type: form.type,
      host: form.port ? form.host + ':' + form.port : form.host,
      ping: 30 + Math.floor(Math.random() * 90),
      active: 0, kind: 'manual'
    }]);
    setForm({ type: 'WireGuard', host: '', port: '', user: '', pass: '', key: '', label: '', url: '' });
    setAddOpen(false);
    t('Сервер добавлен');
  };

  const deleteServer = (id, e) => {
    e && e.stopPropagation();
    setServers(list => list.filter(s => s.id !== id));
    t('Сервер удалён');
  };

  const pingCls = (p) => p < 60 ? '' : p < 100 ? 'mid' : 'slow';
  const pinned = servers.filter(s => s.kind === 'pinned');
  const subServ = servers.filter(s => s.kind === 'sub');
  const manual  = servers.filter(s => s.kind === 'manual');

  const modeInfo = {
    global: { t: 'Global',  s: 'Весь трафик через VPN' },
    rule:   { t: 'Rule',    s: 'По правилам — умный сплит' },
    direct: { t: 'Direct',  s: 'Всё напрямую, VPN выключен' }
  };

  const rulesActive = rules.filter(r => r.on).length;
  const toggleRule = (id) => setRules(r => r.map(x => x.id === id ? { ...x, on: x.on ? 0 : 1 } : x));
  const deleteRule = (id) => setRules(r => r.filter(x => x.id !== id));
  const [ruleForm, setRuleForm] = React.useState({ kind: 'domain', v: '', act: 'proxy' });
  const [ruleAddOpen, setRuleAddOpen] = React.useState(false);
  const addRule = () => {
    if (!ruleForm.v) return t('Введите значение');
    const nextId = Math.max(0, ...rules.map(r => r.id)) + 1;
    setRules([...rules, { id: nextId, ...ruleForm, on: 1 }]);
    setRuleForm({ kind: 'domain', v: '', act: 'proxy' });
    setRuleAddOpen(false);
    t('Правило добавлено');
  };
  const kindLabel = { domain: 'домен', ip: 'IP / CIDR', geoip: 'GeoIP', app: 'приложение' };
  const actLabel  = { proxy: 'через прокси', direct: 'напрямую', block: 'заблокировать' };

  return (
    <div className="screen-scroll pv-scroll stg">
      <AppBar back={onBack} title="Proxy / VPN"
              actions={[
                { icon: 'refresh', onClick: () => { testAll(); } },
                { icon: 'menu', onClick: () => t('Импорт · Экспорт конфигов · Статистика') }
              ]} />

      {/* Status card */}
      <div className={"pv-status" + (on ? '' : ' off')}>
        <div className="pv-status-row">
          <div className="pv-status-icon">
            {on ? '⛨' : '✕'}
          </div>
          <div className="pv-status-t">
            <div className="k">{on ? (mode === 'direct' ? 'Direct' : 'Подключено') : 'Отключено'}</div>
            <b>{on ? (active ? active.n : 'Сервер не выбран') : 'Прямое соединение'}</b>
            <span>
              {on
                ? (active ? active.type + ' · ' + active.host : 'Выберите сервер ниже')
                : 'Трафик идёт напрямую'}
            </span>
          </div>
          <div className={"pv-switch" + (on ? ' on' : '')}
               onClick={() => { setOn(!on); t(!on ? 'VPN включён' : 'VPN выключен'); }}
               role="switch" aria-checked={on}></div>
        </div>
        {on && active && (
          <div className="pv-status-metrics">
            <div className="pv-metric pg">
              <div className="k">Пинг</div>
              <div className="v tnum">{active.ping} мс</div>
            </div>
            <div className="pv-metric up">
              <div className="k">↑ Upload</div>
              <div className="v tnum">4.2 Mb/s</div>
            </div>
            <div className="pv-metric dn">
              <div className="k">↓ Download</div>
              <div className="v tnum">28 Mb/s</div>
            </div>
          </div>
        )}
      </div>

      {/* Mode seg — Global / Rule / Direct */}
      <div className="pv-mode">
        {['global','rule','direct'].map(k => (
          <button key={k}
                  className={"pv-mode-b" + (mode === k ? ' on' : '')}
                  onClick={() => { setMode(k); t('Режим: ' + modeInfo[k].t); }}>
            <b>{modeInfo[k].t}</b>
            <span>{modeInfo[k].s}</span>
          </button>
        ))}
      </div>

      {/* Tabs */}
      <div className="pv-tabs">
        {[
          { k: 'servers', l: 'Серверы',  n: servers.length },
          { k: 'rules',   l: 'Правила',  n: rulesActive },
          { k: 'dns',     l: 'DNS' },
          { k: 'adv',     l: 'Ещё' }
        ].map(x => (
          <button key={x.k}
                  className={"pv-tab" + (tab === x.k ? ' on' : '')}
                  onClick={() => setTab(x.k)}>
            {x.l}
            {x.n != null && <em>{x.n}</em>}
          </button>
        ))}
      </div>

      {/* SERVERS TAB */}
      {tab === 'servers' && (
        <>
          {/* Add server actions */}
          {!addOpen && (
            <div className="pv-add-actions">
              <button className="pv-add-btn" onClick={() => { setAddMode('sub'); setAddOpen(true); }}>
                <span className="ic"><Icon name="download" /></span> Импорт подписки
              </button>
              <button className="pv-add-btn" onClick={() => { setAddMode('qr'); setAddOpen(true); }}>
                <span className="ic"><Icon name="book" /></span> Сканировать QR
              </button>
              <button className="pv-add-btn" onClick={() => { setAddMode('manual'); setAddOpen(true); }}>
                <span className="ic"><Icon name="plus" /></span> Вручную
              </button>
              <button className="pv-add-btn" onClick={testAll} disabled={testingAll}>
                <span className="ic"><Icon name="refresh" /></span> {testingAll ? 'Тестируем…' : 'Тест всех'}
              </button>
            </div>
          )}

          {/* Add form */}
          {addOpen && (
            <div className="pv-form">
              <div className="pv-form-head">
                <span>{addMode === 'sub' ? 'Импорт подписки' : addMode === 'qr' ? 'QR-код' : 'Новый сервер'}</span>
                <span className="close" onClick={() => setAddOpen(false)}>✕</span>
              </div>
              {/* Sub-mode switcher */}
              <div className="pv-add-seg">
                {['sub','qr','manual'].map(k => (
                  <button key={k}
                          className={"pv-add-seg-b" + (addMode === k ? ' on' : '')}
                          onClick={() => setAddMode(k)}>
                    {k === 'sub' ? 'Подписка' : k === 'qr' ? 'QR' : 'Вручную'}
                  </button>
                ))}
              </div>

              {addMode === 'sub' && (
                <>
                  <div className="pv-field">
                    <label>Ссылка на подписку</label>
                    <input placeholder="https://sub.example.com/link"
                           value={form.url}
                           onChange={e => setForm(f => ({ ...f, url: e.target.value }))} />
                  </div>
                  <div className="pv-field">
                    <label>Формат</label>
                    <select>
                      <option>Автоопределение</option>
                      <option>Clash</option>
                      <option>Sing-box</option>
                      <option>V2Ray (base64)</option>
                      <option>Shadowsocks</option>
                    </select>
                  </div>
                </>
              )}

              {addMode === 'qr' && (
                <div className="pv-qr">
                  <div className="pv-qr-frame">
                    <span></span><span></span><span></span><span></span>
                    <div className="pv-qr-scan"></div>
                    <b><Icon name="calendar" /></b>
                  </div>
                  <span className="pv-qr-hint">Наведите камеру на QR-код с конфигом</span>
                </div>
              )}

              {addMode === 'manual' && (
                <>
                  <div className="pv-field">
                    <label>Тип</label>
                    <select value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
                      <option>WireGuard</option>
                      <option>OpenVPN</option>
                      <option>SOCKS5</option>
                      <option>HTTP</option>
                      <option>HTTPS</option>
                      <option>Shadowsocks</option>
                      <option>VMess</option>
                      <option>VLESS</option>
                      <option>Trojan</option>
                      <option>Hysteria2</option>
                      <option>TUIC</option>
                    </select>
                  </div>
                  <div className="pv-field">
                    <label>Название</label>
                    <input placeholder="Home NL"
                           value={form.label}
                           onChange={e => setForm(f => ({ ...f, label: e.target.value }))} />
                  </div>
                  <div className="pv-field-row">
                    <div className="pv-field">
                      <label>Хост / IP</label>
                      <input placeholder="10.0.0.1"
                             value={form.host}
                             onChange={e => setForm(f => ({ ...f, host: e.target.value }))} />
                    </div>
                    <div className="pv-field">
                      <label>Порт</label>
                      <input placeholder="51820"
                             value={form.port}
                             onChange={e => setForm(f => ({ ...f, port: e.target.value }))} />
                    </div>
                  </div>
                  <div className="pv-field">
                    <label>Ключ / UUID / пароль</label>
                    <input placeholder="PRIVATE_KEY = ..."
                           value={form.key}
                           onChange={e => setForm(f => ({ ...f, key: e.target.value }))} />
                  </div>
                </>
              )}

              <div className="pv-form-actions">
                <button className="btn ghost" onClick={() => setAddOpen(false)}>Отмена</button>
                <button className="btn primary" onClick={submitForm}>
                  {addMode === 'qr' ? 'Открыть камеру' : addMode === 'sub' ? 'Импортировать' : 'Добавить'}
                </button>
              </div>
            </div>
          )}

          {/* Pinned / subscription servers */}
          {pinned.length > 0 && (
            <>
              <div className="pv-sechead">
                <span>Aurora VPN · подписка</span>
                <a onClick={() => t('Открываем план подписки')}>Изменить план ›</a>
              </div>
              <div className="pv-list">
                {pinned.map(s => (
                  <div key={s.id}
                       className={"pv-row" + (s.active ? ' active' : '')}
                       onClick={() => on && activate(s.id)}>
                    <div className="pv-flag">{s.n.split(' ').pop().slice(0, 2)}</div>
                    <div className="pv-info">
                      <b>{s.n}</b>
                      <span>{s.type} · {s.host}</span>
                    </div>
                    <button className={"pv-ping " + pingCls(s.ping)}
                            onClick={(e) => { e.stopPropagation(); test(s.id); }}>
                      {testing === s.id ? '···' : s.ping + ' мс'}
                    </button>
                  </div>
                ))}
              </div>
            </>
          )}

          {subServ.length > 0 && (
            <>
              <div className="pv-sechead">
                <span>Из подписки</span>
                <a onClick={() => t('Обновляем подписку…')}>Обновить ›</a>
              </div>
              <div className="pv-list">
                {subServ.map(s => (
                  <div key={s.id}
                       className={"pv-row" + (s.active ? ' active' : '')}
                       onClick={() => on && activate(s.id)}>
                    <div className="pv-flag">{s.n.split(' ').pop().slice(0, 2)}</div>
                    <div className="pv-info">
                      <b>{s.n}</b>
                      <span>{s.type} · {s.host}</span>
                    </div>
                    <button className={"pv-ping " + pingCls(s.ping)}
                            onClick={(e) => { e.stopPropagation(); test(s.id); }}>
                      {testing === s.id ? '···' : s.ping + ' мс'}
                    </button>
                  </div>
                ))}
              </div>
            </>
          )}

          {manual.length > 0 && (
            <>
              <div className="pv-sechead">
                <span>Свои серверы</span>
                <a onClick={() => { setAddMode('manual'); setAddOpen(true); }}>+ Ещё</a>
              </div>
              <div className="pv-list">
                {manual.map(s => (
                  <div key={s.id}
                       className={"pv-row" + (s.active ? ' active' : '')}
                       onClick={() => on && activate(s.id)}>
                    <div className="pv-flag">{s.n.split(' ').pop().slice(0, 2)}</div>
                    <div className="pv-info">
                      <b>{s.n}</b>
                      <span>{s.type} · {s.host}</span>
                    </div>
                    <button className={"pv-ping " + pingCls(s.ping)}
                            onClick={(e) => { e.stopPropagation(); test(s.id); }}>
                      {testing === s.id ? '···' : s.ping + ' мс'}
                    </button>
                    <button className="pv-del" onClick={(e) => deleteServer(s.id, e)} title="Удалить">✕</button>
                  </div>
                ))}
              </div>
            </>
          )}
        </>
      )}

      {/* RULES TAB */}
      {tab === 'rules' && (
        <>
          <div className="pv-sechead">
            <span>Правила маршрутизации</span>
            <a onClick={() => setRuleAddOpen(true)}>+ Добавить</a>
          </div>
          {ruleAddOpen && (
            <div className="pv-form">
              <div className="pv-form-head">
                <span>Новое правило</span>
                <span className="close" onClick={() => setRuleAddOpen(false)}>✕</span>
              </div>
              <div className="pv-field-row">
                <div className="pv-field">
                  <label>Тип</label>
                  <select value={ruleForm.kind} onChange={e => setRuleForm(r => ({ ...r, kind: e.target.value }))}>
                    <option value="domain">Домен</option>
                    <option value="ip">IP / CIDR</option>
                    <option value="geoip">GeoIP</option>
                    <option value="app">Приложение</option>
                  </select>
                </div>
                <div className="pv-field">
                  <label>Действие</label>
                  <select value={ruleForm.act} onChange={e => setRuleForm(r => ({ ...r, act: e.target.value }))}>
                    <option value="proxy">Прокси</option>
                    <option value="direct">Напрямую</option>
                    <option value="block">Заблокировать</option>
                  </select>
                </div>
              </div>
              <div className="pv-field">
                <label>Значение</label>
                <input placeholder={ruleForm.kind === 'geoip' ? 'RU' : ruleForm.kind === 'ip' ? '10.0.0.0/8' : 'example.com'}
                       value={ruleForm.v}
                       onChange={e => setRuleForm(r => ({ ...r, v: e.target.value }))} />
              </div>
              <div className="pv-form-actions">
                <button className="btn ghost" onClick={() => setRuleAddOpen(false)}>Отмена</button>
                <button className="btn primary" onClick={addRule}>Добавить</button>
              </div>
            </div>
          )}
          <div className="pv-rules">
            {rules.map(r => (
              <div key={r.id} className={"pv-rule" + (r.on ? '' : ' off')}>
                <span className={"pv-rule-kind k-" + r.kind}>{kindLabel[r.kind]}</span>
                <div className="pv-rule-v">
                  <b>{r.v}</b>
                  <span>{actLabel[r.act]}</span>
                </div>
                <span className={"pv-rule-act a-" + r.act}>
                  {r.act === 'proxy' ? '⛨' : r.act === 'direct' ? '→' : '✕'}
                </span>
                <div className={"toggle sm" + (r.on ? ' on' : '')}
                     onClick={() => toggleRule(r.id)}></div>
                <button className="pv-del" onClick={() => deleteRule(r.id)} title="Удалить">✕</button>
              </div>
            ))}
          </div>
          <div className="pv-note">
            Порядок правил важен — они проверяются сверху вниз. Всё, что не подошло, идёт по режиму «{modeInfo[mode].t}».
          </div>
        </>
      )}

      {/* DNS TAB */}
      {tab === 'dns' && (
        <>
          <div className="pv-sechead"><span>Режим DNS</span></div>
          <div className="pv-dns-modes">
            {[
              { k: 'fakeip', t: 'Fake-IP',   s: 'Быстро, минимум утечек, рекомендуется' },
              { k: 'remote', t: 'Remote',    s: 'DNS через прокси-сервер' },
              { k: 'direct', t: 'Direct',    s: 'DNS напрямую, без VPN' }
            ].map(x => (
              <button key={x.k}
                      className={"pv-dns-mode" + (dnsMode === x.k ? ' on' : '')}
                      onClick={() => { setDnsMode(x.k); t('DNS: ' + x.t); }}>
                <div className="pv-dns-r"><i></i></div>
                <div className="pv-dns-t">
                  <b>{x.t}</b>
                  <span>{x.s}</span>
                </div>
              </button>
            ))}
          </div>

          <div className="pv-sechead"><span>Серверы DNS</span></div>
          <div className="pv-list">
            {[
              { n: 'Cloudflare · 1.1.1.1',  s: 'DNS-over-HTTPS · рекомендуется', ping: 12, primary: 1 },
              { n: 'Google · 8.8.8.8',       s: 'DNS-over-HTTPS',                 ping: 28 },
              { n: 'AdGuard · 94.140.14.14', s: 'С блокировкой рекламы',          ping: 34 },
              { n: 'Локальный · роутер',     s: 'Использовать системный DNS',    ping: 4  }
            ].map((d, i) => (
              <div key={i} className={"pv-row" + (d.primary ? ' active' : '')}
                   onClick={() => t('DNS выбран: ' + d.n)}>
                <div className="pv-flag" style={{ fontSize: 11 }}>DNS</div>
                <div className="pv-info">
                  <b>{d.n}</b>
                  <span>{d.s}</span>
                </div>
                <span className={"pv-ping " + pingCls(d.ping)}>{d.ping} мс</span>
              </div>
            ))}
          </div>

          <div className="pv-note">
            Fake-IP использует локальный резолвер: приложения получают синтетические
            IP, а трафик уходит уже через VPN. Быстрее и меньше утечек, но иногда
            ломает клиенты, которым нужен реальный IP.
          </div>
        </>
      )}

      {/* ADVANCED TAB */}
      {tab === 'adv' && (
        <>
          <div className="pv-sechead"><span>Автоматизация</span></div>
          <div className="st-g" style={{ margin: '4px 16px 4px' }}>
            <div className="st-r" onClick={() => togglePref('autoOn')}>
              <div className="st-i">
                <b>Автоподключение</b>
                <span>Включать VPN, если источник в чёрном списке</span>
              </div>
              <div className={"toggle" + (prefs.autoOn ? ' on' : '')}></div>
            </div>
            <div className="st-r" onClick={() => togglePref('wifi')}>
              <div className="st-i">
                <b>Только через мобильный интернет</b>
                <span>По Wi-Fi — не подключаться</span>
              </div>
              <div className={"toggle" + (prefs.wifi ? ' on' : '')}></div>
            </div>
            <div className="st-r" onClick={() => togglePref('killSwitch')}>
              <div className="st-i">
                <b>Kill switch</b>
                <span>Блокировать трафик, если VPN отвалится</span>
              </div>
              <div className={"toggle" + (prefs.killSwitch ? ' on' : '')}></div>
            </div>
          </div>

          <div className="pv-sechead"><span>Сетевой стек</span></div>
          <div className="st-g" style={{ margin: '4px 16px 4px' }}>
            <div className="st-r" onClick={() => togglePref('tun')}>
              <div className="st-i">
                <b>TUN-режим</b>
                <span>Заворачивать весь системный трафик через виртуальный интерфейс</span>
              </div>
              <div className={"toggle" + (prefs.tun ? ' on' : '')}></div>
            </div>
            <div className="st-r" onClick={() => togglePref('sniff')}>
              <div className="st-i">
                <b>Sniffing</b>
                <span>Определять домен по SNI — даёт правилам сработать раньше</span>
              </div>
              <div className={"toggle" + (prefs.sniff ? ' on' : '')}></div>
            </div>
            <div className="st-r" onClick={() => togglePref('ipv6')}>
              <div className="st-i">
                <b>IPv6</b>
                <span>Проксировать IPv6 трафик</span>
              </div>
              <div className={"toggle" + (prefs.ipv6 ? ' on' : '')}></div>
            </div>
            <div className="st-r" onClick={() => togglePref('lan')}>
              <div className="st-i">
                <b>Разрешить LAN</b>
                <span>Локальная сеть работает в обход правил</span>
              </div>
              <div className={"toggle" + (prefs.lan ? ' on' : '')}></div>
            </div>
            <div className="st-r" onClick={() => togglePref('ads')}>
              <div className="st-i">
                <b>Блокировщик рекламы</b>
                <span>Локальные правила фильтрации на уровне DNS</span>
              </div>
              <div className={"toggle" + (prefs.ads ? ' on' : '')}></div>
            </div>
          </div>

          <div className="pv-sechead"><span>Диагностика</span></div>
          <div className="pv-actions">
            <button className="pv-action-b" onClick={() => t('Открываем журнал соединений')}>
              <span className="ic"><Icon name="menu" /></span>
              <div><b>Журнал соединений</b><span>Последние 500 запросов</span></div>
              <em>›</em>
            </button>
            <button className="pv-action-b" onClick={() => t('Проверка утечки DNS…')}>
              <span className="ic">◇</span>
              <div><b>Проверка утечки DNS</b><span>Убедиться, что запросы идут через VPN</span></div>
              <em>›</em>
            </button>
            <button className="pv-action-b" onClick={() => t('Кеш очищен')}>
              <span className="ic">✕</span>
              <div><b>Очистить кеш</b><span>Соединения, DNS, статистика</span></div>
              <em>›</em>
            </button>
            <button className="pv-action-b" onClick={() => t('Экспорт конфига…')}>
              <span className="ic"><Icon name="download" /></span>
              <div><b>Экспорт конфигурации</b><span>sing-box.json · для бэкапа</span></div>
              <em>›</em>
            </button>
          </div>
        </>
      )}

      <div style={{ padding: '16px 16px 30px', fontSize: 11,
                    color: 'var(--on-surface-variant)', textAlign: 'center', lineHeight: 1.5 }}>
        Подключение шифрованное. Ключи хранятся локально. Логи трафика не ведём.
      </div>

      {toast && <div className="st-toast">{toast}</div>}
    </div>
  );
}

window.Proxy = Proxy;
