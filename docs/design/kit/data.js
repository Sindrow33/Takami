// Клон window.DB из docs/prototype/data.js (Sindrow33/Takami)
window.DB = {
  typeName: { manga: "Манга", anime: "Аниме", novel: "Ранобэ" },
  short: { manga: "М", anime: "А", novel: "Р" },
  items: [
    { id: 1, type: "manga", t: "Тайтл с длинным названием", src: "MangaHub", y: 2023, r: 8.7,
      count: "120 глав", prog: 35, sub: "Гл. 42 из 120", badge: "12",
      srcUrl: "https://mangahub.io/manga/aogana-kenshin",
      bg: "linear-gradient(150deg,#3B2A6B,#141821)" },
    { id: 2, type: "anime", t: "Аниме сериал", src: "AniLibria", y: 2026, r: 8.1,
      count: "12 эп.", prog: 60, sub: "Эп. 7 · 12:30", badge: "NEW",
      srcUrl: "https://anilibria.tv/release/aogana-kenshin",
      bg: "linear-gradient(150deg,#123A4B,#141821)" },
    { id: 3, type: "novel", t: "Ранобэ, том 3", src: "RanobeLib", y: 2021, r: 7.9,
      count: "210 глав", prog: 43, sub: "Гл. 5 · 43%", badge: "off",
      srcUrl: "https://ranobelib.me/aogana-kenshin",
      bg: "linear-gradient(150deg,#4B2740,#141821)" },
    { id: 4, type: "manga", t: "Источник недоступен", src: "AnimeGo", y: 2020, r: 0,
      count: "—", prog: 0, sub: "Ошибка загрузки", badge: "err", broken: 1,
      srcUrl: "https://animego.org/anime/broken-title-1234",
      bg: "linear-gradient(150deg,#4A1F1F,#141821)" },
    { id: 5, type: "manga", t: "Ещё один тайтл", src: "MangaDex", y: 2019, r: 9.1,
      count: "88 глав", prog: 12, sub: "Гл. 1 из 8", badge: "",
      srcUrl: "https://mangadex.org/title/8d1f-shirokumo-legend",
      bg: "linear-gradient(150deg,#1F4636,#141821)" },
    { id: 6, type: "anime", t: "Новинка сезона", src: "AnimeGo", y: 2026, r: 0,
      count: "выходит", prog: 100, sub: "Завершено", badge: "",
      srcUrl: "https://animego.org/anime/season-newcomer-2026",
      bg: "linear-gradient(150deg,#4A3A16,#141821)" }
  ],
  fr: {
    A: { t: "Тайтл с длинным названием", bg: "linear-gradient(150deg,#3B2A6B,#141821)", items: [1,2,3],
         g: ["Экшен","Фэнтези","Сёнэн"],
         d: "Одна история в трёх форматах: манга-первоисточник, аниме-экранизация и ранобэ. Описание берётся у франшизы, а главы и прогресс — у выбранного формата." },
    B: { t: "Источник недоступен", bg: "linear-gradient(150deg,#4A1F1F,#141821)", items: [4],
         g: ["Хоррор"], d: "Парсер сломан, показаны сохранённые данные." },
    C: { t: "Ещё один тайтл", bg: "linear-gradient(150deg,#1F4636,#141821)", items: [5],
         g: ["Психология","Сэйнэн"], d: "Завершённая история с полным переводом." },
    D: { t: "Новинка сезона", bg: "linear-gradient(150deg,#4A3A16,#141821)", items: [6],
         g: ["Комедия"], d: "Премьера на этой неделе, оценок пока нет." }
  },
  fidOf: { 1: "A", 2: "A", 3: "A", 4: "B", 5: "C", 6: "D" },
  chars: [
    { id: 1, n: "Имя Персонажа", jp: "キャラクター名", role: "Главный", main: 1,
      age: 17, height: "172 см", birthday: "14 марта", zodiac: "Рыбы", bloodType: "0",
      affiliation: "Академия «Аогана»", origin: "Провинция Мисимо",
      bio: "Молодой мечник с наследственным даром слышать «голос клинка». В детстве потерял старшего брата и с тех пор ищет ответ, зачем меч выбирает своего хозяина. Внешне лёгкий, внутри — упрямый до одержимости.",
      quotes: ["«Если клинок молчит — значит, ты его ещё не услышал.»", "«Быть первым — не про быстрее. Про то, чтобы не отвести взгляд.»"],
      seiyuu: 1, appearsIn: [1, 2, 3], mainIn: 1
    },
    { id: 2, n: "Второй Персонаж", jp: "二番目", role: "Главный", main: 1,
      age: 18, height: "165 см", birthday: "2 августа", zodiac: "Лев", bloodType: "AB",
      affiliation: "Академия «Аогана» · клан Ясуги", origin: "Столица Кэйра",
      bio: "Отличница и стратег. За доской и на поле боя видит на два шага дальше остальных, но плохо переносит проигрыш даже в мелочах. Считает главного героя раздражающим, но именно ему доверяет спину.",
      quotes: ["«Хорошая тактика — это когда противник ещё не понял, что уже проиграл.»"],
      seiyuu: 2, appearsIn: [1, 2, 3], mainIn: 1
    },
    { id: 3, n: "Антагонист", jp: "敵役", role: "Главный", main: 1,
      age: 24, height: "188 см", birthday: "неизвестно", zodiac: "—", bloodType: "—",
      affiliation: "Орден «Пепельная нить»",
      bio: "Бывший наставник героя, ушедший в тень. Хочет не победы, а точки: закрыть цикл, который тянется поколениями. Отсюда его страшная выдержка и почти скучная методичность.",
      quotes: ["«Я не злодей. Я — итог того, во что вы отказались верить.»"],
      seiyuu: 3, appearsIn: [1, 2], mainIn: 1
    },
    { id: 4, n: "Третий Персонаж", jp: "三番目", role: "Второстеп.", main: 0,
      age: 16, height: "158 см", birthday: "11 декабря", zodiac: "Стрелец", bloodType: "B",
      affiliation: "Академия «Аогана»", origin: "Портовый город Юнами",
      bio: "Механик и изобретатель. Разговаривает с машинами больше, чем с людьми, и, судя по всему, они ей отвечают взаимностью.",
      quotes: ["«Люди ломаются от чувств. Механизмы — только от усталости металла.»"],
      seiyuu: 4, appearsIn: [1, 2], mainIn: 0
    },
    { id: 5, n: "Наставник", jp: "師匠", role: "Второстеп.", main: 0,
      age: 62, height: "170 см", birthday: "неизвестно", zodiac: "—", bloodType: "A",
      affiliation: "Хранитель храма Сирокумо",
      bio: "Молчаливый старик, у которого учились все, кто хоть что-то умеет. Верит, что настоящий бой начинается там, где кончаются приёмы. Пьёт зелёный чай и никогда не отвечает прямо.",
      quotes: ["«Ты бьёшь как ребёнок, у которого забрали игрушку. Я жду, когда ты забудешь про игрушку.»"],
      seiyuu: 5, appearsIn: [1, 2, 3], mainIn: 0
    },
    { id: 6, n: "Эпизодический", jp: "端役", role: "Эпизод", main: 0,
      age: null, height: "—", birthday: "—", zodiac: "—", bloodType: "—",
      affiliation: "Городская стража Кэйры",
      bio: "Стражник у ворот. Появляется во второй арке, произносит две фразы и уходит на пенсию. Фанаты уверены, что он видел больше, чем говорит.",
      quotes: ["«Проходите, только не бегом.»"],
      seiyuu: 6, appearsIn: [2], mainIn: 0
    }
  ],
  seiyuu: [
    { id: 1, n: "Юки Кадзи",       jp: "梶 裕貴",         y: 1985, roles: 214, note: "«Атака титанов», «Ад Данте»" },
    { id: 2, n: "Каори Исихара",    jp: "石原 夏織",       y: 1993, roles: 96,  note: "«Такаги-сан», «Принцесса-медуза»" },
    { id: 3, n: "Дайсукэ Оно",      jp: "小野 大輔",       y: 1978, roles: 187, note: "«Kuroshitsuji», «Baccano!»" },
    { id: 4, n: "Аой Юки",          jp: "悠木 碧",         y: 1992, roles: 138, note: "«Madoka Magica»" },
    { id: 5, n: "Такахиро Сакурай", jp: "櫻井 孝宏",       y: 1974, roles: 240, note: "«Fate/Zero», «Code Geass»" },
    { id: 6, n: "Дзюн Фукуяма",     jp: "福山 潤",         y: 1978, roles: 178, note: "«Code Geass», «No Game No Life»" }
  ],
  // Все жанры для расширенного поиска
  genres: [
    "Экшен","Приключения","Комедия","Драма","Фэнтези","Мистика","Ужасы",
    "Романтика","Психология","Сёнэн","Сэйнэн","Сёдзё","Слайс","Спорт",
    "Меха","Меха-пилоты","Историческое","Детектив","Триллер","Магия",
    "Тёмное фэнтези","Школа","Постапокалипсис","Кулинария","Музыка"
  ],
  studios: [
    "MAPPA","ufotable","Studio Ghibli","Kyoto Animation","Bones","Wit Studio",
    "Trigger","Studio Kagura","Studio Tenshi","Madhouse","A-1 Pictures"
  ],
  ageRatings: ["G — все","PG-12","R15+","R17+","R18+"],
  countries: ["Япония","Корея","Китай","США","Россия"],
  // Донат — тиры + прогресс месяца
  donate: {
    goal: 800, raised: 542, currency: "$",
    supporters: 148,
    tiers: [
      { key: "bronze", n: "Bronze",   price: 3,  color: "#C97A2E", perks: ["Бейдж «Читатель»", "Спасибо в титрах", "Ранний доступ к бетам"] },
      { key: "silver", n: "Silver",   price: 7,  color: "#94A3B8", popular: true,
        perks: ["Всё из Bronze","Именной ник в приложении","Доступ к закрытому Telegram-чату","Ранние ридер/плеер фичи"] },
      { key: "gold",   n: "Gold",     price: 15, color: "#FFC24A",
        perks: ["Всё из Silver","Заявка на источник в приоритет","Голосование за фичи","Аватар с рамкой Aurora"] }
    ],
    methods: [
      { k: "boosty",   n: "Boosty",         sub: "Ежемесячно", ic: "B" },
      { k: "dalerts",  n: "DonationAlerts", sub: "Разовый перевод", ic: "◇" },
      { k: "crypto",   n: "Криптовалюта",   sub: "USDT · TON · BTC · ETH", ic: "₿" },
      { k: "yookassa", n: "ЮKassa",         sub: "Российские карты · СБП", ic: "Ю" }
    ],
    thanks: [
      "Артём К.", "Anon", "Читатель №142", "Мия · Silver", "Дэн", "Kate",
      "Noname · Gold", "Ilya", "Аноним", "Соня А.", "Юра", "Гость"
    ]
  },
  // Прокси / VPN
  proxyServers: [
    { id: 1, n: "Amsterdam · NL", type: "WireGuard", host: "nl1.aurora.vpn", ping: 42, active: 1, kind: "pinned" },
    { id: 2, n: "Frankfurt · DE",  type: "WireGuard", host: "de3.aurora.vpn", ping: 58, active: 0, kind: "pinned" },
    { id: 3, n: "Helsinki · FI",   type: "OpenVPN",   host: "fi.aurora.vpn",  ping: 71, active: 0, kind: "sub" },
    { id: 4, n: "Vilnius · LT",    type: "SOCKS5",    host: "lt.proxy.io:1080", ping: 96, active: 0, kind: "manual" },
    { id: 5, n: "Warsaw · PL",     type: "HTTP",      host: "pl.proxy.io:8080", ping: 121, active: 0, kind: "manual" }
  ],
  // Новости аниме — карусель на главной
  news: [
    {
      id: 'n1',
      cat: 'Индустрия',
      t: 'MAPPA анонсировала финальный сезон',
      sub: 'Премьера — весна 2027, 24 эпизода одним куском',
      tm: '2 ч назад',
      src: 'Anime News Network',
      bg: 'linear-gradient(150deg,#3B2A6B,#141821)',
      tone: 'primary'
    },
    {
      id: 'n2',
      cat: 'Трейлер',
      t: 'Первый ролик экранизации ранобэ',
      sub: 'ufotable показала 90 секунд боёвки и опенинг',
      tm: '5 ч назад',
      src: 'YouTube · ufotable',
      bg: 'linear-gradient(150deg,#123A4B,#141821)',
      tone: 'cyan'
    },
    {
      id: 'n3',
      cat: 'Манга',
      t: 'Автор уходит в перерыв на 3 месяца',
      sub: 'После 12-й арки — плановая пауза, чтобы «не сгореть»',
      tm: 'вчера',
      src: 'Weekly Shonen Jump',
      bg: 'linear-gradient(150deg,#4B2740,#141821)',
      tone: 'warn'
    },
    {
      id: 'n4',
      cat: 'Релиз',
      t: 'На AniLibria обновлена озвучка',
      sub: 'Полная переозвучка первого сезона, +40 % битрейта',
      tm: '2 дня назад',
      src: 'AniLibria',
      bg: 'linear-gradient(150deg,#1F4636,#141821)',
      tone: 'ok'
    }
  ],
  sources: [
    ["MangaHub", "Манга", "1.4.2", 1, "ok"],
    ["ReadManga", "Манга", "2.0.1", 1, "warn"],
    ["MangaDex", "Манга", "1.9.0", 1, "ok"],
    ["AniLibria", "Аниме", "3.1.0", 1, "ok"],
    ["AnimeGo", "Аниме", "1.2.5", 0, "err"],
    ["RanobeLib", "Ранобэ", "1.0.8", 1, "ok"]
  ]
};
