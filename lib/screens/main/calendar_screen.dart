// Calendar — портирована из kit/Calendar.jsx
// Цветные точки по типу контента: 🔵 аниме, 🟢 манга, 🟣 ранобэ
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class _Schedule {
  final int wd, hour, min, every, num;
  final String status; // ongoing | finished | hiatus
  final bool late;
  _Schedule({
    required this.wd,
    required this.hour,
    required this.min,
    required this.every,
    required this.status,
    required this.num,
    required this.late,
  });
}

class CalendarScreen extends StatefulWidget {
  final void Function(TitleItem) onOpenTitle;
  const CalendarScreen({super.key, required this.onOpenTitle});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  String filter = 'all';
  bool onlyMine = false;
  late List<DateTime> strip;
  late DateTime selected;

  int _hash(String s) {
    int h = 2166136261;
    for (final c in s.codeUnits) {
      h ^= c;
      h = (h * 16777619) & 0xFFFFFFFF;
    }
    return h & 0xFFFFFFFF;
  }

  double Function() _seeded(int sd) {
    int x = sd == 0 ? 1 : sd;
    return () {
      x ^= (x << 13) & 0xFFFFFFFF;
      x ^= (x >> 17);
      x ^= (x << 5) & 0xFFFFFFFF;
      x &= 0xFFFFFFFF;
      return x / 4294967296;
    };
  }

