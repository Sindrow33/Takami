// SupportButton — широкая кнопка «Поддержать» в самом низу настроек.
// Портирован из kit/SupportButton.jsx
import 'package:flutter/material.dart';
import '../theme/aurora_theme.dart';
import 'common.dart';
import 'takami_icon.dart';

class SupportButton extends StatefulWidget {
  const SupportButton({super.key});

  @override
  State<SupportButton> createState() => _SupportButtonState();
}

class _SupportButtonState extends State<SupportButton>
    with SingleTickerProviderStateMixin {
  bool open = false;
  late AnimationController _heartCtrl;

  final links = const [
    (
      'Boosty',
      'Ежемесячная подписка · от 100 ₽',
      [Color(0xFFFF5A26), Color(0xFFE23000)],
      'B',
    ),
    (
      'Tribute',
      'Через Telegram · разовые донаты',
      [Color(0xFF38B6FF), Color(0xFF0079E5)],
      'T',
    ),
    (
      'Patreon',
      'Ежемесячная подписка · в USD',
      [Color(0xFFFF6249), Color(0xFFE23A20)],
      'P',
    ),
    (
      'Ko-fi',
      'Одноразовые донаты · чашка кофе',
      [Color(0xFFFF5E5B), Color(0xFFD93A3F)],
      'K',
    ),
    (
      'Криптовалюта',
      'BTC · ETH · TON · USDT',
      [Color(0xFFF7931A), Color(0xFFB87513)],
      '₿',
    ),
  ];

  @override
  void initState() {
    super.initState();
    _heartCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat();
  }

  @override
  void dispose() {
    _heartCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 40),
      child: Column(
        children: [
          GestureDetector(
            onTap: () => setState(() => open = !open),
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.02),
                border: Border.all(color: const Color(0x597C5CFF), width: 1.5),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  AnimatedBuilder(
                    animation: _heartCtrl,
                    builder: (context, child) {
                      final t = _heartCtrl.value;
                      double scale;
                      double rot;
                      if (t < 0.15) {
                        scale = 1 + (t / 0.15) * 0.18;
                        rot = -6;
                      } else if (t < 0.30) {
                        scale = 1.18 - ((t - 0.15) / 0.15) * 0.18;
                        rot = -6;
                      } else if (t < 0.45) {
                        scale = 1 + ((t - 0.30) / 0.15) * 0.12;
                        rot = -6 + ((t - 0.30) / 0.15) * 12;
                      } else if (t < 0.60) {
                        scale = 1.12 - ((t - 0.45) / 0.15) * 0.12;
                        rot = 6;
                      } else {
                        scale = 1;
                        rot = 6 - ((t - 0.60) / 0.40) * 12;
                      }
                      return Transform.rotate(
                        angle: rot * 3.14159 / 180,
                        child: Transform.scale(
                          scale: scale,
                          child: Container(
                            decoration: BoxDecoration(
                              boxShadow: [
                                BoxShadow(
                                  color: AuroraColors.heartDonate.withValues(
                                    alpha: 0.55,
                                  ),
                                  blurRadius: 8,
                                ),
                              ],
                            ),
                            child: const TakamiIcon(
                              TIcon.heartFilled,
                              size: 22,
                              color: AuroraColors.heartDonate,
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Text(
                      'Поддержать разработку',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFFE4DAFF),
                      ),
                    ),
                  ),
                  AnimatedRotation(
                    turns: open ? 0.5 : 0,
                    duration: const Duration(milliseconds: 280),
                    child: const TakamiIcon(
                      TIcon.chevron,
                      size: 16,
                      color: Color(0xFFE4DAFF),
                    ),
                  ),
                ],
              ),
            ),
          ),
          AnimatedCrossFade(
            duration: const Duration(milliseconds: 320),
            crossFadeState: open
                ? CrossFadeState.showFirst
                : CrossFadeState.showSecond,
            firstChild: Container(
              margin: const EdgeInsets.only(top: 8),
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.02),
                border: Border.all(color: AuroraColors.brd),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Column(
                children: links.map((l) {
                  final (name, sub, colors, letter) = l;
                  return GestureDetector(
                    onTap: () =>
                        ToastController.show(context, 'Открываем $name …'),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 12,
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 34,
                            height: 34,
                            decoration: BoxDecoration(
                              gradient: LinearGradient(colors: colors),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Center(
                              child: Text(
                                letter,
                                style: const TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.w700,
                                  color: Colors.white,
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
                                  name,
                                  style: const TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600,
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
                          const Text(
                            '›',
                            style: TextStyle(
                              fontSize: 16,
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
            secondChild: const SizedBox(width: double.infinity),
          ),
        ],
      ),
    );
  }
}
