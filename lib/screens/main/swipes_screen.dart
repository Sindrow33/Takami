// Swipes — Tinder-подобный подбор тайтлов. Портирован из kit/Swipes.jsx
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class SwipesScreen extends StatefulWidget {
  final VoidCallback onBack;
  const SwipesScreen({super.key, required this.onBack});

  @override
  State<SwipesScreen> createState() => _SwipesScreenState();
}

class _SwipesScreenState extends State<SwipesScreen> {
  String filter = 'all';
  int likes = 0;
  int nopes = 0;
  int index = 0;
  Offset dragOffset = Offset.zero;

  List<TitleItem> get pool {
    final map = {
      'manga': ContentType.manga,
      'anime': ContentType.anime,
      'novel': ContentType.novel,
    };
    if (filter == 'all') return TakamiDB.swipesPool;
    return TakamiDB.swipesPool.where((x) => x.type == map[filter]).toList();
  }

  void _t(String m) => ToastController.show(context, m);

  void _swipe(bool like) {
    setState(() {
      if (like) {
        likes++;
        _t('В библиотеку добавлено');
      } else {
        nopes++;
      }
      index++;
      dragOffset = Offset.zero;
    });
  }

  @override
  Widget build(BuildContext context) {
    final items = pool;
    final hasCard = index < items.length;

    return Column(
      children: [
        TakamiAppBar(onBack: widget.onBack, title: 'Свайпы'),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              FilterChip2(
                label: 'Всё',
                selected: filter == 'all',
                onTap: () => setState(() {
                  filter = 'all';
                  index = 0;
                }),
              ),
              FilterChip2(
                label: 'Манга',
                selected: filter == 'manga',
                onTap: () => setState(() {
                  filter = 'manga';
                  index = 0;
                }),
              ),
              FilterChip2(
                label: 'Аниме',
                selected: filter == 'anime',
                onTap: () => setState(() {
                  filter = 'anime';
                  index = 0;
                }),
              ),
              FilterChip2(
                label: 'Ранобэ',
                selected: filter == 'novel',
                onTap: () => setState(() {
                  filter = 'novel';
                  index = 0;
                }),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              Text(
                '👍 $likes',
                style: const TextStyle(fontSize: 12, color: AuroraColors.ok),
              ),
              const SizedBox(width: 16),
              Text(
                '👎 $nopes',
                style: const TextStyle(fontSize: 12, color: AuroraColors.error),
              ),
            ],
          ),
        ),
        Expanded(
          child: Center(
            child: hasCard
                ? _buildCard(items[index])
                : Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const TakamiIcon(
                        TIcon.swipes,
                        size: 48,
                        color: AuroraColors.onSurfaceVariant,
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        'Пул закончился',
                        style: TextStyle(fontSize: 15, color: Colors.white),
                      ),
                      const SizedBox(height: 8),
                      TextButton(
                        onPressed: () => setState(() => index = 0),
                        child: const Text(
                          'Начать заново',
                          style: TextStyle(color: AuroraColors.acc2),
                        ),
                      ),
                    ],
                  ),
          ),
        ),
        if (hasCard)
          Padding(
            padding: const EdgeInsets.only(bottom: 24),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _actionBtn(
                  Icons.close,
                  AuroraColors.error,
                  () => _swipe(false),
                ),
                const SizedBox(width: 24),
                _actionBtn(
                  Icons.info_outline,
                  AuroraColors.onSurfaceVariant,
                  () => _t('Открываем подробности'),
                ),
                const SizedBox(width: 24),
                _actionBtn(Icons.favorite, AuroraColors.ok, () => _swipe(true)),
              ],
            ),
          ),
      ],
    );
  }

  Widget _actionBtn(IconData icon, Color color, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 56,
        height: 56,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AuroraColors.surfaceContainer,
          border: Border.all(color: color.withValues(alpha: 0.4)),
        ),
        child: Icon(icon, color: color, size: 26),
      ),
    );
  }

  Widget _buildCard(TitleItem item) {
    final genres = TakamiDB.swipesGenres[item.id] ?? [];
    final desc = TakamiDB.swipesDesc[item.id] ?? '';

    return GestureDetector(
      onPanUpdate: (details) => setState(() => dragOffset += details.delta),
      onPanEnd: (details) {
        if (dragOffset.dx > 100) {
          _swipe(true);
        } else if (dragOffset.dx < -100) {
          _swipe(false);
        } else {
          setState(() => dragOffset = Offset.zero);
        }
      },
      child: Transform.translate(
        offset: dragOffset,
        child: Transform.rotate(
          angle: dragOffset.dx / 600,
          child: Container(
            width: 300,
            height: 440,
            decoration: BoxDecoration(
              gradient: item.bg,
              borderRadius: BorderRadius.circular(AuroraRadii.l),
              border: Border.all(color: AuroraColors.brd),
              boxShadow: AuroraShadows.mdShadow,
            ),
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: item.type.color.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    item.type.ruName,
                    style: TextStyle(
                      fontSize: 10.5,
                      color: item.type.color,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const Spacer(),
                Text(
                  item.title,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    fontFamily: AuroraFonts.display,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${item.source} · ${item.year}',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AuroraColors.onSurfaceVariant,
                  ),
                ),
                if (item.rating > 0)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      '★ ${item.rating}',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AuroraColors.warn,
                      ),
                    ),
                  ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 6,
                  children: genres
                      .map(
                        (g) => Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.white10,
                            borderRadius: BorderRadius.circular(999),
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
                const SizedBox(height: 12),
                Text(
                  desc,
                  style: const TextStyle(
                    fontSize: 12.5,
                    color: Colors.white70,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
