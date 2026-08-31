// Title — экран тайтла/франшизы. Портирован из kit/Title.jsx
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class TitleDetailScreen extends StatefulWidget {
  final int itemId;
  final VoidCallback onBack;
  final void Function(TitleItem) onRead;
  final void Function(CharacterItem) onOpenChar;

  const TitleDetailScreen({
    super.key,
    required this.itemId,
    required this.onBack,
    required this.onRead,
    required this.onOpenChar,
  });

  @override
  State<TitleDetailScreen> createState() => _TitleDetailScreenState();
}

class _TitleDetailScreenState extends State<TitleDetailScreen> {
  bool descOpen = false;
  bool inLib = true;

  void _t(String m) => ToastController.show(context, m);

  static const _palettes = [
    [Color(0xFF5B3BE8), Color(0xFF2A1F55), Color(0xFF141821)],
    [Color(0xFFE64C7A), Color(0xFF5A1F35), Color(0xFF141821)],
    [Color(0xFF00A6C0), Color(0xFF0F3A48), Color(0xFF141821)],
    [Color(0xFFC97A2E), Color(0xFF4A2E15), Color(0xFF141821)],
    [Color(0xFF3D8F6A), Color(0xFF1F3A2E), Color(0xFF141821)],
    [Color(0xFF7C5CFF), Color(0xFF2A1F55), Color(0xFF141821)],
  ];

