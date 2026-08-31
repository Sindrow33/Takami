// Search — поиск. Портирован из kit/Search.jsx (упрощённая версия)
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class SearchScreen extends StatefulWidget {
  final VoidCallback onBack;
  final void Function(TitleItem) onOpenTitle;
  const SearchScreen({
    super.key,
    required this.onBack,
    required this.onOpenTitle,
  });

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final _ctrl = TextEditingController();
  String filter = 'all';
  final recent = ['Клинок души', 'AniLibria', 'MAPPA 2027'];

  void _t(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    final map = {
      'manga': ContentType.manga,
      'anime': ContentType.anime,
      'novel': ContentType.novel,
    };
    final query = _ctrl.text.trim().toLowerCase();
    final results = TakamiDB.items.where((x) {
      final fr = TakamiDB.franchiseOf(x.id);
      final matchesQuery =
          query.isEmpty || fr.title.toLowerCase().contains(query);
      final matchesFilter = filter == 'all' || x.type == map[filter];
      return matchesQuery && matchesFilter;
    }).toList();

    return Column(
      children: [
        SafeArea(
          bottom: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(8, 8, 16, 8),
            child: Row(
              children: [
                IconButton(
                  onPressed: widget.onBack,
                  icon: const TakamiIcon(TIcon.back, color: Colors.white),
                ),
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: AuroraColors.sub,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AuroraColors.brd),
                    ),
                    child: Row(
                      children: [
                        const TakamiIcon(
                          TIcon.search,
                          size: 16,
                          color: AuroraColors.onSurfaceVariant,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _ctrl,
                            onChanged: (_) => setState(() {}),
                            style: const TextStyle(
                              fontSize: 13,
                              color: Colors.white,
                            ),
                            decoration: const InputDecoration(
                              hintText: 'Поиск тайтлов, персонажей…',
                              hintStyle: TextStyle(
                                fontSize: 12.5,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                              border: InputBorder.none,
                              isDense: true,
                              contentPadding: EdgeInsets.symmetric(
                                vertical: 12,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                IconButton(
                  onPressed: () => _t('Скриншот-анализ · выберите изображение'),
                  icon: const TakamiIcon(TIcon.filter, color: Colors.white),
                ),
              ],
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
            ],
          ),
        ),
        Expanded(
          child: query.isEmpty
              ? SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Недавнее',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 10),
                      ...recent.map(
                        (r) => Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: GestureDetector(
                            onTap: () => setState(() => _ctrl.text = r),
                            child: Row(
                              children: [
                                const TakamiIcon(
                                  TIcon.clock,
                                  size: 14,
                                  color: AuroraColors.onSurfaceVariant,
                                ),
                                const SizedBox(width: 10),
                                Text(
                                  r,
                                  style: const TextStyle(
                                    fontSize: 13,
                                    color: Colors.white70,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      const Text(
                        'Жанры',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: TakamiDB.genres
                            .take(12)
                            .map(
                              (g) => GestureDetector(
                                onTap: () => _t('Поиск по жанру: $g'),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12,
                                    vertical: 8,
                                  ),
                                  decoration: BoxDecoration(
                                    color: AuroraColors.sub,
                                    borderRadius: BorderRadius.circular(999),
                                    border: Border.all(color: AuroraColors.brd),
                                  ),
                                  child: Text(
                                    g,
                                    style: const TextStyle(
                                      fontSize: 11.5,
                                      color: Colors.white70,
                                    ),
                                  ),
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    ],
                  ),
                )
              : results.isEmpty
              ? const Center(
                  child: Text(
                    'Ничего не найдено',
                    style: TextStyle(color: AuroraColors.onSurfaceVariant),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: results.length,
                  itemBuilder: (context, i) {
                    final x = results[i];
                    final fr = TakamiDB.franchiseOf(x.id);
                    return GestureDetector(
                      onTap: () => widget.onOpenTitle(x),
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Container(
                                width: 48,
                                height: 64,
                                decoration: BoxDecoration(gradient: fr.bg),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    fr.title,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white,
                                    ),
                                  ),
                                  Text(
                                    '${x.type.ruName} · ${x.source}',
                                    style: const TextStyle(
                                      fontSize: 11,
                                      color: AuroraColors.onSurfaceVariant,
                                    ),
                                  ),
                                ],
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
    );
  }
}
