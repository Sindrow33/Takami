// Единый набор SVG-иконок в языке нижнего таб-бара (stroke 1.8, round).
// Использование: <Icon name="home" /> или <Icon name="home" size="sm" />
// Пути хранятся как raw JSX-строки → рендерятся через dangerouslySetInnerHTML,
// чтобы избежать проблем с порядком загрузки babel-скриптов.
const ICON_PATHS = {
  home:      '<path d="M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/>',
  library:   '<rect x="4" y="4" width="4.5" height="16" rx="1"/>'
           + '<rect x="10" y="6" width="4.5" height="14" rx="1"/>'
           + '<path d="M16.5 8.4 18.9 8l2.9 12.7-2.4.4z"/>',
  calendar:  '<rect x="3.5" y="5" width="17" height="15" rx="2.5"/>'
           + '<path d="M3.5 10h17M8 3v4M16 3v4"/>'
           + '<circle cx="12" cy="15" r="1.4" fill="currentColor" stroke="none"/>',
  more:      '<g fill="currentColor" stroke="none">'
           + '<circle cx="6" cy="12" r="1.8"/>'
           + '<circle cx="12" cy="12" r="1.8"/>'
           + '<circle cx="18" cy="12" r="1.8"/></g>',
  spark:     '<path d="M12 3v6M12 15v6M3 12h6M15 12h6M6 6l3.5 3.5M14.5 14.5 18 18M6 18l3.5-3.5M14.5 9.5 18 6"/>',
  search:    '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-3.5-3.5"/>',
  settings:  '<circle cx="12" cy="12" r="3"/>'
           + '<path d="M19.4 12a7.4 7.4 0 0 0-.1-1.4l2-1.5-2-3.4-2.3.9a7.4 7.4 0 0 0-2.4-1.4L14 2.5h-4L9.4 5a7.4 7.4 0 0 0-2.4 1.4l-2.3-.9-2 3.4 2 1.5A7.4 7.4 0 0 0 4.6 12c0 .5 0 1 .1 1.4l-2 1.5 2 3.4 2.3-.9a7.4 7.4 0 0 0 2.4 1.4L10 21.5h4l.6-2.5a7.4 7.4 0 0 0 2.4-1.4l2.3.9 2-3.4-2-1.5c.1-.4.1-.9.1-1.4z"/>',
  menu:      '<g fill="currentColor" stroke="none">'
           + '<circle cx="12" cy="5" r="1.8"/>'
           + '<circle cx="12" cy="12" r="1.8"/>'
           + '<circle cx="12" cy="19" r="1.8"/></g>',
  back:      '<path d="m15 6-6 6 6 6"/>',
  play:      '<path d="M8 5v14l11-7z" fill="currentColor" stroke="none"/>',
  pause:     '<rect x="7" y="5" width="3.5" height="14" rx=".8" fill="currentColor" stroke="none"/>'
           + '<rect x="13.5" y="5" width="3.5" height="14" rx=".8" fill="currentColor" stroke="none"/>',
  prev:      '<rect x="5" y="6" width="2" height="12" rx=".6" fill="currentColor" stroke="none"/>'
           + '<path d="M9 12 20 5v14z" fill="currentColor" stroke="none"/>',
  next:      '<rect x="17" y="6" width="2" height="12" rx=".6" fill="currentColor" stroke="none"/>'
           + '<path d="M15 12 4 5v14z" fill="currentColor" stroke="none"/>',
  plus:      '<path d="M12 5v14M5 12h14"/>',
  refresh:   '<path d="M4 12a8 8 0 0 1 14.9-4M20 12a8 8 0 0 1-14.9 4"/>'
           + '<path d="M19 4v4h-4M5 20v-4h4"/>',
  download:  '<path d="M12 4v11"/><path d="m7 10 5 5 5-5"/><path d="M5 19h14"/>',
  spark4:    '<path d="M12 3l2 6 6 3-6 3-2 6-2-6-6-3 6-3z"/>',
  chevron:   '<path d="m9 6 6 6-6 6"/>',
  arrowL:    '<path d="M14 6l-6 6 6 6"/>',
  arrowR:    '<path d="M10 6l6 6-6 6"/>',
  info:      '<circle cx="12" cy="12" r="9"/><path d="M12 8h.01M11 12h1v5h1"/>',
  dot:       '<circle cx="12" cy="12" r="2.5" fill="currentColor" stroke="none"/>',
  alert:     '<path d="M12 3 2 20h20z"/><path d="M12 10v5M12 18h.01"/>',
  check:     '<path d="m5 12 5 5L20 7"/>',
  bookmark:  '<path d="M6 4h12v17l-6-4-6 4z"/>',
  book:      '<path d="M4 5a1 1 0 0 1 1-1h6v16H5a1 1 0 0 1-1-1z"/>'
           + '<path d="M13 4h6a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-6z"/>',
  // --- v4 additions ---
  heart:     '<path d="M12 21s-8-5.5-8-11a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 5.5-8 11-8 11z"/>',
  brain:     '<path d="M8 4a3.5 3.5 0 0 0-3.5 3.5c0 .5.1 1 .3 1.4A3.5 3.5 0 0 0 4 12a3.5 3.5 0 0 0 1 2.5A3 3 0 0 0 8 19a3 3 0 0 0 4-1V4a3.5 3.5 0 0 0-4 0z"/>'
           + '<path d="M16 4a3.5 3.5 0 0 1 3.5 3.5c0 .5-.1 1-.3 1.4A3.5 3.5 0 0 1 20 12a3.5 3.5 0 0 1-1 2.5A3 3 0 0 1 16 19a3 3 0 0 1-4-1V4a3.5 3.5 0 0 1 4 0z"/>'
           + '<path d="M9 8h.01M9 12h.01M9 16h.01M15 8h.01M15 12h.01M15 16h.01"/>',
  bell:      '<path d="M6 8a6 6 0 0 1 12 0c0 6 2 8 2 8H4s2-2 2-8z"/>'
           + '<path d="M10 20a2 2 0 0 0 4 0"/>',
  folder:    '<path d="M3 6a1 1 0 0 1 1-1h5l2 2h9a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z"/>',
  battery:   '<rect x="2" y="7" width="18" height="10" rx="2"/>'
           + '<path d="M22 10v4"/>'
           + '<rect x="4" y="9" width="6" height="6" rx=".5" fill="currentColor" stroke="none"/>',
  shield:    '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/>',
  doc:       '<path d="M6 3h8l5 5v12a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"/>'
           + '<path d="M14 3v5h5"/>',
  eye:       '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/>'
           + '<circle cx="12" cy="12" r="3"/>',
  eyeOff:    '<path d="M3 3l18 18"/>'
           + '<path d="M10.6 6.2A10 10 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-2.6 3.4"/>'
           + '<path d="M6.7 6.8A17 17 0 0 0 2 12s3.5 7 10 7c1.4 0 2.7-.3 3.9-.7"/>'
           + '<path d="M9.4 9.4a3 3 0 0 0 4.2 4.2"/>',
  copy:      '<rect x="8" y="8" width="12" height="12" rx="2"/>'
           + '<path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
  close:     '<path d="M6 6l12 12M6 18 18 6"/>',
  home:      '<path d="M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/>',
  external:  '<path d="M14 4h6v6"/>'
           + '<path d="M20 4 10 14"/>'
           + '<path d="M20 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h5"/>',
  send:      '<path d="M22 2 11 13"/>'
           + '<path d="M22 2 15 22l-4-9-9-4z"/>',
  edit:      '<path d="M4 20h4L20 8l-4-4L4 16z"/>'
           + '<path d="M14 6l4 4"/>',
  github:    '<path d="M12 2a10 10 0 0 0-3.2 19.5c.5.1.7-.2.7-.5v-1.7c-2.8.6-3.4-1.4-3.4-1.4-.4-1-1-1.4-1-1.4-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.5 2.4 1.1 3 .8.1-.7.4-1.1.7-1.4-2.2-.3-4.6-1.1-4.6-5a4 4 0 0 1 1-2.7c-.1-.3-.4-1.3.1-2.7 0 0 .9-.3 2.8 1a9.6 9.6 0 0 1 5 0c1.9-1.3 2.8-1 2.8-1 .5 1.4.2 2.4.1 2.7a4 4 0 0 1 1 2.7c0 3.9-2.4 4.7-4.6 5 .4.3.7.9.7 1.9v2.7c0 .3.2.6.7.5A10 10 0 0 0 12 2z" fill="currentColor" stroke="none"/>',
  telegram:  '<path d="M22 3 2 11l6 2 2 7 4-4 6 5z"/><path d="M8 13l14-10-9 12"/>',
  qr:        '<rect x="3" y="3" width="7" height="7" rx="1"/>'
           + '<rect x="14" y="3" width="7" height="7" rx="1"/>'
           + '<rect x="3" y="14" width="7" height="7" rx="1"/>'
           + '<path d="M14 14h3v3h-3zM19 14h2M14 19h2M17 17v2M20 17v4"/>',
  headphones:'<path d="M4 15v-3a8 8 0 0 1 16 0v3"/>'
           + '<path d="M4 15a2 2 0 0 1 2-2h1v6H6a2 2 0 0 1-2-2z"/>'
           + '<path d="M20 15a2 2 0 0 0-2-2h-1v6h1a2 2 0 0 0 2-2z"/>',
  compass:   '<circle cx="12" cy="12" r="9"/>'
           + '<path d="m9.5 14.5 5-2 -2 5 -5 2z"/>',
  wallet:    '<rect x="3" y="6" width="18" height="14" rx="2"/>'
           + '<path d="M3 10h18"/><path d="M18 15h.01"/>',
  usb:       '<circle cx="6" cy="18" r="2"/>'
           + '<path d="M6 16V8l6 3v3l4-2V6"/>'
           + '<path d="M14 4h6v4h-6z"/>',
  filter:    '<path d="M4 5h16l-6 8v6l-4-2v-4z"/>',
  users:     '<circle cx="9" cy="8" r="3.5"/>'
           + '<path d="M2 20a7 7 0 0 1 14 0"/>'
           + '<circle cx="17" cy="9" r="2.5"/>'
           + '<path d="M22 20a5 5 0 0 0-5-5"/>',
  music:     '<circle cx="6" cy="18" r="2.5"/>'
           + '<circle cx="17" cy="16" r="2.5"/>'
           + '<path d="M8.5 18V6l11-2v12"/>',
  bookOpen:  '<path d="M2 5a1 1 0 0 1 1-1h6a3 3 0 0 1 3 3v14a2 2 0 0 0-2-2H2z"/>'
           + '<path d="M22 5a1 1 0 0 0-1-1h-6a3 3 0 0 0-3 3v14a2 2 0 0 1 2-2h8z"/>',
  play:      '<path d="M8 5v14l11-7z" fill="currentColor" stroke="none"/>',
  chart:     '<path d="M3 3v18h18"/>'
           + '<path d="M7 17V9M12 17V6M17 17v-5"/>',
  cursor:    '<path d="M6 3l14 8-6 2-3 7z"/>',
  spark2:    '<path d="M12 3v3M12 18v3M4.5 4.5l2 2M17.5 17.5l2 2M3 12h3M18 12h3M4.5 19.5l2-2M17.5 6.5l2-2"/>'
           + '<circle cx="12" cy="12" r="3" fill="currentColor" stroke="none"/>',
  swipes:    '<path d="M9 3 5 7l4 4"/>'
           + '<path d="M15 21 19 17l-4-4"/>'
           + '<path d="M5 7h11a4 4 0 0 1 4 4v0"/>'
           + '<path d="M19 17H8a4 4 0 0 1-4-4v0"/>',
  news:      '<rect x="3" y="4" width="18" height="16" rx="2"/>'
           + '<path d="M7 8h10M7 12h10M7 16h6"/>',
  clock:     '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  volume:    '<path d="M4 10v4h4l5 4V6L8 10z"/>'
           + '<path d="M17 9a4 4 0 0 1 0 6"/>',
  paste:     '<rect x="8" y="4" width="8" height="4" rx="1"/>'
           + '<path d="M16 6h2a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h2"/>',
};

function Icon({ name, size, style, className }) {
  const inner = ICON_PATHS[name];
  if (!inner) return null;
  const cls = 'icn' + (size ? ' ' + size : '') + (className ? ' ' + className : '');
  return (
    <svg
      className={cls}
      viewBox="0 0 24 24"
      style={style}
      dangerouslySetInnerHTML={{ __html: inner }}
    />
  );
}

window.Icon = Icon;