  @override
  Widget build(BuildContext context) {
    final it = TakamiDB.items.firstWhere(
      (x) => x.id == widget.itemId,
      orElse: () => TakamiDB.items.first,
    );
    final fr = TakamiDB.franchiseOf(it.id);
    final sib = fr.itemIds
        .map((id) => TakamiDB.items.firstWhere((x) => x.id == id))
        .toList();

    final unit = it.type == ContentType.anime ? 'Эпизод' : 'Глава';
    final contLabel = it.type == ContentType.anime ? 'эп. 8' : 'гл. 43';

    final chapters = <(int, String, String, bool, bool, bool)>[];
    for (int n = 45; n >= 38; n--) {
      final bad = it.broken && n == 41;
      chapters.add((
        n,
        '$unit $n',
        bad ? 'Не загружается' : '12.0${n % 9}.2026',
        n < 43,
        bad,
        n > 43,
      ));
    }

    return Column(
      children: [
        TakamiAppBar(
          onBack: widget.onBack,
          actions: [
            TakamiAction(
              icon: TIcon.search,
              onClick: () => _t('Поиск главы или страницы в тайтле'),
            ),
            TakamiAction(
              icon: TIcon.menu,
              onClick: () =>
                  _t('Поделиться · Скачать всё · Отслеживать · Мигрировать'),
            ),
          ],
        ),
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Hero
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(AuroraRadii.m),
                        child: Container(
                          width: 104,
                          height: 140,
                          decoration: BoxDecoration(gradient: fr.bg),
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              fr.title,
                              style: const TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w700,
                                fontFamily: AuroraFonts.display,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Автор · Выходит',
                              style: TextStyle(
                                fontSize: 11.5,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                            Text(
                              '${it.type.ruName} · ${it.count} · ${it.year}',
                              style: const TextStyle(
                                fontSize: 11.5,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Wrap(
                              spacing: 6,
                              runSpacing: 6,
                              children: fr.genres
                                  .map(
                                    (g) => Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 8,
                                        vertical: 4,
                                      ),
                                      decoration: BoxDecoration(
                                        color: AuroraColors.sub,
                                        borderRadius: BorderRadius.circular(
                                          999,
                                        ),
                                        border: Border.all(
                                          color: AuroraColors.brd,
                                        ),
                                      ),
                                      child: Text(
                                        g,
                                        style: const TextStyle(
                                          fontSize: 10.5,
                                          color: Colors.white,
                                        ),
                                      ),
                                    ),
                                  )
                                  .toList(),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),

                if (sib.length > 1)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Row(
                      children:
                          [
                            ContentType.manga,
                            ContentType.anime,
                            ContentType.novel,
                          ].map((k) {
                            final o = sib.where((x) => x.type == k).firstOrNull;
                            final isOn = o != null && o.id == it.id;
                            return Expanded(
                              child: Container(
                                margin: const EdgeInsets.only(right: 6),
                                padding: const EdgeInsets.symmetric(
                                  vertical: 8,
                                ),
                                decoration: BoxDecoration(
                                  color: isOn
                                      ? AuroraColors.acc.withValues(alpha: 0.15)
                                      : AuroraColors.sub,
                                  border: Border.all(
                                    color: isOn
                                        ? AuroraColors.acc
                                        : AuroraColors.brd,
                                  ),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Column(
                                  children: [
                                    Text(
                                      k.ruName,
                                      style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.w600,
                                        color: isOn
                                            ? AuroraColors.acc2
                                            : Colors.white70,
                                      ),
                                    ),
                                    Text(
                                      o?.count ?? 'нет',
                                      style: const TextStyle(
                                        fontSize: 10,
                                        color: AuroraColors.onSurfaceVariant,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          }).toList(),
                    ),
                  ),
                const SizedBox(height: 12),

                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => setState(() => inLib = !inLib),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: inLib
                                ? AuroraColors.ok
                                : Colors.white,
                            side: BorderSide(
                              color: inLib
                                  ? AuroraColors.ok.withValues(alpha: 0.4)
                                  : AuroraColors.brd,
                            ),
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          icon: TakamiIcon(
                            inLib ? TIcon.check : TIcon.plus,
                            size: 15,
                            color: inLib ? AuroraColors.ok : Colors.white,
                          ),
                          label: Text(
                            inLib ? 'В библиотеке' : 'В библиотеку',
                            style: const TextStyle(fontSize: 12.5),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: () => widget.onRead(it),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AuroraColors.acc,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          icon: TakamiIcon(
                            it.type == ContentType.anime
                                ? TIcon.play
                                : TIcon.book,
                            size: 15,
                            color: Colors.white,
                          ),
                          label: Text(
                            'Продолжить · $contLabel',
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),

                // Source status
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: it.broken
                          ? AuroraColors.error.withValues(alpha: 0.08)
                          : AuroraColors.sub,
                      border: Border.all(
                        color: it.broken
                            ? AuroraColors.error.withValues(alpha: 0.3)
                            : AuroraColors.brd,
                      ),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Column(
                      children: [
                        Row(
                          children: [
                            Container(
                              width: 8,
                              height: 8,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: it.broken
                                    ? AuroraColors.error
                                    : AuroraColors.ok,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    it.source,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white,
                                    ),
                                  ),
                                  Text(
                                    it.broken
                                        ? 'парсер сломан · показаны сохранённые данные'
                                        : 'активен · v1.4.2 · проверен минуту назад',
                                    style: const TextStyle(
                                      fontSize: 10.5,
                                      color: AuroraColors.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            TextButton(
                              onPressed: () => _t('Открываем выбор источника'),
                              child: const Text(
                                'Сменить',
                                style: TextStyle(
                                  fontSize: 11.5,
                                  color: AuroraColors.acc2,
                                ),
                              ),
                            ),
                          ],
                        ),
                        if (it.sourceUrl.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 8),
                            child: GestureDetector(
                              onTap: () =>
                                  _t('Открываем в браузере: ${it.source}'),
                              child: Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 10,
                                  vertical: 8,
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.black.withValues(alpha: 0.25),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Row(
                                  children: [
                                    const TakamiIcon(
                                      TIcon.external,
                                      size: 13,
                                      color: AuroraColors.acc2,
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: Text(
                                        it.sourceUrl.replaceFirst(
                                          RegExp(r'^https?://'),
                                          '',
                                        ),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(
                                          fontSize: 10.5,
                                          color: AuroraColors.onSurfaceVariant,
                                        ),
                                      ),
                                    ),
                                    const Text(
                                      '›',
                                      style: TextStyle(
                                        color: AuroraColors.onSurfaceVariant,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),

                if (it.broken)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AuroraColors.error.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          const TakamiIcon(
                            TIcon.alert,
                            size: 18,
                            color: Color(0xFFFF9E9E),
                          ),
                          const SizedBox(width: 10),
                          const Expanded(
                            child: Text(
                              'Часть глав не найдена у этого источника. Можно мигрировать — прогресс сохранится.',
                              style: TextStyle(
                                fontSize: 11.5,
                                color: Colors.white,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                  child: Text(
                    descOpen
                        ? fr.description
                        : (fr.description.length > 120
                              ? '${fr.description.substring(0, 120)}…'
                              : fr.description),
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: Colors.white70,
                      height: 1.5,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 16, top: 4),
                  child: GestureDetector(
                    onTap: () => setState(() => descOpen = !descOpen),
                    child: Text(
                      descOpen ? 'Свернуть ‹' : 'Читать полностью ›',
                      style: const TextStyle(
                        fontSize: 11.5,
                        color: AuroraColors.acc2,
                      ),
                    ),
                  ),
                ),

                // Characters
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                  child: Row(
                    children: [
                      const Expanded(
                        child: Text(
                          'Персонажи',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      Text(
                        'Все ${TakamiDB.chars.length} ›',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AuroraColors.acc2,
                        ),
                      ),
                    ],
                  ),
                ),
                SizedBox(
                  height: 168,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: TakamiDB.chars.length,
                    itemBuilder: (context, i) {
                      final c = TakamiDB.chars[i];
                      final colors = _palettes[i % _palettes.length];
                      return GestureDetector(
                        onTap: () => widget.onOpenChar(c),
                        child: Container(
                          width: 108,
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
                                            gradient: LinearGradient(
                                              begin: Alignment.topLeft,
                                              end: Alignment.bottomRight,
                                              colors: colors,
                                              stops: const [0, 0.6, 1],
                                            ),
                                          ),
                                        ),
                                      ),
                                      Center(
                                        child: Text(
                                          c.name.isNotEmpty ? c.name[0] : '?',
                                          style: const TextStyle(
                                            fontSize: 32,
                                            fontWeight: FontWeight.w700,
                                            color: Colors.white24,
                                          ),
                                        ),
                                      ),
                                      if (c.main)
                                        Positioned(
                                          top: 6,
                                          left: 6,
                                          child: Container(
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 6,
                                              vertical: 2,
                                            ),
                                            decoration: BoxDecoration(
                                              color: AuroraColors.acc,
                                              borderRadius:
                                                  BorderRadius.circular(6),
                                            ),
                                            child: const Text(
                                              'Главный',
                                              style: TextStyle(
                                                fontSize: 8.5,
                                                color: Colors.white,
                                                fontWeight: FontWeight.w700,
                                              ),
                                            ),
                                          ),
                                        ),
                                      Positioned(
                                        bottom: 6,
                                        right: 6,
                                        child: Text(
                                          c.jp,
                                          style: TextStyle(
                                            fontSize: 9,
                                            color: Colors.white.withValues(
                                              alpha: 0.5,
                                            ),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                c.name,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontSize: 11.5,
                                  fontWeight: FontWeight.w500,
                                  color: Colors.white,
                                ),
                              ),
                              Text(
                                c.role,
                                style: const TextStyle(
                                  fontSize: 10,
                                  color: AuroraColors.onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),

                // Chapters
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          it.type == ContentType.anime ? 'Эпизоды' : 'Главы',
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      Text(
                        'Все ${it.count}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AuroraColors.acc2,
                        ),
                      ),
                    ],
                  ),
                ),
                ...chapters.map((ch) {
                  final (n, title, date, read, broken, downloaded) = ch;
                  return GestureDetector(
                    onTap: broken ? null : () => widget.onRead(it),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 8,
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  title,
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600,
                                    color: read ? Colors.white54 : Colors.white,
                                  ),
                                ),
                                Text(
                                  date,
                                  style: TextStyle(
                                    fontSize: 10.5,
                                    color: broken
                                        ? AuroraColors.error
                                        : AuroraColors.onSurfaceVariant,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          TakamiIcon(
                            downloaded ? TIcon.check : TIcon.download,
                            size: 16,
                            color: downloaded
                                ? AuroraColors.ok
                                : AuroraColors.onSurfaceVariant,
                          ),
                        ],
                      ),
                    ),
                  );
                }),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
