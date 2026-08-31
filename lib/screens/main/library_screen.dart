// Library — портирована из kit/Library.jsx
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../state/app_state.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class LibraryScreen extends StatefulWidget {
  final void Function(AppScreen) onGo;
  final void Function(TitleItem) onOpenTitle;

  const LibraryScreen({
    super.key,
    required this.onGo,
    required this.onOpenTitle,
  });

  @override
  State<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends State<LibraryScreen> {
  String filter = 'all';

  void _toast(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    final filterMap = {
      'manga': ContentType.manga,
      'anime': ContentType.anime,
      'novel': ContentType.novel,
    };

    final franchiseEntries = TakamiDB.franchises.entries
        .map((e) {
          final fr = e.value;
          final items = fr.itemIds
              .map((id) => TakamiDB.items.firstWhere((x) => x.id == id))
              .toList();
          final shown = filter == 'all'
              ? items
              : items.where((x) => x.type == filterMap[filter]).toList();
          if (shown.isEmpty) return null;
          final head = shown.first;
          final multi = items.length > 1;
          return (fr, items, head, multi);
        })
        .whereType<(Franchise, List<TitleItem>, TitleItem, bool)>()
        .toList();

    return Column(
      children: [
        TakamiAppBar(
          title: 'Библиотека',
          actions: [
            TakamiAction(
              icon: TIcon.search,
              onClick: () => widget.onGo(AppScreen.search),
            ),
            TakamiAction(
              icon: TIcon.menu,
              onClick: () => _toast('Сортировка · Вид · Обновить всё'),
            ),
          ],
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
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
            ],
          ),
        ),
        Expanded(
          child: GridView.builder(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 10,
              mainAxisSpacing: 14,
              childAspectRatio: 0.62,
            ),
            itemCount: franchiseEntries.length,
            itemBuilder: (context, i) {
              final (fr, items, head, multi) = franchiseEntries[i];
              return GestureDetector(
                onTap: () => widget.onOpenTitle(head),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    AspectRatio(
                      aspectRatio: 3 / 4,
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(AuroraRadii.m),
                        child: Stack(
                          children: [
                            Positioned.fill(
                              child: DecoratedBox(
                                decoration: BoxDecoration(gradient: fr.bg),
                              ),
                            ),
                            if (multi)
                              Positioned(
                                bottom: 4,
                                left: 4,
                                right: 4,
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 5,
                                    vertical: 2,
                                  ),
                                  decoration: BoxDecoration(
                                    color: Colors.black.withValues(alpha: 0.5),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    '${items.length} формата',
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(
                                      fontSize: 8.5,
                                      color: Colors.white,
                                    ),
                                  ),
                                ),
                              ),
                            if (head.badge == 'err')
                              Positioned(
                                top: 4,
                                right: 4,
                                child: _badgeDot(AuroraColors.error, '!'),
                              )
                            else if (head.badge == 'off')
                              Positioned(
                                top: 4,
                                right: 4,
                                child: _badgeDot(
                                  AuroraColors.onSurfaceVariant,
                                  '↓',
                                ),
                              )
                            else if (head.badge.isNotEmpty)
                              Positioned(
                                top: 4,
                                right: 4,
                                child: _badgeDot(
                                  AuroraColors.acc,
                                  head.badge,
                                  wide: true,
                                ),
                              ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      fr.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w500,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      head.sub,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 9.5,
                        color: AuroraColors.onSurfaceVariant,
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(2),
                        child: LinearProgressIndicator(
                          value: head.progress / 100,
                          minHeight: 2,
                          backgroundColor: Colors.white12,
                          color: AuroraColors.acc,
                        ),
                      ),
                    ),
                    if (multi)
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Row(
                          children:
                              [
                                ContentType.manga,
                                ContentType.anime,
                                ContentType.novel,
                              ].map((k) {
                                final has = items.any((x) => x.type == k);
                                final active = has && head.type == k;
                                return Padding(
                                  padding: const EdgeInsets.only(right: 4),
                                  child: Text(
                                    k.shortName,
                                    style: TextStyle(
                                      fontSize: 9,
                                      fontWeight: FontWeight.w700,
                                      color: active
                                          ? AuroraColors.acc
                                          : has
                                          ? Colors.white54
                                          : Colors.white24,
                                    ),
                                  ),
                                );
                              }).toList(),
                        ),
                      ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _badgeDot(Color color, String text, {bool wide = false}) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: wide ? 6 : 5, vertical: 2),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 9,
          color: Colors.white,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
