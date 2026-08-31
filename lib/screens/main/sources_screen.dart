// Sources — управление источниками-парсерами. Портирован из kit/Sources.jsx
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class SourcesScreen extends StatefulWidget {
  final VoidCallback onBack;
  const SourcesScreen({super.key, required this.onBack});

  @override
  State<SourcesScreen> createState() => _SourcesScreenState();
}

class _SourcesScreenState extends State<SourcesScreen> {
  String tab = 'installed';
  late Map<String, bool> enabled;
  bool addOpen = false;
  final _urlCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    enabled = {for (final s in TakamiDB.sources) s.name: s.active};
  }

  void _t(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TakamiAppBar(
          onBack: widget.onBack,
          title: 'Источники',
          actions: [
            TakamiAction(
              icon: TIcon.plus,
              onClick: () => setState(() => addOpen = true),
            ),
          ],
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              FilterChip2(
                label: 'Установленные',
                selected: tab == 'installed',
                onTap: () => setState(() => tab = 'installed'),
              ),
              FilterChip2(
                label: 'Каталог',
                selected: tab == 'catalog',
                onTap: () => setState(() => tab = 'catalog'),
              ),
              FilterChip2(
                label: 'Обновления · 2',
                selected: tab == 'updates',
                onTap: () => setState(() => tab = 'updates'),
              ),
            ],
          ),
        ),
        if (addOpen)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AuroraColors.sub,
                border: Border.all(color: AuroraColors.brd),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Expanded(
                        child: Text(
                          'Добавить источник',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      GestureDetector(
                        onTap: () => setState(() => addOpen = false),
                        child: const TakamiIcon(
                          TIcon.close,
                          size: 16,
                          color: Colors.white54,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _urlCtrl,
                    style: const TextStyle(fontSize: 12.5, color: Colors.white),
                    decoration: InputDecoration(
                      hintText:
                          'https://raw.githubusercontent.com/…/index.min.json',
                      hintStyle: const TextStyle(
                        fontSize: 11.5,
                        color: AuroraColors.onSurfaceVariant,
                      ),
                      filled: true,
                      fillColor: Colors.black26,
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: BorderSide.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => setState(() => addOpen = false),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: Colors.white70,
                            side: const BorderSide(color: AuroraColors.brd),
                          ),
                          child: const Text('Отмена'),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () {
                            setState(() => addOpen = false);
                            _urlCtrl.clear();
                            _t('Репозиторий подключён');
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AuroraColors.acc,
                            foregroundColor: Colors.white,
                          ),
                          child: const Text('Подключить'),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: TakamiDB.sources.map((s) {
              final isOn = enabled[s.name] ?? false;
              return GestureDetector(
                onTap: () => _t('${s.name} — карточка расширения'),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  child: Opacity(
                    opacity: s.active ? 1 : 0.5,
                    child: Row(
                      children: [
                        Container(
                          width: 36,
                          height: 36,
                          decoration: BoxDecoration(
                            color: AuroraColors.surfaceVariant,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Center(
                            child: Text(
                              s.name[0],
                              style: const TextStyle(
                                color: Colors.white70,
                                fontWeight: FontWeight.w700,
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
                                s.name,
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.white,
                                ),
                              ),
                              Text(
                                '${s.type} · v${s.version}${!s.active ? " · парсер сломан" : ""}',
                                style: const TextStyle(
                                  fontSize: 10.5,
                                  color: AuroraColors.onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Container(
                          width: 8,
                          height: 8,
                          margin: const EdgeInsets.only(right: 10),
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: s.status == 'err'
                                ? AuroraColors.error
                                : s.status == 'warn'
                                ? AuroraColors.warn
                                : AuroraColors.ok,
                          ),
                        ),
                        Switch(
                          value: isOn,
                          activeTrackColor: AuroraColors.acc,
                          onChanged: (v) {
                            setState(() => enabled[s.name] = v);
                            _t('${s.name}${v ? " включён" : " выключен"}');
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }
}
