// Character — экран биографии персонажа. Портирован из kit/Character.jsx
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class CharacterScreen extends StatefulWidget {
  final int charId;
  final VoidCallback onBack;
  final void Function(TitleItem) onOpenTitle;

  const CharacterScreen({
    super.key,
    required this.charId,
    required this.onBack,
    required this.onOpenTitle,
  });

  @override
  State<CharacterScreen> createState() => _CharacterScreenState();
}

class _CharacterScreenState extends State<CharacterScreen> {
  bool fav = false;

  static const _palettes = [
    [Color(0xFF5B3BE8), Color(0xFF2A1F55), Color(0xFF141821)],
    [Color(0xFFE64C7A), Color(0xFF5A1F35), Color(0xFF141821)],
    [Color(0xFF00A6C0), Color(0xFF0F3A48), Color(0xFF141821)],
    [Color(0xFFC97A2E), Color(0xFF4A2E15), Color(0xFF141821)],
    [Color(0xFF3D8F6A), Color(0xFF1F3A2E), Color(0xFF141821)],
    [Color(0xFF7C5CFF), Color(0xFF2A1F55), Color(0xFF141821)],
  ];

  void _t(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    final c = TakamiDB.chars.firstWhere(
      (x) => x.id == widget.charId,
      orElse: () => TakamiDB.chars.first,
    );
    final seiyuu = TakamiDB.seiyuu[c.seiyuuId];
    final colors = _palettes[(c.id - 1) % _palettes.length];
    final appearsIn = c.appearsIn
        .map(
          (id) => TakamiDB.items.firstWhere(
            (x) => x.id == id,
            orElse: () => TakamiDB.items.first,
          ),
        )
        .toList();

    final info = <(String, String)>[
      ('Возраст', c.age != null ? '${c.age}' : '—'),
      ('Рост', c.height.isEmpty ? '—' : c.height),
      ('Группа крови', c.bloodType.isEmpty ? '—' : c.bloodType),
      ('День рожд.', c.birthday.isEmpty ? '—' : c.birthday),
      ('Знак', c.zodiac.isEmpty ? '—' : c.zodiac),
    ];

    return Column(
      children: [
        TakamiAppBar(
          onBack: widget.onBack,
          actions: [
            TakamiAction(
              icon: TIcon.search,
              onClick: () => _t('Поиск: другие тайтлы с этим персонажем'),
            ),
            TakamiAction(
              icon: TIcon.menu,
              onClick: () => _t('Поделиться · В избранное · Пожаловаться'),
            ),
          ],
        ),
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Column(
                      children: [
                        Container(
                          width: 120,
                          height: 120,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: LinearGradient(colors: colors),
                          ),
                          child: Center(
                            child: Text(
                              c.name.isNotEmpty ? c.name[0] : '?',
                              style: const TextStyle(
                                fontSize: 44,
                                fontWeight: FontWeight.w700,
                                color: Colors.white38,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          c.name,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            fontFamily: AuroraFonts.display,
                            color: Colors.white,
                          ),
                        ),
                        Text(
                          c.jp,
                          style: const TextStyle(
                            fontSize: 13,
                            color: AuroraColors.acc2,
                          ),
                        ),
                        const SizedBox(height: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: AuroraColors.sub,
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: Text(
                            c.role,
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.white70,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => setState(() => fav = !fav),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: fav
                                ? const Color(0xFFFF6B8A)
                                : Colors.white,
                            side: BorderSide(
                              color: fav
                                  ? const Color(0x80FF6B8A)
                                  : AuroraColors.brd,
                            ),
                            padding: const EdgeInsets.symmetric(vertical: 11),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          icon: TakamiIcon(
                            fav ? TIcon.heartFilled : TIcon.heart,
                            size: 15,
                            color: fav ? const Color(0xFFFF6B8A) : Colors.white,
                          ),
                          label: Text(
                            fav ? 'В избранном' : 'В избранное',
                            style: const TextStyle(fontSize: 12.5),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                  child: GridView.count(
                    crossAxisCount: 2,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    mainAxisSpacing: 8,
                    crossAxisSpacing: 8,
                    childAspectRatio: 3,
                    children: info.map((e) {
                      return Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AuroraColors.sub,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              e.$1,
                              style: const TextStyle(
                                fontSize: 10,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                            Text(
                              e.$2,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
                ),
                if (c.affiliation.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                    child: _infoBlock('Принадлежность', c.affiliation),
                  ),
                if (c.origin.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                    child: _infoBlock('Происхождение', c.origin),
                  ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                  child: _infoBlock('Биография', c.bio, isBio: true),
                ),
                if (c.quotes.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Цитаты',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 8),
                        ...c.quotes.map(
                          (q) => Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: AuroraColors.acc.withValues(alpha: 0.06),
                                border: const Border(
                                  left: BorderSide(
                                    color: AuroraColors.acc,
                                    width: 3,
                                  ),
                                ),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                q,
                                style: const TextStyle(
                                  fontSize: 12.5,
                                  color: Colors.white70,
                                  fontStyle: FontStyle.italic,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                if (seiyuu != null)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                    child: GestureDetector(
                      onTap: () => _t('Сэйю: ${seiyuu.name}'),
                      child: Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AuroraColors.sub,
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: AuroraColors.brd),
                        ),
                        child: Row(
                          children: [
                            Container(
                              width: 44,
                              height: 44,
                              decoration: const BoxDecoration(
                                shape: BoxShape.circle,
                                color: AuroraColors.surfaceVariant,
                              ),
                              child: Center(
                                child: Text(
                                  seiyuu.name[0],
                                  style: const TextStyle(
                                    fontSize: 18,
                                    color: Colors.white70,
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${seiyuu.name} · сэйю',
                                    style: const TextStyle(
                                      fontSize: 12.5,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white,
                                    ),
                                  ),
                                  Text(
                                    '${seiyuu.jp} · ${seiyuu.roles} ролей',
                                    style: const TextStyle(
                                      fontSize: 10.5,
                                      color: AuroraColors.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                if (appearsIn.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Появляется в',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 8),
                        ...appearsIn.map(
                          (it) => GestureDetector(
                            onTap: () => widget.onOpenTitle(it),
                            child: Padding(
                              padding: const EdgeInsets.only(bottom: 8),
                              child: Row(
                                children: [
                                  ClipRRect(
                                    borderRadius: BorderRadius.circular(8),
                                    child: Container(
                                      width: 40,
                                      height: 40,
                                      decoration: BoxDecoration(
                                        gradient: TakamiDB.franchiseOf(
                                          it.id,
                                        ).bg,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text(
                                      TakamiDB.franchiseOf(it.id).title,
                                      style: const TextStyle(
                                        fontSize: 12.5,
                                        color: Colors.white,
                                      ),
                                    ),
                                  ),
                                  const TakamiIcon(
                                    TIcon.chevron,
                                    size: 14,
                                    color: AuroraColors.onSurfaceVariant,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _infoBlock(String label, String text, {bool isBio = false}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: Colors.white,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          text,
          style: const TextStyle(
            fontSize: 12.5,
            color: Colors.white70,
            height: 1.55,
          ),
        ),
      ],
    );
  }
}
