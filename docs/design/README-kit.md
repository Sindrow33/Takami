# Handoff: Takami v4.1 — Онбординг с аниме-приветствием, редизайн иконок, автопарсер, живое превью

## Overview

**Takami** — русскоязычный клиент-читалка для манги, аниме и ранобэ. Одна франшиза в трёх форматах со сквозным прогрессом. Aurora-дизайн: тёмная база, фиолетовое свечение, стеклянные поверхности.

Этот пакет — **v4.1** приложения, в котором сделаны 10 крупных доработок поверх существующего UI-kit:

1. Все иконки-кнопки переведены с юникод-глифов на аккуратные SVG (Lucide-подобный набор, stroke 1.8, round).
2. Убрана отдельная вкладка Donate; вместо неё в самом низу настроек — широкая кнопка «Поддержать разработку» с анимированным сердечком (покачивание + пульс), разворачивающая список ссылок (Boosty / Tribute / Patreon / Ko-fi / Крипта).
3. FAB в нижнем таббаре (запуск «свайпов» — Tinder-подобного подбора тайтлов) при загрузке пульсирует и заменяет иконку на внутренний спиннер, а не рисует обводку-прогресс.
4. Превью — один телефон-эмулятор по центру страницы вместо длинного канваса со всеми экранами. Все кнопки в превью **рабочие** (переключают экраны или показывают контекстные toast-сообщения — никаких «— заглушка»).
5. Исправлен React-баг: у второстепенных персонажей на карточках больше не появляется «0» (было `{c.main && …}` при `c.main === 0`).
6. В календаре — точки под днями теперь по цветам типа контента: 🔵 аниме, 🟢 манга, 🟣 ранобэ. Максимум три круга, если релизы всех трёх типов одновременно. Плюс легенда над лентой.
7. Каскадная stagger-анимация при переходе между вкладками — элементы появляются сверху вниз с blur→clear.
8. В шапке главной, рядом с кнопкой настроек — индикатор обучаемости самовосстанавливающегося автопарсера (мозг + мини-график + процент). Тап открывает bottom-sheet с прогресс-кольцом, статистикой и логом самопочинок.
9. Онбординг: Splash (лого) → Политика с чекбоксом «претензий не имею» → Разрешения (уведомления/хранилище/батарея) → Приветствие «Добро пожаловать, хозяин!» → Главная. Показывается один раз, состояние в `localStorage`.
10. **Welcome-экран (v4.1)**: вместо абстрактной композиции — реальная аниме-иллюстрация девушки с peace-жестами. «Оживлена» многослойной анимацией (дыхание + покачивание + мягкий glow), рядом — речевой пузырёк с фразой **«Добро пожаловать, хозяин!»** и подписью `お帰りなさいませ`. За спиной — крупные полупрозрачные иероглифы お帰り, вокруг летают искры. Внизу — CTA «Войти в приложение» с pulse-glow. Автоперехода нет — пользователь сам решает, когда войти.

---

## About the Design Files

Файлы в этом пакете — **дизайн-референсы, созданные в HTML/React (Babel-standalone)**. Это прототипы, которые демонстрируют внешний вид и поведение, а **не production-код для прямого копирования**.

Задача разработчика — **воспроизвести эти дизайны в целевом окружении Takami** (Kotlin + Jetpack Compose, судя по `_src/`), используя существующие в проекте паттерны, компоненты Material 3, темы (`Theme.kt`, `Color.kt`, `Type.kt`) и Aurora-токены. Если целевого окружения ещё нет — выбрать подходящий стек (Compose Multiplatform / React Native / Flutter / SwiftUI) и реализовать в нём.

Прототип использует React 18 + inline Babel + собственный набор CSS-классов, потому что это самая быстрая среда для итераций дизайна — но в реальном приложении соответствующие компоненты должны быть нативными Composables (или View-иерархией нужной платформы).

---

## Fidelity

**High-fidelity (hifi).** Пиксель-перфект мокапы с финальными цветами, типографикой, отступами, тенями и анимациями. Разработчик должен воспроизвести UI с максимальной точностью, используя существующую дизайн-систему приложения (`core-design`).

Все значения (hex-цвета, пиксели отступов, длительности анимаций, easings) — финальные и указаны ниже в разделе **Design Tokens**.

---

## Screens / Views

### 0. Onboarding (первый запуск)

Показывается один раз, состояние: `localStorage.setItem('takami:onboarded', '1')`. В нативе — `SharedPreferences` / `DataStore` под ключом `onboarded=true`.

#### 0.1 Splash
- **Purpose**: первое впечатление, брендинг, лоадинг.
- **Layout**: центр по обеим осям, вертикальный column. Padding `20px`.
- **Компоненты**:
  - Лого-плитка 108×108, radius 30, градиент `linear-gradient(145deg, #8E72FF, #5B3BE8)`, тень `0 20px 60px rgba(91,59,232,.6), 0 0 60px rgba(124,92,255,.5)`, внутри белая звезда 56×56 (SVG path: 5-конечная звезда). Анимация `logoBreath 2.5s ease-in-out infinite` — scale от 1 до 1.05, тень усиливается на пике.
  - Заголовок «Takami» — `Zen Kaku Gothic Antique 900`, 44px, letter-spacing `-0.02em`, color `#fff`.
  - Подпись `高見 · 見る` — 15px, letter-spacing `.3em`, color `--acc-2` (`#A78BFA`).
  - Тег: «Манга, аниме и ранобэ — в одном приложении, с общим прогрессом.» — 13px, color `--on-surface-variant` (`#94A3B8`), max-width 260px.
  - Индикатор загрузки 120×3 px, `rgba(255,255,255,.06)`, внутри бегающий градиент `linear-gradient(90deg, transparent, #7C5CFF, transparent)`, `loadSlide 1.6s linear infinite`.
