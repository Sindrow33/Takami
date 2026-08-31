// Player — видеоплеер аниме. Портирован из kit/Player.jsx
import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/takami_icon.dart';

class PlayerScreen extends StatefulWidget {
  final VoidCallback onBack;
  const PlayerScreen({super.key, required this.onBack});

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  bool playing = true;
  double t = 750;
  final total = 1440;
  String? sheet; // subs | audio | quality | speed
  String subs = 'ru';
  String audio = 'ru-ai';
  String quality = '1080p';
  double speed = 1;
  bool locked = false;
  bool pip = false;
  bool keepAwake = true;
  bool cast = false;

  String _fmt(double s) {
    final m = (s / 60).floor();
    final sec = (s % 60).floor();
    return '$m:${sec.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final showSkip = t > 60 && t < 180;
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          Container(
            width: double.infinity,
            height: double.infinity,
            color: const Color(0xFF0A0A0A),
            child: const Center(
              child: Text(
                'Видео',
                style: TextStyle(color: Colors.white24, fontSize: 20),
              ),
            ),
          ),
          Container(
            width: double.infinity,
            height: double.infinity,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.black.withValues(alpha: 0.5),
                  Colors.transparent,
                  Colors.black.withValues(alpha: 0.6),
                ],
                stops: const [0, 0.4, 1],
              ),
            ),
          ),
          if (!locked)
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                child: Row(
                  children: [
                    IconButton(
                      onPressed: widget.onBack,
                      icon: const TakamiIcon(TIcon.back, color: Colors.white),
                    ),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Эпизод 7 · «Название серии»',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: Colors.white,
                            ),
                          ),
                          Text(
                            'Аниме сериал · AniLibria · ⤓ скачано',
                            style: TextStyle(
                              fontSize: 10.5,
                              color: AuroraColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      onPressed: () => setState(() => cast = !cast),
                      icon: TakamiIcon(
                        TIcon.cast,
                        color: cast ? AuroraColors.acc : Colors.white,
                      ),
                    ),
                    IconButton(
                      onPressed: () => setState(() => pip = !pip),
                      icon: TakamiIcon(
                        TIcon.pip,
                        color: pip ? AuroraColors.acc : Colors.white,
                      ),
                    ),
                    IconButton(
                      onPressed: () => setState(() => keepAwake = !keepAwake),
                      icon: TakamiIcon(
                        TIcon.awake,
                        color: keepAwake ? AuroraColors.acc : Colors.white,
                      ),
                    ),
                    IconButton(
                      onPressed: () => setState(() => locked = true),
                      icon: const Icon(
                        Icons.lock_open,
                        color: Colors.white,
                        size: 20,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          if (!locked)
            Center(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    iconSize: 36,
                    onPressed: () =>
                        setState(() => t = (t - 10).clamp(0, total.toDouble())),
                    icon: const Icon(Icons.replay_10, color: Colors.white),
                  ),
                  const SizedBox(width: 24),
                  GestureDetector(
                    onTap: () => setState(() => playing = !playing),
                    child: Container(
                      width: 64,
                      height: 64,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white24,
                      ),
                      child: Icon(
                        playing ? Icons.pause : Icons.play_arrow,
                        color: Colors.white,
                        size: 32,
                      ),
                    ),
                  ),
                  const SizedBox(width: 24),
                  IconButton(
                    iconSize: 36,
                    onPressed: () =>
                        setState(() => t = (t + 10).clamp(0, total.toDouble())),
                    icon: const Icon(Icons.forward_10, color: Colors.white),
                  ),
                ],
              ),
            ),
          if (showSkip && !locked)
            Positioned(
              right: 16,
              bottom: 140,
              child: ElevatedButton(
                onPressed: () => setState(() => t = 180),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.black.withValues(alpha: 0.7),
                  foregroundColor: Colors.white,
                ),
                child: const Text(
                  'Пропустить опенинг ›',
                  style: TextStyle(fontSize: 11.5),
                ),
              ),
            ),
          if (!locked)
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Text(
                            _fmt(t),
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.white70,
                              fontFeatures: [FontFeature.tabularFigures()],
                            ),
                          ),
                          Expanded(
                            child: Slider(
                              value: t,
                              min: 0,
                              max: total.toDouble(),
                              activeColor: AuroraColors.acc,
                              inactiveColor: Colors.white24,
                              onChanged: (v) => setState(() => t = v),
                            ),
                          ),
                          Text(
                            _fmt(total.toDouble()),
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.white70,
                              fontFeatures: [FontFeature.tabularFigures()],
                            ),
                          ),
                        ],
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _pillBtn(
                            'Субтитры',
                            () => setState(() => sheet = 'subs'),
                          ),
                          _pillBtn(
                            'Аудио',
                            () => setState(() => sheet = 'audio'),
                          ),
                          _pillBtn(
                            quality,
                            () => setState(() => sheet = 'quality'),
                          ),
                          _pillBtn(
                            '${speed}x',
                            () => setState(() => sheet = 'speed'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          if (locked)
            Positioned(
              bottom: 40,
              right: 24,
              child: IconButton(
                onPressed: () => setState(() => locked = false),
                icon: const Icon(Icons.lock, color: Colors.white54, size: 28),
              ),
            ),
          if (sheet != null)
            Positioned.fill(
              child: GestureDetector(
                onTap: () => setState(() => sheet = null),
                child: Container(
                  color: Colors.black54,
                  alignment: Alignment.bottomCenter,
                  child: GestureDetector(onTap: () {}, child: _buildSheet()),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _pillBtn(String label, VoidCallback onTap) {
    return TextButton(
      onPressed: onTap,
      style: TextButton.styleFrom(
        backgroundColor: Colors.white.withValues(alpha: 0.1),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 11, color: Colors.white),
      ),
    );
  }

  Widget _buildSheet() {
    final List<(String, String)> items;
    String value;
    void Function(String) onSelect;
    switch (sheet) {
      case 'subs':
        items = const [
          ('off', 'Отключены'),
          ('ru', 'Русские (Anilibria)'),
          ('en', 'English (Official)'),
          ('ru-forced', 'Русские · только надписи'),
        ];
        value = subs;
        onSelect = (v) => setState(() {
          subs = v;
          sheet = null;
        });
        break;
      case 'audio':
        items = const [
          ('ja', 'Оригинал'),
          ('ru-dub', 'Русский · Anidub'),
          ('ru-vo', 'Русский · Anilibria'),
          ('ru-ai', 'Русский · ИИ-озвучка'),
        ];
        value = audio;
        onSelect = (v) => setState(() {
          audio = v;
          sheet = null;
        });
        break;
      case 'quality':
        items = const [
          ('4K', '4K'),
          ('1080p', '1080p'),
          ('720p', '720p'),
          ('480p', '480p'),
          ('auto', 'Авто'),
        ];
        value = quality;
        onSelect = (v) => setState(() {
          quality = v;
          sheet = null;
        });
        break;
      default:
        items = const [
          ('0.5', '0.5x'),
          ('0.75', '0.75x'),
          ('1', '1x'),
          ('1.25', '1.25x'),
          ('1.5', '1.5x'),
          ('2', '2x'),
        ];
        value = '$speed';
        onSelect = (v) => setState(() {
          speed = double.parse(v);
          sheet = null;
        });
    }

    return Container(
      decoration: const BoxDecoration(
        color: AuroraColors.surfaceContainer,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: items.map((it) {
          final (k, n) = it;
          final isOn = k == value;
          return GestureDetector(
            onTap: () => onSelect(k),
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 12),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      n,
                      style: TextStyle(
                        fontSize: 13,
                        color: isOn ? AuroraColors.acc2 : Colors.white,
                      ),
                    ),
                  ),
                  if (isOn)
                    const TakamiIcon(
                      TIcon.check,
                      size: 16,
                      color: AuroraColors.acc2,
                    ),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}
