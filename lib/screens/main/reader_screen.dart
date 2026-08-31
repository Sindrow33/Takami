// Reader — ридер манги. Портирован из kit/Reader.jsx
import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class ReaderScreen extends StatefulWidget {
  final VoidCallback onBack;
  const ReaderScreen({super.key, required this.onBack});

  @override
  State<ReaderScreen> createState() => _ReaderScreenState();
}

class _ReaderScreenState extends State<ReaderScreen> {
  bool uiOpen = true;
  bool settingsOpen = false;
  double page = 3;
  final total = 18;

  String mode = 'webtoon'; // webtoon | ltr | rtl | double
  double brightness = 85;
  bool autoScroll = false;
  bool crop = true;
  String tint = 'off';
  double fontSize = 16;
  bool keepAwake = true;
  bool tapZones = true;

  Color get _tintBg => switch (tint) {
    'sepia' => const Color(0xFF2A1F14),
    'dark' => const Color(0xFF0A0A0A),
    _ => Colors.black,
  };

  final _heights = const [
    180.0,
    260,
    210,
    280,
    200,
    250,
    230,
    190,
    270,
    200,
    260,
    220,
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          GestureDetector(
            onTap: () => setState(() => uiOpen = !uiOpen),
            behavior: HitTestBehavior.translucent,
            child: Container(
              width: double.infinity,
              height: double.infinity,
              color: _tintBg,
            ),
          ),
          Positioned.fill(
            child: Opacity(
              opacity: brightness / 100,
              child: ListView.builder(
                padding: EdgeInsets.zero,
                itemCount: _heights.length,
                itemBuilder: (context, i) => Container(
                  height: _heights[i].toDouble(),
                  margin: const EdgeInsets.symmetric(vertical: 1),
                  color: const Color(0xFF15161C),
                  alignment: Alignment.center,
                  child: Text(
                    '${i + 1}',
                    style: const TextStyle(color: Colors.white24, fontSize: 24),
                  ),
                ),
              ),
            ),
          ),
          if (uiOpen && !settingsOpen)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: SafeArea(
                child: Container(
                  color: Colors.black.withValues(alpha: 0.6),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 4,
                    vertical: 4,
                  ),
                  child: Row(
                    children: [
                      IconButton(
                        onPressed: widget.onBack,
                        icon: const TakamiIcon(TIcon.back, color: Colors.white),
                      ),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            Text(
                              'Глава 42',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: Colors.white,
                              ),
                            ),
                            Text(
                              'Тайтл с длинным названием',
                              style: TextStyle(
                                fontSize: 10.5,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        onPressed: () => setState(() => settingsOpen = true),
                        icon: const TakamiIcon(
                          TIcon.settings,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          if (uiOpen && !settingsOpen)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: SafeArea(
                child: Container(
                  color: Colors.black.withValues(alpha: 0.6),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 10,
                  ),
                  child: Column(
                    children: [
                      Slider(
                        value: page,
                        min: 1,
                        max: total.toDouble(),
                        activeColor: AuroraColors.acc,
                        onChanged: (v) => setState(() => page = v),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          TextButton.icon(
                            onPressed: () => setState(
                              () =>
                                  page = (page - 1).clamp(1, total.toDouble()),
                            ),
                            icon: const TakamiIcon(
                              TIcon.arrowL,
                              size: 14,
                              color: Colors.white70,
                            ),
                            label: const Text(
                              'Пред.',
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.white70,
                              ),
                            ),
                          ),
                          Text(
                            '${page.round()} / $total',
                            style: const TextStyle(
                              fontSize: 11,
                              color: AuroraColors.onSurfaceVariant,
                            ),
                          ),
                          TextButton.icon(
                            onPressed: () => setState(
                              () =>
                                  page = (page + 1).clamp(1, total.toDouble()),
                            ),
                            icon: const TakamiIcon(
                              TIcon.arrowR,
                              size: 14,
                              color: Colors.white70,
                            ),
                            label: const Text(
                              'След.',
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.white70,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          if (!settingsOpen && mode == 'webtoon')
            Positioned(
              right: 16,
              bottom: 100,
              child: GestureDetector(
                onTap: () => setState(() => autoScroll = !autoScroll),
                child: Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: autoScroll
                        ? AuroraColors.acc
                        : Colors.black.withValues(alpha: 0.6),
                    border: Border.all(color: Colors.white24),
                  ),
                  child: Icon(
                    autoScroll ? Icons.pause : Icons.play_arrow,
                    color: Colors.white,
                    size: 20,
                  ),
                ),
              ),
            ),
          if (settingsOpen)
            Positioned.fill(
              child: GestureDetector(
                onTap: () => setState(() => settingsOpen = false),
                child: Container(
                  color: Colors.black54,
                  alignment: Alignment.bottomCenter,
                  child: GestureDetector(
                    onTap: () {},
                    child: Container(
                      decoration: const BoxDecoration(
                        color: AuroraColors.surfaceContainer,
                        borderRadius: BorderRadius.vertical(
                          top: Radius.circular(24),
                        ),
                      ),
                      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Expanded(
                                child: Text(
                                  'Настройки ридера',
                                  style: TextStyle(
                                    fontSize: 15,
                                    fontWeight: FontWeight.w700,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                              IconButton(
                                onPressed: () =>
                                    setState(() => settingsOpen = false),
                                icon: const TakamiIcon(
                                  TIcon.close,
                                  color: Colors.white,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          SegRow<String>(
                            label: 'Режим чтения',
                            value: mode,
                            options: const [
                              ('webtoon', 'Свиток'),
                              ('ltr', 'LTR'),
                              ('rtl', 'RTL'),
                              ('double', 'Разворот'),
                            ],
                            onChanged: (v) => setState(() => mode = v),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 8),
                            child: Row(
                              children: [
                                const SizedBox(
                                  width: 100,
                                  child: Text(
                                    'Яркость',
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      color: Colors.white,
                                    ),
                                  ),
                                ),
                                Expanded(
                                  child: Slider(
                                    value: brightness,
                                    min: 20,
                                    max: 100,
                                    activeColor: AuroraColors.acc,
                                    onChanged: (v) =>
                                        setState(() => brightness = v),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          SegRow<String>(
                            label: 'Оттенок',
                            value: tint,
                            options: const [
                              ('off', 'Выкл'),
                              ('sepia', 'Сепия'),
                              ('dark', 'Тёмный'),
                            ],
                            onChanged: (v) => setState(() => tint = v),
                          ),
                          SwitchRow(
                            label: 'Обрезать поля',
                            value: crop,
                            onChanged: (v) => setState(() => crop = v),
                          ),
                          SwitchRow(
                            label: 'Держать экран включённым',
                            value: keepAwake,
                            onChanged: (v) => setState(() => keepAwake = v),
                          ),
                          SwitchRow(
                            label: 'Тап-зоны для навигации',
                            value: tapZones,
                            onChanged: (v) => setState(() => tapZones = v),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