  _Schedule _scheduleOf(TitleItem item) {
    final r = _seeded(_hash(item.id.toString()) + 31);
    final st = r();
    final status = st < 0.12
        ? 'finished'
        : st < 0.24
        ? 'hiatus'
        : 'ongoing';
    final wd = (r() * 7).floor();
    final hour = 10 + (r() * 12).floor();
    final min = r() < 0.5 ? 0 : 30;
    final every = r() < 0.78 ? 7 : 14;
    final num = 5 + (r() * 120).floor();
    final late = r() < 0.18;
    return _Schedule(
      wd: wd,
      hour: hour,
      min: min,
      every: every,
      status: status,
      num: num,
      late: late,
    );
  }

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    final mon = DateTime(
      now.year,
      now.month,
      now.day,
    ).subtract(Duration(days: (now.weekday - 1) % 7));
    strip = List.generate(14, (i) => mon.add(Duration(days: i)));
    selected = DateTime(now.year, now.month, now.day);
  }

  List<TitleItem> get _filteredItems {
    final map = {
      'manga': ContentType.manga,
      'anime': ContentType.anime,
      'novel': ContentType.novel,
    };
    if (filter == 'all') return TakamiDB.items;
    return TakamiDB.items.where((x) => x.type == map[filter]).toList();
  }

  List<(TitleItem, _Schedule, DateTime, int)> _releasesFor(DateTime day) {
    final out = <(TitleItem, _Schedule, DateTime, int)>[];
    for (final it in _filteredItems) {
      final s = _scheduleOf(it);
      if (s.status != 'ongoing') continue;
      if (day.weekday % 7 != s.wd) continue;
      final time = DateTime(day.year, day.month, day.day, s.hour, s.min);
      out.add((it, s, time, s.num));
    }
    out.sort((a, b) => a.$3.compareTo(b.$3));
    return out;
  }

  List<ContentType> _typesFor(DateTime day) {
    final rels = _releasesFor(day);
    final set = <ContentType>{};
    for (final r in rels) {
      set.add(r.$1.type);
    }
    return [
      ContentType.anime,
      ContentType.manga,
      ContentType.novel,
    ].where(set.contains).toList();
  }

  (TitleItem, _Schedule, DateTime, int)? get _nearest {
    final now = DateTime.now();
    (TitleItem, _Schedule, DateTime, int)? best;
    for (int d = 0; d < 14; d++) {
      final day = strip[0].add(Duration(days: d));
      final list = _releasesFor(day);
      for (final r in list) {
        if (r.$3.isAfter(now) && (best == null || r.$3.isBefore(best.$3))) {
          best = r;
        }
      }
    }
    return best;
  }

  String _timeLeft(DateTime ts) {
    final m = ts.difference(DateTime.now()).inMinutes;
    if (m < 60) return '$m мин';
    final h = m ~/ 60;
    if (h < 24) return '$h ч';
    return '${h ~/ 24} дн';
  }

  String _stateOf(DateTime ts, _Schedule s) {
    final now = DateTime.now();
    if (ts.isAfter(now)) return 'soon';
    if (s.late && now.difference(ts).inDays < 3) return 'late';
    return 'out';
  }

  Color _dotColor(ContentType t) => t.color;

  void _toast(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    final nearest = _nearest;
    final dayReleases = _releasesFor(selected);
    final paused = _filteredItems
        .map((it) => (it, _scheduleOf(it)))
        .where((x) => x.$2.status != 'ongoing')
        .toList();

    const monthNames = [
      'января',
      'февраля',
      'марта',
      'апреля',
      'мая',
      'июня',
      'июля',
      'августа',
      'сентября',
      'октября',
      'ноября',
      'декабря',
    ];
    const weekdays = [
      'понедельник',
      'вторник',
      'среда',
      'четверг',
      'пятница',
      'суббота',
      'воскресенье',
    ];
    const wdShort = ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'];

    return Column(
      children: [
        TakamiAppBar(
          title: 'Календарь',
          actions: [
            TakamiAction(
              icon: TIcon.refresh,
              onClick: () => _toast('Расписание обновлено · +2 релиза'),
            ),
            TakamiAction(
              icon: TIcon.menu,
              onClick: () =>
                  _toast('Показать только: моя библиотека / все источники'),
            ),
          ],
        ),
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (nearest != null)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
                    child: GestureDetector(
                      onTap: () => widget.onOpenTitle(nearest.$1),
                      child: Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AuroraColors.sub,
                          border: Border.all(color: AuroraColors.brd),
                          borderRadius: BorderRadius.circular(AuroraRadii.m),
                        ),
                        child: Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Container(
                                width: 48,
                                height: 48,
                                decoration: BoxDecoration(
                                  gradient: TakamiDB.franchiseOf(
                                    nearest.$1.id,
                                  ).bg,
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text(
                                    'Ближайший релиз',
                                    style: TextStyle(
                                      fontSize: 10,
                                      color: AuroraColors.acc2,
                                    ),
                                  ),
                                  Text(
                                    nearest.$1.title,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white,
                                    ),
                                  ),
                                  Text(
                                    '${nearest.$1.type == ContentType.anime ? "Эпизод" : "Глава"} ${nearest.$4} · через ${_timeLeft(nearest.$3)}',
                                    style: const TextStyle(
                                      fontSize: 11,
                                      color: AuroraColors.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Text(
                              '${nearest.$3.hour.toString().padLeft(2, '0')}:${nearest.$3.minute.toString().padLeft(2, '0')}',
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Row(
                    children: [
                      FilterChip2(
                        label: 'Всё',
                        selected: filter == 'all',
                        onTap: () => setState(() => filter = 'all'),
                      ),
                      FilterChip2(
                        label: 'Манга',
                        selected: filter == 'manga',
                        onTap: () => setState(() => filter = 'manga'),
                      ),
                      FilterChip2(
                        label: 'Аниме',
                        selected: filter == 'anime',
                        onTap: () => setState(() => filter = 'anime'),
                      ),
                      FilterChip2(
                        label: 'Ранобэ',
                        selected: filter == 'novel',
                        onTap: () => setState(() => filter = 'novel'),
                      ),
                      FilterChip2(
                        label: 'Только моё',
                        selected: onlyMine,
                        onTap: () => setState(() => onlyMine = !onlyMine),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 4),
                  child: Row(
                    children: [
                      _legendChip(AuroraColors.typeAnime, 'аниме'),
                      const SizedBox(width: 12),
                      _legendChip(AuroraColors.typeManga, 'манга'),
                      const SizedBox(width: 12),
                      _legendChip(AuroraColors.typeNovel, 'ранобэ'),
                    ],
                  ),
                ),
                SizedBox(
                  height: 64,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    itemCount: strip.length,
                    itemBuilder: (context, i) {
                      final d = strip[i];
                      final isSel =
                          d.year == selected.year &&
                          d.month == selected.month &&
                          d.day == selected.day;
                      final today = DateTime.now();
                      final isToday =
                          d.year == today.year &&
                          d.month == today.month &&
                          d.day == today.day;
                      final types = _typesFor(d);
                      return GestureDetector(
                        onTap: () => setState(() => selected = d),
                        child: Container(
                          width: 40,
                          margin: const EdgeInsets.only(right: 6),
                          padding: const EdgeInsets.symmetric(vertical: 8),
                          decoration: BoxDecoration(
                            color: isSel
                                ? AuroraColors.acc
                                : (isToday
                                      ? AuroraColors.sub
                                      : Colors.transparent),
                            border: isToday && !isSel
                                ? Border.all(
                                    color: AuroraColors.acc.withValues(
                                      alpha: 0.5,
                                    ),
                                  )
                                : null,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(
                                wdShort[d.weekday - 1],
                                style: TextStyle(
                                  fontSize: 9.5,
                                  color: isSel
                                      ? Colors.white70
                                      : AuroraColors.onSurfaceVariant,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                '${d.day}',
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w700,
                                  color: isSel ? Colors.white : Colors.white,
                                ),
                              ),
                              const SizedBox(height: 4),
                              if (types.isNotEmpty)
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: types
                                      .map(
                                        (t) => Container(
                                          width: 5,
                                          height: 5,
                                          margin: const EdgeInsets.symmetric(
                                            horizontal: 1,
                                          ),
                                          decoration: BoxDecoration(
                                            shape: BoxShape.circle,
                                            color: isSel
                                                ? Colors.white
                                                : _dotColor(t),
                                            boxShadow: isSel
                                                ? null
                                                : [
                                                    BoxShadow(
                                                      color: _dotColor(
                                                        t,
                                                      ).withValues(alpha: 0.6),
                                                      blurRadius: 4,
                                                    ),
                                                  ],
                                          ),
                                        ),
                                      )
                                      .toList(),
                                )
                              else
                                Container(
                                  width: 4,
                                  height: 4,
                                  decoration: const BoxDecoration(
                                    shape: BoxShape.circle,
                                    color: Colors.white12,
                                  ),
                                ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  child: Row(
                    children: [
                      Text(
                        '${weekdays[selected.weekday - 1]}, ${selected.day} ${monthNames[selected.month - 1]}',
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: Colors.white,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        '${dayReleases.length}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AuroraColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                if (dayReleases.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 20,
                    ),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: AuroraColors.sub,
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: const Column(
                        children: [
                          Text(
                            'В этот день релизов нет',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            ),
                          ),
                          SizedBox(height: 4),
                          Text(
                            'Расписание берётся из источника и может сдвигаться',
                            style: TextStyle(
                              fontSize: 11,
                              color: AuroraColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ...dayReleases.map((r) {
                  final st = _stateOf(r.$3, r.$2);
                  final stLabel = {
                    'out': 'вышло',
                    'late': 'задержка',
                    'soon': 'ожидается',
                  }[st]!;
                  final stColor = {
                    'out': AuroraColors.onSurfaceVariant,
                    'late': AuroraColors.warn,
                    'soon': AuroraColors.acc2,
                  }[st]!;
                  return Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 4,
                    ),
                    child: GestureDetector(
                      onTap: () => widget.onOpenTitle(r.$1),
                      child: Row(
                        children: [
                          SizedBox(
                            width: 44,
                            child: Text(
                              '${r.$3.hour.toString().padLeft(2, '0')}:${r.$3.minute.toString().padLeft(2, '0')}',
                              style: const TextStyle(
                                fontSize: 11,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                          ),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Container(
                              width: 40,
                              height: 40,
                              decoration: BoxDecoration(
                                gradient: TakamiDB.franchiseOf(r.$1.id).bg,
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  r.$1.title,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontSize: 12.5,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.white,
                                  ),
                                ),
                                Text(
                                  '${r.$1.type == ContentType.anime ? "Эпизод" : "Глава"} ${r.$4} · ${r.$2.every == 14 ? "раз в 2 недели" : "еженедельно"}',
                                  style: const TextStyle(
                                    fontSize: 10.5,
                                    color: AuroraColors.onSurfaceVariant,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: stColor.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              stLabel,
                              style: TextStyle(
                                fontSize: 10,
                                color: stColor,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
                if (paused.isNotEmpty) ...[
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
                    child: Row(
                      children: [
                        const Text(
                          'Без расписания',
                          style: TextStyle(fontSize: 12.5, color: Colors.white),
                        ),
                        const Spacer(),
                        Text(
                          '${paused.length}',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AuroraColors.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  ...paused.map((x) {
                    final (it, s) = x;
                    return Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 4,
                      ),
                      child: GestureDetector(
                        onTap: () => widget.onOpenTitle(it),
                        child: Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Container(
                                width: 40,
                                height: 40,
                                decoration: BoxDecoration(
                                  gradient: TakamiDB.franchiseOf(it.id).bg,
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    it.title,
                                    style: const TextStyle(
                                      fontSize: 12.5,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white,
                                    ),
                                  ),
                                  Text(
                                    s.status == 'hiatus'
                                        ? 'Хиатус — выпуск приостановлен'
                                        : 'Завершён · ${s.num} всего',
                                    style: TextStyle(
                                      fontSize: 10.5,
                                      color: s.status == 'hiatus'
                                          ? AuroraColors.warn
                                          : AuroraColors.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  }),
                ],
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                  child: Text(
                    'Даты расчётные: строятся по среднему интервалу выпусков источника. Точное время публикации сайты почти никогда не отдают.',
                    style: TextStyle(
                      fontSize: 10.5,
                      color: Colors.white.withValues(alpha: 0.35),
                      height: 1.4,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _legendChip(Color color, String label) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(shape: BoxShape.circle, color: color),
        ),
        const SizedBox(width: 5),
        Text(
          label,
          style: const TextStyle(
            fontSize: 10.5,
            color: AuroraColors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
