// Home — главная. Портирована из kit/Home.jsx
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../state/app_state.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/ai_indicator.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class HomeScreen extends StatefulWidget {
  final void Function(AppScreen) onGo;
  final void Function(TitleItem) onOpenTitle;

  const HomeScreen({super.key, required this.onGo, required this.onOpenTitle});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _newsIdx = 0;
  final _newsController = ScrollController();

  TIcon _typeIcon(ContentType t) => switch (t) {
    ContentType.anime => TIcon.play,
    ContentType.novel => TIcon.bookOpen,
    ContentType.manga => TIcon.book,
  };

  String _greeting() {
    final h = DateTime.now().hour;
    if (h < 5) return 'Доброй ночи';
    if (h < 12) return 'Доброе утро';
    if (h < 18) return 'Добрый день';
    return 'Добрый вечер';
  }

  String _dateStr() {
    const wd = [
      'понедельник',
      'вторник',
      'среда',
      'четверг',
      'пятница',
      'суббота',
      'воскресенье',
    ];
    const months = [
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
    final now = DateTime.now();
    return '${wd[now.weekday - 1]}, ${now.day} ${months[now.month - 1]}';
  }

  void _toast(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    final cont = TakamiDB.items.firstWhere(
      (x) => x.progress > 0 && x.progress < 100 && !x.broken,
      orElse: () => TakamiDB.items.first,
    );
    final fr = TakamiDB.franchiseOf(cont.id);

    final rails = <(String, String, List<TitleItem>)>[
      (
        'Продолжить',
        'вы читаете',
        TakamiDB.items
            .where((x) => x.progress > 0 && x.progress < 100 && !x.broken)
            .toList(),
      ),
      (
        'Манга',
        'популярное',
        TakamiDB.items.where((x) => x.type == ContentType.manga).toList(),
      ),
      (
        'Аниме',
        'сейчас идёт',
        TakamiDB.items.where((x) => x.type == ContentType.anime).toList(),
      ),
      (
        'Ранобэ',
        'подборка',
        TakamiDB.items.where((x) => x.type == ContentType.novel).toList(),
      ),
    ];

    return SingleChildScrollView(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CascadeIn(
            index: 0,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _dateStr().toUpperCase(),
                          style: const TextStyle(
                            fontSize: 11,
                            color: AuroraColors.onSurfaceVariant,
                            letterSpacing: 0.5,
                          ),
                        ),
                        const SizedBox(height: 2),
                        RichText(
                          text: TextSpan(
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w700,
                              fontFamily: AuroraFonts.display,
                              color: Colors.white,
                            ),
                            children: [
                              TextSpan(text: _greeting()),
                              const TextSpan(
                                text: ', Читатель',
                                style: TextStyle(
                                  color: AuroraColors.onSurfaceVariant,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const AiIndicator(),
                  IconButton(
                    onPressed: () => widget.onGo(AppScreen.search),
                    icon: const TakamiIcon(TIcon.search, color: Colors.white),
                  ),
                  IconButton(
                    onPressed: () => widget.onGo(AppScreen.settings),
                    icon: const TakamiIcon(TIcon.settings, color: Colors.white),
                  ),
                ],
              ),
            ),
          ),

          // Hero continue
          CascadeIn(
            index: 1,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: GestureDetector(
                onTap: () => widget.onOpenTitle(cont),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(AuroraRadii.l),
                  child: SizedBox(
                    height: 220,
                    child: Stack(
                      children: [
                        Positioned.fill(
                          child: Container(
                            decoration: BoxDecoration(gradient: fr.bg),
                          ),
                        ),
                        Positioned(
                          top: 14,
                          left: 14,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 5,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.black.withValues(alpha: 0.4),
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              'ПРОДОЛЖИТЬ · ${cont.type.ruName.toUpperCase()}',
                              style: const TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                                letterSpacing: 0.3,
                              ),
                            ),
                          ),
                        ),
                        Positioned(
                          left: 0,
                          right: 0,
                          bottom: 0,
                          child: ClipRRect(
                            child: BackdropGlass(
                              child: Padding(
                                padding: const EdgeInsets.fromLTRB(
                                  16,
                                  14,
                                  16,
                                  14,
                                ),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            fr.title,
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                            style: const TextStyle(
                                              fontSize: 17,
                                              fontWeight: FontWeight.w600,
                                              color: Colors.white,
                                            ),
                                          ),
                                          Text(
                                            '${cont.sub} · ${cont.source}',
                                            style: const TextStyle(
                                              fontSize: 11,
                                              color:
                                                  AuroraColors.onSurfaceVariant,
                                            ),
                                          ),
                                          const SizedBox(height: 6),
                                          Row(
                                            children: [
                                              Expanded(
                                                child: ClipRRect(
                                                  borderRadius:
                                                      BorderRadius.circular(2),
                                                  child:
                                                      LinearProgressIndicator(
                                                        value:
                                                            cont.progress / 100,
                                                        minHeight: 2,
                                                        backgroundColor:
                                                            Colors.white24,
                                                        color: AuroraColors.acc,
                                                      ),
                                                ),
                                              ),
                                              const SizedBox(width: 8),
                                              Text(
                                                '${cont.progress}%',
                                                style: const TextStyle(
                                                  fontSize: 11,
                                                  color: Colors.white,
                                                  fontFeatures: [
                                                    FontFeature.tabularFigures(),
                                                  ],
                                                ),
                                              ),
                                            ],
                                          ),
                                        ],
                                      ),
                                    ),
                                    const SizedBox(width: 10),
                                    ElevatedButton.icon(
                                      onPressed: () => widget.onGo(
                                        cont.type == ContentType.anime
                                            ? AppScreen.player
                                            : AppScreen.reader,
                                      ),
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: AuroraColors.acc,
                                        foregroundColor: Colors.white,
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(
                                            999,
                                          ),
                                        ),
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 14,
                                          vertical: 10,
                                        ),
                                      ),
                                      icon: TakamiIcon(
                                        cont.type == ContentType.anime
                                            ? TIcon.play
                                            : TIcon.book,
                                        size: 16,
                                        color: Colors.white,
                                      ),
                                      label: Text(
                                        cont.type == ContentType.anime
                                            ? 'Смотреть'
                                            : 'Читать',
                                        style: const TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Quick actions
          CascadeIn(
            index: 2,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  _quickAction(
                    TIcon.refresh,
                    'Обновления',
                    '3',
                    () => widget.onGo(AppScreen.library),
                  ),
                  _quickAction(
                    TIcon.calendar,
                    'Календарь',
                    'сегодня',
                    () => widget.onGo(AppScreen.calendar),
                  ),
                  _quickAction(
                    TIcon.search,
                    'Поиск',
                    'по кадру',
                    () => widget.onGo(AppScreen.search),
                  ),
                  _quickAction(
                    TIcon.swipes,
                    'Свайпы',
                    'подбор',
                    () => widget.onGo(AppScreen.swipes),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 20),

          // News carousel
          CascadeIn(
            index: 3,
            child: _sectionHeader(
              'Новости аниме',
              'что происходит в индустрии',
              () => _toast('Открываем ленту новостей аниме-индустрии'),
            ),
          ),
          SizedBox(
            height: 132,
            child: NotificationListener<ScrollNotification>(
              onNotification: (notif) {
                final w = 268.0;
                setState(() => _newsIdx = (_newsController.offset / w).round());
                return false;
              },
              child: ListView.builder(
                controller: _newsController,
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: TakamiDB.news.length,
                itemBuilder: (context, i) {
                  final n = TakamiDB.news[i];
                  return GestureDetector(
                    onTap: () => _toast('Открываем: ${n.title}'),
                    child: Container(
                      width: 250,
                      margin: const EdgeInsets.only(right: 12),
                      decoration: BoxDecoration(
                        gradient: n.bg,
                        borderRadius: BorderRadius.circular(AuroraRadii.m),
                        border: Border.all(color: AuroraColors.brd),
                      ),
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 3,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.black.withValues(alpha: 0.3),
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              n.category,
                              style: const TextStyle(
                                fontSize: 9.5,
                                color: Colors.white,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          const Spacer(),
                          Text(
                            n.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            n.sub,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 10.5,
                              color: AuroraColors.onSurfaceVariant,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              Text(
                                n.source,
                                style: const TextStyle(
                                  fontSize: 9.5,
                                  color: Colors.white70,
                                ),
                              ),
                              const Text(
                                '  ·  ',
                                style: TextStyle(
                                  fontSize: 9.5,
                                  color: Colors.white38,
                                ),
                              ),
                              Text(
                                n.time,
                                style: const TextStyle(
                                  fontSize: 9.5,
                                  color: Colors.white70,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(TakamiDB.news.length, (i) {
                return Container(
                  width: 5,
                  height: 5,
                  margin: const EdgeInsets.symmetric(horizontal: 3),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: i == _newsIdx ? AuroraColors.acc : Colors.white24,
                  ),
                );
              }),
            ),
          ),

          // Rails
          for (int ri = 0; ri < rails.length; ri++)
            if (rails[ri].$3.isNotEmpty)
              CascadeIn(
                index: 4 + ri,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 12),
                    _sectionHeader(
                      rails[ri].$1,
                      rails[ri].$2,
                      () => widget.onGo(AppScreen.library),
                    ),
                    SizedBox(
                      height: 190,
                      child: ListView.builder(
                        scrollDirection: Axis.horizontal,
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        itemCount: rails[ri].$3.length,
                        itemBuilder: (context, i) {
                          final x = rails[ri].$3[i];
                          final f2 = TakamiDB.franchiseOf(x.id);
                          final showBadge =
                              x.badge.isNotEmpty &&
                              x.badge != 'off' &&
                              x.badge != 'err';
                          return GestureDetector(
                            onTap: () => widget.onOpenTitle(x),
                            child: Container(
                              width: 118,
                              margin: const EdgeInsets.only(right: 10),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  AspectRatio(
                                    aspectRatio: 3 / 4,
                                    child: ClipRRect(
                                      borderRadius: BorderRadius.circular(
                                        AuroraRadii.m,
                                      ),
                                      child: Stack(
                                        children: [
                                          Positioned.fill(
                                            child: DecoratedBox(
                                              decoration: BoxDecoration(
                                                gradient: f2.bg,
                                              ),
                                            ),
                                          ),
                                          Positioned(
                                            top: 6,
                                            left: 6,
                                            child: TakamiIcon(
                                              _typeIcon(x.type),
                                              size: 16,
                                              color: Colors.white70,
                                            ),
                                          ),
                                          if (showBadge)
                                            Positioned(
                                              top: 6,
                                              right: 6,
                                              child: Container(
                                                padding:
                                                    const EdgeInsets.symmetric(
                                                      horizontal: 6,
                                                      vertical: 2,
                                                    ),
                                                decoration: BoxDecoration(
                                                  color: AuroraColors.acc,
                                                  borderRadius:
                                                      BorderRadius.circular(6),
                                                ),
                                                child: Text(
                                                  x.badge,
                                                  style: const TextStyle(
                                                    fontSize: 9,
                                                    color: Colors.white,
                                                    fontWeight: FontWeight.w700,
                                                  ),
                                                ),
                                              ),
                                            ),
                                          if (x.progress > 0 &&
                                              x.progress < 100)
                                            Positioned(
                                              left: 0,
                                              right: 0,
                                              bottom: 0,
                                              child: LinearProgressIndicator(
                                                value: x.progress / 100,
                                                minHeight: 3,
                                                backgroundColor: Colors.black38,
                                                color: AuroraColors.acc,
                                              ),
                                            ),
                                        ],
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    f2.title,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.w500,
                                      color: Colors.white,
                                    ),
                                  ),
                                  if (x.rating > 0)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 2),
                                      child: Text(
                                        '★ ${x.rating.toStringAsFixed(1)}',
                                        style: const TextStyle(
                                          fontSize: 10.5,
                                          color: AuroraColors.warn,
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  ],
                ),
              ),
        ],
      ),
    );
  }

  Widget _quickAction(
    TIcon icon,
    String label,
    String value,
    VoidCallback onTap,
  ) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: Column(
          children: [
            TakamiIcon(icon, size: 22, color: AuroraColors.acc),
            const SizedBox(height: 6),
            Text(
              label,
              style: const TextStyle(fontSize: 11, color: Colors.white),
            ),
            Text(
              value,
              style: const TextStyle(
                fontSize: 10,
                color: AuroraColors.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _sectionHeader(String title, String sub, VoidCallback onMore) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                ),
                Text(
                  sub,
                  style: const TextStyle(
                    fontSize: 11,
                    color: AuroraColors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          GestureDetector(
            onTap: onMore,
            child: const Text(
              'Все ›',
              style: TextStyle(fontSize: 12, color: AuroraColors.acc2),
            ),
          ),
        ],
      ),
    );
  }
}

class BackdropGlass extends StatelessWidget {
  final Widget child;
  const BackdropGlass({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: const Color(0xB3121218),
        border: const Border(top: BorderSide(color: AuroraColors.brd)),
      ),
      child: child,
    );
  }
}