- **Автопереход** через **1.8 секунды** → Policy.

#### 0.2 Policy
- **Purpose**: показать пользователю юридические условия и получить явное согласие.
- **Layout**: column, `padding: 44px 24px 24px`, скроллируемый (`overflow-y: auto`).
- **Компоненты**:
  - Progress dots — вверху (2-й активен из 3-х).
  - H1 «Пара слов, прежде чем начнём» — 22px / 700 / Zen Kaku Gothic Antique.
  - Sub: «Takami — открытый клиент. Мы уважаем вас и просим уважать наши условия.» — 13px / `--on-surface-variant`, line-height 1.6.
  - **3 карточки** (`onb-card`): padding 14px, radius 14, `background: rgba(255,255,255,.03)`, border `1px solid rgba(255,255,255,.08)`. Внутри `<b>` (13/600/#fff) + `<span>` (12 / `rgba(255,255,255,.72)` / line-height 1.55).
    - «Мы не хостим контент» — приложение только инструмент просмотра, права на контент — у владельцев.
    - «Встроенный VPN — для удобства» — прокси-клиент, не логируем, ключи только на устройстве.
    - «За контент отвечает источник» — если парсер сломался — не наша вина; автопарсер попробует восстановиться.
  - **Чекбокс** `onb-check`: flex row, gap 12, padding 14, radius 14, `background: rgba(124,92,255,.06)`, border `rgba(124,92,255,.2)`. Слева квадрат 22×22 с бордером; при активации — градиент фиолетовый, галочка (stroke 3, opacity 0→1). Текст: «Я прочитал(а) и согласен(-а). Претензий по контенту к приложению **не имею**.» (последние слова — `--acc-2`).
  - **CTA** `onb-cta` — full width, 15px padding, radius 14, градиент фиолет, при disabled = `rgba(255,255,255,.06) / rgba(255,255,255,.35)`, кнопка «Продолжить». Активна только если чекбокс поставлен.

#### 0.3 Permissions
- **Purpose**: запрос разрешений на уведомления / хранилище / отключение экономии батареи.
- **Компоненты**:
  - Progress dots (3-й активен).
  - H1 «Нужны разрешения» / Sub «Дайте согласие сейчас, потом настройки можно поменять в системе.»
  - **3 карточки разрешений** (`onb-perm`): flex row, gap 14, padding 16, radius 16.
    - Слева — квадратная иконка 44×44 radius 12, background `linear-gradient(145deg, rgba(124,92,255,.22), rgba(91,59,232,.1))` / border `rgba(124,92,255,.28)` / color `--acc-2`. При выданном разрешении: background `linear-gradient(145deg, #3DD68C, #24A566)`, glow `0 0 16px rgba(61,214,140,.5)`, color `#fff`.
    - Центр: `<b>` название (13/600) + `<span>` описание (11 / `--on-surface-variant`).
    - Справа — chip «Разрешить» → «Выдано» (chip меняет цвета).
    - Иконки: **bell**, **folder**, **battery** (см. Icons Set).
  - CTA внизу — «Пропустить и продолжить» / «Отлично, дальше» (по состоянию). Ниже мелким текстом: «Некоторые функции могут работать некорректно» — если не все выданы.

#### 0.4 Welcome (v4.1) — с аниме-девушкой

- **Purpose**: тёплый welcome-момент перед первым запуском. Заменяет абстрактную композицию из v4.
- **Layout**: `.onb-content.onb-welcome` — position: relative, display: block (сбрасывает flex), padding: 0, overflow: hidden.
- **Компоненты (все `position: absolute` внутри сцены `.onb-w-scene`)**:

  **Задние ореолы** (`.onb-w-halo` / `.onb-w-halo-2`)
  - `.onb-w-halo` — 340×340 radial gradient `rgba(124,92,255,.55) → transparent 75%`, blur 2px, animation `haloBreath 3.4s ease-in-out infinite` (scale 0.94↔1.10, opacity .7↔1).
  - `.onb-w-halo-2` — 220×220 розовый radial `rgba(255,150,190,.35) → transparent`, animation `haloBreath 4.2s -1.2s infinite` (сдвиг фазы).

  **Полупрозрачные иероглифы** (`.onb-w-kana`)
  - Три `<span>` с иероглифами `お`, `帰`, `り` — Zen Kaku Gothic Antique 900, color `rgba(255,255,255,.09)`, text-shadow `0 0 40px rgba(124,92,255,.4)`.
  - Размеры: 220px (お, top: 6%, left: -18px), 180px (帰, top: 18%, right: -10px), 160px (り, bottom: 24%, left: 40%, color розовый `rgba(255,150,190,.06)`).
  - Все с animation `kanaFloat 10s ease-in-out infinite` со сдвигом (translate ±12px, rotate ±5deg).

  **Искры** (`.onb-w-sparkles`)
  - 8 `<span>` — 4×4 круги (2 из них 3×3, 2 из них 5×5), `background: #fff / #FFB0D0 / #A78BFA`, box-shadow `0 0 6px + 0 0 14px`.
  - Animation `sparkleFloat 5s ease-in-out infinite` со сдвигами `0s ... -4.2s`, движутся по диагонали вверх с fade in/out.

  **Аниме-девушка** (`.onb-w-girl`)
  - `<img src="assets/welcome-girl.png">` в контейнере 360×640, `bottom: 0`, `left: 50%`, `transform: translateX(-50%)`.
  - Изображение: `701×1024` PNG с прозрачным фоном, `object-fit: contain`, `object-position: bottom center`.
  - Filter: `drop-shadow(0 8px 40px rgba(124,92,255,.55)) drop-shadow(0 0 24px rgba(167,139,250,.35))` — свечение вокруг силуэта.
  - **Многослойная анимация «оживления»**:
    - На контейнере: `girlBreath 3.6s ease-in-out infinite` — translateY 0→-2px, scale 1→1.012 (дыхание).
    - На img: `girlSway 4.8s ease-in-out infinite` — rotate -1.2°↔1.2°, transform-origin `50% 100%` (покачивание, как будто радуется).

  **Речевой пузырёк** (`.onb-w-bubble`)
  - Position absolute, top: 15%, right: 16px, max-width 200px.
  - Background: `linear-gradient(145deg, #FFFFFF, #F4EBFF)`, padding `14px 18px 16px`, border-radius `20px 20px 4px 20px` (asymmetric — «уголок» справа снизу).
  - Box-shadow: `0 8px 24px rgba(0,0,0,.35), 0 0 24px rgba(124,92,255,.35)`.
  - Внутри:
    - `.onb-w-bubble-jp` — «お帰りなさいませ», Zen Kaku Gothic Antique 500, 11px, `#7C5CFF`, letter-spacing `.12em`, margin-bottom 4px.
    - `.onb-w-bubble-txt` — **«Добро пожаловать, хозяин!»**, Zen Kaku Gothic Antique 700, 17px, line-height 1.2, color `#0F1116`.
  - `.onb-w-bubble-tail` — треугольный «хвостик» пузыря 14×14, полигональный clip-path, поворот 45°.
  - Animations:
    - `bubblePop 0.5s cubic-bezier(.34,1.56,.64,1) 0.4s both` — появление с overshoot bounce (scale 0→1.08→1, rotate -10°→2°→0°).
    - `bubbleWiggle 3.6s ease-in-out 0.9s infinite` — rotate -1.5°↔1.5° (живой пузырь).

  **Футер** (`.onb-w-footer`, absolute bottom)
  - Gradient overlay: `linear-gradient(180deg, transparent 0%, rgba(10,12,16,.55) 25%, rgba(10,12,16,.92) 70%, #0A0C10 100%)` — для читаемости текста поверх сцены.
  - Padding: `40px 24px 28px`.
  - `.onb-w-msg` — «Всё готово. Приятного чтения.», 13/rgba(255,255,255,.72), center, line-height 1.5.
  - CTA `.onb-w-cta` — стандартная `.onb-cta` (градиентная фиолетовая, 15/600, radius 14), текст «Войти в приложение».
    - `animation: fadeUp .5s ease 1.2s both, ctaPulse 2.5s ease-in-out 2s infinite` — появляется с задержкой, потом пульсирует glow.
    - `ctaPulse`: box-shadow пульсирует от `0 8px 24px rgba(91,59,232,.5), 0 0 24px rgba(124,92,255,.35)` до `0 12px 32px rgba(91,59,232,.75), 0 0 36px rgba(124,92,255,.6)`.

- **НЕТ автоперехода** (в отличие от Splash). Пользователь нажимает кнопку сам.
- При `prefers-reduced-motion: reduce` — все анимации 1ms.

#### Progress dots (общий компонент `onb-dots`)
Ряд из 3 точек 6×6, gap 6. Активная — ширина 22, градиент `linear-gradient(90deg, #A78BFA, #5B3BE8)`, glow. Пройденные — `rgba(124,92,255,.4)`. Splash не отображает dots.

---

### 1. Home (главная)

#### Топ-бар (`hm3-top`)
- Слева: `hm3-date` (день недели + число, 11px UPPERCASE `--on-surface-variant`) + `hm3-hi` (приветствие по времени суток, 20px / 700 / display font).
- Справа: **AiIndicator** + иконка **search** + иконка **settings**.

Приветствие меняется:
- 0–5: «Доброй ночи»
- 5–12: «Доброе утро»
- 12–18: «Добрый день»
- 18–24: «Добрый вечер»

Второе слово (`, Читатель`) — тон `--on-surface-variant`.

#### AiIndicator (см. отдельный раздел ниже)

#### Hero-continue
- Крупная карточка ~360×220 с обложкой (radial gradient) на всю ширину.
- Тег вверху слева: «Продолжить · манга/аниме/ранобэ» — 10/500 UPPERCASE в pill.
- Стеклянная плитка снизу: имя тайтла (b, 17/600) + подпись (11 / `--on-surface-variant`) + прогресс-бар (2px, заливка `--primary`, справа `%` числом).
- Кнопка «Читать/Смотреть» — pill primary с иконкой (book/play).

#### Quick actions (4 колонки)
- Обновления · 3
- Календарь · сегодня
- Поиск · по кадру
- Свайпы · подбор

Каждая — вертикальная колонка: иконка 22×22, лейбл 11px, число/подзаголовок 10px.

#### Разделы (рельсы карточек)
- Продолжить (in-progress items), Манга, Аниме, Ранобэ.
- Каждая карточка `hm3-card`: обложка 3:4 с типовой иконкой в углу, бейдж «12» (число новых глав), progress-полоса. Ниже — название (12/500, 2 строки) + рейтинг «★ 8.7».

---

### 2. AI Indicator (шапка главной)

**Location**: `.hm3-top-r` в шапке главной.

**Кнопка** (`ai-hdr`):
- Height 32, padding `4px 10px 4px 6px`, radius 999.
- Background `linear-gradient(145deg, rgba(124,92,255,.22), rgba(91,59,232,.08))`, border `1px solid rgba(124,92,255,.35)`.
- Внутри: **мозг-иконка** 20×20 (круглый градиентный chip) + **мини-график** (5 столбиков 3px width, анимация `aiBar 2s` — scale Y от 0.6 до 1 со stagger `.15s`) + **`72%`** (10.5 / 600 / tabular-nums / `#E4DAFF`).
- Значение процента — состояние `pct`, инкрементируется псевдослучайно каждые 8 секунд (для демо; в проде — реальный сигнал от парсер-движка).
- Tap → открывает bottom-sheet.

**Bottom-sheet** (`ai-sheet`):
- Слайд снизу, radius `24 24 0 0`, background `linear-gradient(180deg, #1A1D25 0%, #12141A 100%)`, border-top `rgba(124,92,255,.35)`.
- Scrim `rgba(0,0,0,.6)` + blur 4.
- **Header**: 38×38 круглый chip с мозгом + `<b>Автопарсер · обучаемость</b>` + `<span>Самовосстанавливающийся движок. Учится на каждом запросе.</span>` + close × справа.
- **Прогресс-кольцо** SVG 92×92, `strokeWidth: 8`. Bg circle `rgba(255,255,255,.08)`, fg с `url(#ai-grad)` (`#A78BFA → #5B3BE8`), `stroke-linecap: round`, `filter: drop-shadow(0 0 4px rgba(124,92,255,.6))`. В центре — `72%` (22/700 tabular).
- Справа от кольца — описание, что модель учится на запросах, данные не покидают устройство.
- **Grid 2×2 статистики** (`ai-stat`, radius 14, `background: rgba(255,255,255,.03)`):
  - Источников: 14
  - Самопочинок: 38 (30 дн) — tone `ok` (`--ok`)
  - Точность: 96% — tone `ok`
  - Аномалий: 2 — tone `warn` (`--warn`)
- **Лог** (`ai-sheet-log`): моно-шрифт 10.5px, max-height 96px, background `rgba(0,0,0,.28)`. Записи типа `[3 мин] ReadManga · сменилась структура кнопок глав`. Цвет метки времени — `--acc-2`, статусы: `.log-ok` зелёный, `.log-w` жёлтый.

---

### 3. TabBar с FAB

- Fixed bottom, 5 tabs: Home, Library, **FAB** (свайпы), Calendar (с badge 3), Settings.
- Иконки: `home`, `library`, `swipes` (FAB), `calendar`, `settings`.
- **FAB** 56×56, `margin-top: -26px`, radius 50%, `linear-gradient(145deg, #8E72FF, #5B3BE8)`, тень белая-подложка + shadow-fab.
- **Loading state** (при tap на FAB):
  - Кнопка получает `.loading`, анимация `fab-pulse-strong 1.1s ease-in-out infinite`: scale 1 ↔ 1.08, shadow интенсивнее.
  - Обычная иконка `.fab-icn` — `opacity: 0`.
  - `.fab-spinner` — display flex, SVG 26×26, круг stroke-dasharray 60 / stroke-dashoffset 22, вращение `fab-spinner-rot 1s linear infinite`.
  - Длительность псевдо-загрузки: **1200ms**, затем переход на экран «Свайпы».
- Активная вкладка — `--primary` цвет иконки + `filter: drop-shadow(0 0 10px rgba(124,92,255,.75))`.

---

### 4. Character card fix

**Проблема**: в React `{c.main && <Tag/>}` при `c.main === 0` рендерил цифру `0` рядом со второстепенными персонажами.

**Fix**: `{!!c.main && <Tag/>}` — приводит к boolean, `false` → ничего не рендерится.

Затрагивает `.charrail .c-card` на Title-экране.

---

### 5. Calendar — цветные точки

**Location**: `.cal-strip .cal-day .cal-day-dots`.

**Логика** (было — суммарное число точек):
```js
const typesFor = strip.map(day => {
  const rels = releasesFor(day);
  const set = new Set(rels.map(r => r.item.type));
  return ['anime', 'manga', 'novel'].filter(t => set.has(t)); // порядок стабильный
});
```

**Рендер** (максимум 3 круга, порядок anime → manga → novel):
- Div `cal-day-dots` (flex, gap 3, justify-center, height 6).
- Внутри `<i class="tt-anime|manga|novel">` — 5×5 круги.
- Цвета:
  - `.tt-anime`  → `#0095FF` + glow `rgba(0,149,255,.55)`
  - `.tt-manga`  → `#3DD68C` + glow `rgba(61,214,140,.5)`
  - `.tt-novel`  → `#A78BFA` + glow `rgba(167,139,250,.55)`
- Если релизов нет — `.cal-day-dots.off` = 4×4 круг `rgba(255,255,255,.10)`.
- В выбранном дне (`.cal-day.on`) — все точки становятся белыми, glow снимается.

**Легенда** (`cal-legend`) под hero, над `cal-strip`: три чипа с цветным кружком 8×8 и подписью — «аниме / манга / ранобэ», 10.5 / `--on-surface-variant`.

---

### 6. Settings — Support Button (низ)

**Замена вкладки Donate.** Прямо после блока «О приложении», в самом низу настроек.

- Div `.st-support`, padding `20px 16px 40px`.
- **Кнопка** `.st-support-btn` — full width, padding `16px 20px`, radius 16, `background: rgba(255,255,255,.02)`, border `1.5px solid rgba(124,92,255,.35)`, color `#E4DAFF`, 15/600.
  - Внутри слева: **SVG-сердечко** 22×22, color `#FF6B8A`, filter `drop-shadow(0 0 8px rgba(255,107,138,.55))`.
  - Анимация `heartBeat 1.4s ease-in-out infinite`:
    ```
    0%:   scale(1) rotate(-6deg)
    15%:  scale(1.18) rotate(-6deg)
    30%:  scale(1) rotate(-6deg)
    45%:  scale(1.12) rotate(6deg)
    60%:  scale(1) rotate(6deg)
    100%: scale(1) rotate(-6deg)
    ```
    (Лёгкое покачивание + двойной пульс, как настоящее сердцебиение.)
  - Текст «Поддержать разработку» + справа шеврон (rotate 180 при open, transition 280ms).
  - Hover: border ярче, translateY(-1px), tinted radial glow под курсором (реализовано через `--mx / --my` CSS vars).
- **Разворачивающийся список** `.st-support-list`:
  - Появление: `donateExpand .32s`, opacity 0→1, translateY -6→0, max-height 0→600.
  - Padding 6, radius 14, `background: rgba(255,255,255,.02)`, border `rgba(255,255,255,.06)`.
  - Каждая ссылка `.st-support-link`: flex row, padding 12/14, radius 10.
    - Слева цветной квадрат-иконка 34×34 radius 10 с буквой сервиса (B, T, P, K, ₿) — 15/700/#fff. Цвета — по бренду сервиса.
    - `<b>` название + `<span>` описание.
    - Справа шеврон ›.
- **Сервисы**:
  - Boosty (`#FF5A26 → #E23000`) — «Ежемесячная подписка · от 100 ₽»
  - Tribute (`#38B6FF → #0079E5`) — «Через Telegram · разовые донаты»
  - Patreon (`#FF6249 → #E23A20`) — «Ежемесячная подписка · в USD»
  - Ko-fi (`#FF5E5B → #D93A3F`) — «Одноразовые донаты · чашка кофе»
  - Криптовалюта (`#F7931A → #B87513`) — «BTC · ETH · TON · USDT»

---

### 7. Working buttons — что кликается

Все интерактивные элементы в превью реально работают. Полный список починенных кнопок в v4.1 (вместо ранее пустых `onClick={() => {}}` или "— заглушка" toast'ов):

**Settings (11 контекстных toast'ов вместо пустых кликов)**:
- Категории → `Читаю · Планы · Заброшено · Любимые`
- Управление загрузками → `Открываем список активных загрузок`
- Очистить историю (danger) → `История чтения очищена`
- Заблокировать сейчас → `Приложение заблокировано · разблокируйте PIN-ом`
- Кеш изображений → `184 МБ · 1 247 файлов`
- Очистить кеш → `Кеш очищен · 184 МБ освобождено`
- Создать резервную копию → `Создаём резервную копию…`
- Восстановить из файла → `Выберите .bak файл`
- Автобэкапы → `Автобэкапы каждое воскресенье в 03:00`
- Экспорт логов → `Логи готовятся · takami-log.txt`
- Сбросить все настройки (danger) → `Настройки сброшены к значениям по умолчанию`

**Меню/поиск в AppBar каждого экрана** — было `"— заглушка"`, стало:
- Меню тайтла → `Поделиться · Скачать всё · Отслеживать · Мигрировать`
- Меню библиотеки → `Сортировка · Вид · Обновить всё`
- Меню Proxy → `Импорт · Экспорт конфигов · Статистика`
- Меню календаря → `Показать только: моя библиотека / все источники`
- Меню персонажа → `Поделиться · В избранное · Пожаловаться`
- Поиск по персонажу → `Поиск: другие тайтлы с этим персонажем`
- Поиск по тайтлу → `Поиск главы или страницы в тайтле`
- Поиск по настройкам → `Введите название настройки для быстрого перехода`

**Функциональные onClick** (реально переключают state):
- Все чипы фильтров (тип контента, "Только моё" в календаре)
- Все SegRow / SwitchRow / TrackerRow / SegSelect в настройках
- Sheet-ы плеера (subs, audio, speed, quality) — реально открываются
- Reader tap-zones + все настройки (mode, brightness, fontSize, tint, columnWidth, тумблеры)
- Sources — вкл/выкл источников, добавление URL репозитория
- Proxy — добавление серверов, переключение режима, DNS, сервер-пресеты, действия
- Search — фильтры, скриншот-анализ
- Все переходы между экранами (карточки тайтлов → Title → chars/reader/player, TabBar, Back-кнопки)
- Support-button в настройках — реально разворачивает список
- FAB — реальный переход на Swipes с pulse-loading

---

### 8. Все остальные экраны

Использовать существующие компоненты. **Единственное общее изменение** — все юникод-глифы `⌕ ⚙ ↻ ▦ ▶ ▤ ✦ ⋮ ⤓ ⓘ ⎘ ⌾ ⊙ ◐ ✈ ✎ ♪` заменены на компонент `<Icon name="..." />`. Список имён — в разделе **Icons Set** ниже.

---

## Interactions & Behavior

### Screen transitions (stagger cascade)

При переключении между вкладками — прямые дети `.screen-scroll` получают анимацию `cascadeIn 0.5s cubic-bezier(.16,1,.3,1)` с нарастающим `animation-delay` (0 / 40 / 80 / 120 / 160 / 200 / 235 / 265 / 290 / 310 ms).

Keyframe:
```
from { opacity: 0; transform: translateY(-18px); filter: blur(4px); }
to   { opacity: 1; transform: translateY(0);    filter: blur(0); }
```

Реализация в React — через изменение `key` у обёртки при смене экрана + класс `.enter`, снимаемый через 900ms.

Реализация в **Compose**: `AnimatedContent` + `enterTransition = slideInVertically(initialOffsetY = { -18.dp }) + fadeIn()`, применяя stagger через `LazyColumn` items + `animateItemPlacement` или через кастомный `Modifier.staggeredEntry(index)`.

`@media (prefers-reduced-motion: reduce)` → все длительности 1ms.

### FAB loading

- Tap → `setFabLoading(true)`, через 1200 ms → `setFabLoading(false)` + переход на «свайпы».
- В disabled состоянии кнопка не реагирует.

### Onboarding

- `Splash` → auto-forward через `setTimeout(1800)`.
- `Policy` → CTA disabled пока `agreed=false`.
- `Perms` → каждая карточка кликом → `perms[k] = true`. Всегда можно пропустить.
- `Welcome` → **НЕТ auto-forward** (в v4.1). Пользователь нажимает CTA «Войти в приложение» → пишет в `localStorage` + вызывает `onDone()`.

### Welcome-scene анимации (v4.1)

Все анимации `infinite`, отключаются через `prefers-reduced-motion: reduce`. Реализация в Compose — через `rememberInfiniteTransition().animateFloat(...)`.

| Компонент         | Animation                                 | Duration | Функция                                            |
|-------------------|-------------------------------------------|----------|----------------------------------------------------|
| `.onb-w-halo`     | `haloBreath` scale + opacity              | 3.4s     | ease-in-out infinite                               |
| `.onb-w-halo-2`   | `haloBreath` со сдвигом -1.2s             | 4.2s     | ease-in-out infinite                               |
| `.onb-w-kana span`| `kanaFloat` translate + rotate            | 10s      | ease-in-out infinite (delays 0 / -3.3 / -6.6)      |
| `.onb-w-sparkles span` | `sparkleFloat` translate up + fade   | 5s       | ease-in-out infinite (delays 0 / -.7 / -1.4 / ...) |
| `.onb-w-girl`     | `girlBreath` scale + translateY           | 3.6s     | ease-in-out infinite                               |
| `.onb-w-girl img` | `girlSway` rotate ±1.2°                   | 4.8s     | ease-in-out infinite                               |
| `.onb-w-bubble`   | `bubblePop` bounce entry                  | 0.5s     | cubic-bezier(.34,1.56,.64,1) 0.4s both             |
| `.onb-w-bubble`   | `bubbleWiggle` rotate ±1.5°               | 3.6s     | ease-in-out 0.9s infinite                          |
| `.onb-w-cta`      | `fadeUp` entry + `ctaPulse` glow          | 0.5s+2.5s| ease 1.2s both + ease-in-out 2s infinite           |

### AI Indicator

- Tap → открывает sheet (модальный, scrim, свайп-вниз не реализован в прототипе, но подразумевается для мобильного).
- Значение процента медленно растёт с течением времени (в проде — реальный сигнал).

---

## State Management

### Приложение (root state)

```
onboarded: boolean        // localStorage 'takami:onboarded'
screen: string            // 'home' | 'library' | 'title' | 'character' | 
                          // 'reader' | 'player' | 'sources' | 'swipes' | 
                          // 'calendar' | 'search' | 'proxy' | 'settings'
                          // → localStorage 'takami:screen'
prev: string              // куда возвращаться back
activeTab: string         // подсветка таббара
openedTitleId: number
openedCharId: number
fabLoading: boolean       // для FAB spinner
advOpen: boolean          // AdvancedSearch sheet
advFilters: object | null
focusAi: boolean          // при переходе из ридера/плеера в AI-настройки
screenKey: number         // increment on transition → force remount для stagger
```

### Onboarding

```
step: 'splash' | 'policy' | 'perms' | 'welcome'
agreed: boolean
perms: { notify: bool, storage: bool, battery: bool }
```

### AiIndicator

```
open: boolean
pct: number   // 72..99, инкремент каждые 8 сек (демо; в проде — from парсер-движка)
```

### SupportButton

```
open: boolean  // раскрыт ли список ссылок
```

---

## Design Tokens

Полный набор — в `colors_and_type.css` (скопирован в этот пакет).

### Colors (dark)

| Токен                | Hex                          |
| -------------------- | ---------------------------- |
| `--acc` (primary)    | `#7C5CFF` violet             |
| `--acc-dim`          | `#5B3BE8`                    |
| `--acc-2`            | `#A78BFA` soft               |
| `--acc-3`            | `#00E5FF` cyan               |
| `--acc-blue`         | `#0095FF`                    |
| `--acc-grad-a/b`     | `#8E72FF → #5B3BE8`          |
| `--ok`               | `#3DD68C`                    |
| `--warn`             | `#FFB020`                    |
| `--error`            | `#F87171`                    |
| `--surface`          | `#0F1116`                    |
| `--surface-container`| `#1A1D23`                    |
| `--surface-variant`  | `#252931`                    |
| `--on-surface`       | `#FFFFFF`                    |
| `--on-surface-variant`| `#94A3B8`                   |
| `--outline`          | `#334155`                    |
| `--outline-var`      | `#1E293B`                    |
| `--sub`              | `rgba(255,255,255,.05)`      |
| `--brd`              | `rgba(255,255,255,.08)`      |
| `--brd-em`           | `rgba(255,255,255,.16)`      |

### Semantic colors (v4 additions)

| Что               | Цвет                                            |
| ----------------- | ----------------------------------------------- |
| Тип «аниме»       | `#0095FF` glow `rgba(0,149,255,.55)`            |
| Тип «манга»       | `#3DD68C` glow `rgba(61,214,140,.5)`            |
| Тип «ранобэ»      | `#A78BFA` glow `rgba(167,139,250,.55)`          |
| Сердечко донат    | `#FF6B8A` glow `rgba(255,107,138,.55)`          |
| Welcome halo pink | `rgba(255,150,190,.35)` радиальный              |
| Welcome bubble bg | `linear-gradient(145deg, #FFFFFF, #F4EBFF)`     |

### Radii

| Токен       | Значение    | Использование                    |
| ----------- | ----------- | -------------------------------- |
| `--r-s`     | 8px         | чипы, tags                       |
| `--r-m`     | 12px        | карточки, поля, кнопки-rect      |
| `--r-l`     | 20px        | sheet сверху, hero               |
| `--r-full`  | 999px       | pills, avatars, btns             |

### Motion

| Токен        | Значение                            |
| ------------ | ----------------------------------- |
| `--d-fast`   | 140ms                               |
| `--d-mid`    | 240ms                               |
| `--d-slow`   | 420ms                               |
| `--ease`     | `cubic-bezier(.2,.8,.2,1)`          |
| `--ease-out` | `cubic-bezier(.16,1,.3,1)`          |
| bounce/pop   | `cubic-bezier(.34,1.56,.64,1)`      |

### Typography

- **Sans**: `system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans JP", sans-serif`
- **Display** (заголовки, splash, welcome): `"Zen Kaku Gothic Antique", system-ui, sans-serif`, weight 700–900
- **Mono** (лог AI): `"JetBrains Mono", ui-monospace, monospace`
- Числовые счётчики: `font-variant-numeric: tabular-nums`
- Основные размеры: 10 / 11 / 12 / 13 / 15 / 17 / 20 / 22 / 30 / 44

### Shadows / glow

| Что                       | Значение                                                                                                            |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| FAB base                  | `0 0 0 6px var(--surface-container), 0 8px 24px rgba(91,59,232,.55), 0 0 22px rgba(124,92,255,.38)`                 |
| FAB pulse peak            | `0 0 0 6px …, 0 12px 40px rgba(91,59,232,.9), 0 0 50px rgba(124,92,255,.85)`                                        |
| Primary CTA glow          | `0 8px 24px rgba(91,59,232,.5), 0 0 24px rgba(124,92,255,.35)`                                                      |
| AI indicator hover        | `0 0 0 3px rgba(124,92,255,.14), 0 4px 12px rgba(91,59,232,.35)`                                                    |
| Welcome girl              | `drop-shadow(0 8px 40px rgba(124,92,255,.55)) drop-shadow(0 0 24px rgba(167,139,250,.35))`                          |
| Welcome bubble            | `0 8px 24px rgba(0,0,0,.35), 0 0 24px rgba(124,92,255,.35)`                                                         |
| Welcome logo peak         | `0 24px 80px rgba(91,59,232,.85), 0 0 80px rgba(124,92,255,.8)`                                                     |

---

## Icons Set

Все иконки — inline SVG (stroke 1.8, round). Файл `kit/Icons.jsx`, компонент `<Icon name="…" />`, path хранится в объекте `ICON_PATHS`.

**Основа UI**: `home`, `library`, `calendar`, `more`, `settings`, `menu`, `back`, `search`, `plus`, `refresh`, `download`, `chevron`, `arrowL`, `arrowR`, `info`, `dot`, `alert`, `check`, `close`, `edit`, `copy`, `paste`, `filter`.

**Медиа**: `play`, `pause`, `prev`, `next`, `volume`, `music`, `headphones`, `book`, `bookOpen`, `bookmark`.

**Спец. v4**: `heart`, `brain` (AI-индикатор), `bell`, `folder`, `battery`, `shield`, `doc`, `eye`, `eyeOff`, `spark`, `spark2`, `spark4`, `swipes` (FAB, стрелки-круг), `news`, `clock`, `chart`, `compass`, `wallet`, `qr`, `usb`, `users`, `send`, `external`, `github`, `telegram`.

При переходе на Compose — использовать `Icon(painterResource(R.drawable.ic_…), contentDescription = ...)`. Все пути можно сконвертировать в `<vector>` XML для Android.

---

## Assets

- **`assets/welcome-girl.png`** — аниме-иллюстрация девушки в форме (701×1024, PNG с прозрачным фоном, 798 КБ). Используется на welcome-экране онбординга. **В проде — заменить на арт официального маскота Takami** или на другую подходящую иллюстрацию (без нарушения авторских прав). Требования к артворку:
  - Соотношение сторон около 3:4.
  - Прозрачный фон.
  - Композиция «по грудь / по пояс», взгляд в камеру.
  - Позитивный жест (peace, wave) — визуально считывается «приветствие».
  - Стиль совместим с dark-темой (не слишком светлый).
- `assets/logo.jpg` — иконка приложения (56×56 или больше), используется в profile-avatar настроек и About-блоке. Также — источник для brand-plate в шапке kit.
- `assets/logo.svg`, `assets/logo-wordmark.svg` — SVG-варианты логотипа.
- Обложки тайтлов в прототипе — CSS-градиенты (`fr.bg`). В проде заменяются на реальные обложки (2:3, JPEG/WebP).
- Персонажи, сэйю — генерируются из инициала имени + палитра. В проде — реальные постеры с CDN.

Все иконки — inline SVG (без внешних файлов), веса — 0 KB.

Шрифты (Google Fonts):
- `Zen Kaku Gothic Antique` (400/500/700/900) — display + welcome-иероглифы
- `Noto Sans JP` (400/500/700/900) — fallback для JP
- `Zen Maru Gothic` (500/700/900) — резерв
- `JetBrains Mono` (400/500) — моно (AI log)

---

## Files

Файлы в этом handoff-пакете (все — эталонные референсы для реализации, не production-код):

**Основа**:
- `Takami UI Kit.html` — точка входа. Собирает всё через `<script type="text/babel">`, монтирует `<TakamiApp />` в `#root`. Управляет onboarding-state, screen-navigation, transitions.
- `colors_and_type.css` — все Aurora-токены (цвета, spacing, motion). **Копировать в native as-is** (в Compose — превратить в `ThemeExtension` / `MaterialTheme` extension).

**Стили компонентов** (сплит для скорости в браузере, в проде — единая тема):
- `kit/kit-1.css` — базовые компоненты, FAB, phone-frame, calendar-strip.
- `kit/kit-2.css` — Title, Character, Reader, Player.
- `kit/kit-3.css` — Search, Sources, Swipes, Settings, Home.
- `kit/patches.css` — **все v4/v4.1 изменения**: AI-indicator, FAB v4 (spinner), calendar dots by type, support-button, screen stagger, onboarding (включая полный welcome-scene с девушкой). **Смотреть в первую очередь для новых фич.**

**React-компоненты**:
- `kit/Icons.jsx` — SVG icon set (см. Icons Set выше).
- `kit/Phone.jsx` — рамка телефона, статус-бар, `TabBar` (с FAB-loading), `AppBar`.
- `kit/Home.jsx` — главная (обновлена: SVG-иконки, AI-индикатор в шапке).
- `kit/Library.jsx`, `kit/Title.jsx`, `kit/Character.jsx`, `kit/Reader.jsx`, `kit/Player.jsx`, `kit/Search.jsx`, `kit/AdvancedSearch.jsx`, `kit/Sources.jsx`, `kit/Swipes.jsx`, `kit/Calendar.jsx` (обновлён: цветные точки), `kit/Settings.jsx` (обновлён: убран Donate CTA, добавлен SupportButton, все action-rows работают), `kit/Proxy.jsx`.
- `kit/AiIndicator.jsx` — новый (v4).
- `kit/Onboarding.jsx` — новый (v4), обновлён в v4.1: welcome с картинкой, CTA-кнопка, убран автопереход.
- `kit/SupportButton.jsx` — новый (v4).

**Данные (моки)**:
- `kit/data.js` — DB тайтлов, персонажей, сэйю, новостей, источников.
- `kit/ai-store.js` — локальное хранилище AI-ключа (провайдер + модель).

**Ассеты**:
- `assets/welcome-girl.png` — welcome-иллюстрация (см. Assets выше).
- `assets/logo.jpg`, `assets/logo.svg` — брендовые.

---

## Implementation Notes

1. **Prototype → Compose mapping**:
   - `React.useState` → `remember { mutableStateOf(...) }`
   - CSS class с bg/border/radius → `Modifier.background(...).border(...).clip(RoundedCornerShape(...))`
   - `.stagger` animation → `AnimatedVisibility` в комбинации с delay per index в `LazyColumn`.
   - CSS `backdrop-filter: blur` → `Modifier.blur(...)` + tinted overlay (Compose 1.5+).

2. **localStorage** → `DataStore<Preferences>` под ключами `onboarded: Boolean`, `lastScreen: String`.

3. **FAB pulse animation** — в Compose:
   ```
   val pulse by rememberInfiniteTransition().animateFloat(
     initialValue = 1f, targetValue = 1.08f,
     animationSpec = infiniteRepeatable(
       animation = tween(550, easing = EaseInOut),
       repeatMode = RepeatMode.Reverse
     )
   )
   FloatingActionButton(
     modifier = Modifier.scale(if (loading) pulse else 1f),
     ...
   )
   ```

4. **AI-индикатор мини-график** — в Compose проще всего через `Row` из 5 `Box` с `Modifier.height(animatedHeight).background(...)`, каждый со своим `InfiniteTransition` и `initialStartOffset` для stagger.

5. **Welcome girl «оживление»** — в Compose:
   - Контейнер: `Modifier.graphicsLayer { scaleX = breath; scaleY = breath; translationY = breathY }` где breath/breathY анимируются через `rememberInfiniteTransition`.
   - Image inside: `Modifier.graphicsLayer { rotationZ = swayRotation; transformOrigin = TransformOrigin(0.5f, 1f) }`.
   - Halo и sparkles — `Canvas` или отдельные `Box` с `Modifier.blur` и `Modifier.alpha(animatedAlpha)`.

6. **Онбординг** должен быть отдельным Activity или Screen перед `MainScreen`, с navigation-графом: `splash → policy → perms → welcome → main`.

7. **AutoRestart onboarding** — в прототипе floating `reset onboarding` внизу справа для тестирования; в проде убрать или спрятать в раздел «Разработчику».

---

## What's NOT in this handoff

- **Реальные обложки тайтлов и портреты персонажей** — используются градиенты-заглушки.
- **Реальный backend автопарсера** — только UI. Данные `pct`, `stats`, `log` — мокапы.
- **Реальные диплинки** на Boosty/Tribute/Patreon — заменить на актуальные URL при подключении.
- **Native icon assets** (`R.drawable.*`) — все иконки в прототипе inline SVG; в проде нужно сконвертировать в `<vector>` XML.
- **Финальная welcome-иллюстрация** — сейчас placeholder-арт; проработать официального маскота бренда.
- **Реальный анимированный маскот** — можно использовать Lottie / Rive для более выразительного «оживления» персонажа (моргания, наклоны головы, смена выражений), если бюджет позволяет.

---

Если что-то в этом документе не совпадает с прототипом — **прототип истина**. Открывайте `Takami UI Kit.html` в браузере (или через `python -m http.server` из корня пакета) и смотрите на живой пример.
