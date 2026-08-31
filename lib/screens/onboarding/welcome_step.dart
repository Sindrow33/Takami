// WelcomeStep — welcome-экран с аниме-девушкой (v4.1).
// Портирован из kit/Onboarding.jsx + kit/patches.css
import 'dart:math';
import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';

class WelcomeStep extends StatefulWidget {
  final VoidCallback onEnter;
  const WelcomeStep({super.key, required this.onEnter});

  @override
  State<WelcomeStep> createState() => _WelcomeStepState();
}

class _WelcomeStepState extends State<WelcomeStep>
    with TickerProviderStateMixin {
  late AnimationController _haloCtrl;
  late AnimationController _halo2Ctrl;
  late AnimationController _kanaCtrl;
  late AnimationController _sparkleCtrl;
  late AnimationController _girlBreathCtrl;
  late AnimationController _girlSwayCtrl;
  late AnimationController _bubblePopCtrl;
  late AnimationController _bubbleWiggleCtrl;
  late AnimationController _ctaFadeCtrl;
  late AnimationController _ctaPulseCtrl;

  @override
  void initState() {
    super.initState();
    _haloCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3400),
    )..repeat(reverse: true);
    _halo2Ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 4200),
    );
    Future.delayed(const Duration(milliseconds: 1200), () {
      if (mounted) _halo2Ctrl.repeat(reverse: true);
    });
    _kanaCtrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 10),
    )..repeat();
    _sparkleCtrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 5),
    )..repeat();
    _girlBreathCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3600),
    )..repeat(reverse: true);
    _girlSwayCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 4800),
    )..repeat(reverse: true);
    _bubblePopCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _bubbleWiggleCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3600),
    );
    _ctaFadeCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _ctaPulseCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2500),
    );

    Future.delayed(const Duration(milliseconds: 400), () {
      if (mounted) _bubblePopCtrl.forward();
    });
    Future.delayed(const Duration(milliseconds: 900), () {
      if (mounted) _bubbleWiggleCtrl.repeat(reverse: true);
    });
    Future.delayed(const Duration(milliseconds: 1200), () {
      if (mounted) _ctaFadeCtrl.forward();
    });
    Future.delayed(const Duration(milliseconds: 2000), () {
      if (mounted) _ctaPulseCtrl.repeat(reverse: true);
    });
  }

  @override
  void dispose() {
    _haloCtrl.dispose();
    _halo2Ctrl.dispose();
    _kanaCtrl.dispose();
    _sparkleCtrl.dispose();
    _girlBreathCtrl.dispose();
    _girlSwayCtrl.dispose();
    _bubblePopCtrl.dispose();
    _bubbleWiggleCtrl.dispose();
    _ctaFadeCtrl.dispose();
    _ctaPulseCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    return Stack(
      fit: StackFit.expand,
      children: [
        // Задние ореолы
        Positioned(
          top: size.height * 0.28,
          left: size.width * 0.5 - 170,
          child: AnimatedBuilder(
            animation: _haloCtrl,
            builder: (context, child) {
              final s = 0.94 + _haloCtrl.value * 0.16;
              final o = 0.7 + _haloCtrl.value * 0.3;
              return Opacity(
                opacity: o.clamp(0, 1),
                child: Transform.scale(
                  scale: s,
                  child: Container(
                    width: 340,
                    height: 340,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          const Color(0x8C7C5CFF),
                          const Color(0x007C5CFF),
                        ],
                        stops: const [0, 0.75],
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
        Positioned(
          top: size.height * 0.38,
          left: size.width * 0.5 - 110,
          child: AnimatedBuilder(
            animation: _halo2Ctrl,
            builder: (context, child) {
              final s = 0.94 + _halo2Ctrl.value * 0.16;
              return Transform.scale(
                scale: s,
                child: Container(
                  width: 220,
                  height: 220,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: RadialGradient(
                      colors: [Color(0x59FF96BE), Color(0x00FF96BE)],
                    ),
                  ),
                ),
              );
            },
          ),
        ),

        // Иероглифы
        _kanaSpan(
          'お',
          top: size.height * 0.06,
          left: -18,
          fontSize: 220,
          color: Colors.white.withValues(alpha: 0.09),
          phase: 0,
        ),
        _kanaSpan(
          '帰',
          top: size.height * 0.18,
          right: -10,
          fontSize: 180,
          color: Colors.white.withValues(alpha: 0.09),
          phase: -3.3,
        ),
        _kanaSpan(
          'り',
          bottom: size.height * 0.24,
          left: size.width * 0.4,
          fontSize: 160,
          color: const Color(0x0FFF96BE),
          phase: -6.6,
        ),

        // Искры
        ...List.generate(8, (i) => _sparkle(i, size)),

        // Девушка
        Positioned(
          bottom: 0,
          left: size.width * 0.5 - 180,
          child: AnimatedBuilder(
            animation: _girlBreathCtrl,
            builder: (context, child) {
              final ty = -_girlBreathCtrl.value * 2;
              final s = 1.0 + _girlBreathCtrl.value * 0.012;
              return Transform.translate(
                offset: Offset(0, ty),
                child: Transform.scale(
                  scale: s,
                  alignment: Alignment.bottomCenter,
                  child: SizedBox(
                    width: 360,
                    height: 640,
                    child: AnimatedBuilder(
                      animation: _girlSwayCtrl,
                      builder: (context, child) {
                        final rot =
                            (-1.2 + _girlSwayCtrl.value * 2.4) * pi / 180;
                        return Transform.rotate(
                          angle: rot,
                          alignment: const Alignment(0, 1),
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              boxShadow: [
                                BoxShadow(
                                  color: const Color(0x8C7C5CFF),
                                  blurRadius: 40,
                                  offset: const Offset(0, 8),
                                ),
                                BoxShadow(
                                  color: const Color(0x59A78BFA),
                                  blurRadius: 24,
                                ),
                              ],
                            ),
                            child: Image.asset(
                              'assets/images/welcome_girl.png',
                              fit: BoxFit.contain,
                              alignment: Alignment.bottomCenter,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
              );
            },
          ),
        ),

        // Речевой пузырёк
        Positioned(
          top: size.height * 0.15,
          right: 16,
          child: AnimatedBuilder(
            animation: Listenable.merge([_bubblePopCtrl, _bubbleWiggleCtrl]),
            builder: (context, child) {
              final pop = _bubblePopCtrl.value;
              double scale;
              double rotate;
              if (pop < 0.6) {
                final t = pop / 0.6;
                scale = t * 1.08;
                rotate = -10 + t * 12;
              } else {
                final t = (pop - 0.6) / 0.4;
                scale = 1.08 - t * 0.08;
                rotate = 2 - t * 2;
              }
              final wiggle = (-1.5 + _bubbleWiggleCtrl.value * 3.0) * pi / 180;
              return Opacity(
                opacity: pop.clamp(0, 1),
                child: Transform.scale(
                  scale: scale.clamp(0, 1.2),
                  child: Transform.rotate(
                    angle: rotate * pi / 180 + wiggle,
                    child: Container(
                      constraints: const BoxConstraints(maxWidth: 200),
                      padding: const EdgeInsets.fromLTRB(18, 14, 18, 16),
                      decoration: BoxDecoration(
                        gradient: AuroraColors.bubbleGradient,
                        borderRadius: const BorderRadius.only(
                          topLeft: Radius.circular(20),
                          topRight: Radius.circular(20),
                          bottomLeft: Radius.circular(20),
                          bottomRight: Radius.circular(4),
                        ),
                        boxShadow: AuroraShadows.welcomeBubble,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text(
                            'お帰りなさいませ',
                            style: TextStyle(
                              fontFamily: AuroraFonts.display,
                              fontWeight: FontWeight.w500,
                              fontSize: 11,
                              color: AuroraColors.acc,
                              letterSpacing: 1.3,
                            ),
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'Добро пожаловать,\nхозяин!',
                            style: TextStyle(
                              fontFamily: AuroraFonts.display,
                              fontWeight: FontWeight.w700,
                              fontSize: 17,
                              height: 1.2,
                              color: Color(0xFF0F1116),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),

        // Футер
        Align(
          alignment: Alignment.bottomCenter,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(24, 60, 24, 28),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.transparent,
                  Color(0x8C0A0C10),
                  Color(0xEB0A0C10),
                  Color(0xFF0A0C10),
                ],
                stops: [0, 0.25, 0.7, 1],
              ),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'Всё готово. Приятного чтения.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 13,
                    color: Color(0xB8FFFFFF),
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 16),
                AnimatedBuilder(
                  animation: Listenable.merge([_ctaFadeCtrl, _ctaPulseCtrl]),
                  builder: (context, child) {
                    final fadeOpacity = _ctaFadeCtrl.value;
                    final fadeY = (1 - _ctaFadeCtrl.value) * 12;
                    final pulse = _ctaPulseCtrl.value;
                    final shadows = [
                      BoxShadow(
                        color: Color.lerp(
                          const Color(0x805B3BE8),
                          const Color(0xBF5B3BE8),
                          pulse,
                        )!,
                        blurRadius: 24 + pulse * 8,
                        offset: Offset(0, 8 + pulse * 4),
                      ),
                      BoxShadow(
                        color: Color.lerp(
                          const Color(0x597C5CFF),
                          const Color(0x997C5CFF),
                          pulse,
                        )!,
                        blurRadius: 24 + pulse * 12,
                      ),
                    ];
                    return Opacity(
                      opacity: fadeOpacity,
                      child: Transform.translate(
                        offset: Offset(0, fadeY),
                        child: SizedBox(
                          width: double.infinity,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(14),
                              boxShadow: shadows,
                            ),
                            child: ElevatedButton(
                              onPressed: widget.onEnter,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: AuroraColors.acc,
                                foregroundColor: Colors.white,
                                padding: const EdgeInsets.symmetric(
                                  vertical: 15,
                                ),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(14),
                                ),
                                elevation: 0,
                              ),
                              child: const Text(
                                'Войти в приложение',
                                style: TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _kanaSpan(
    String char, {
    double? top,
    double? bottom,
    double? left,
    double? right,
    required double fontSize,
    required Color color,
    required double phase,
  }) {
    return Positioned(
      top: top,
      bottom: bottom,
      left: left,
      right: right,
      child: AnimatedBuilder(
        animation: _kanaCtrl,
        builder: (context, child) {
          final t = ((_kanaCtrl.value * 10 + phase) % 10) / 10;
          final dy = sin(t * 2 * pi) * 12;
          final rot = sin(t * 2 * pi) * 5 * pi / 180;
          return Transform.translate(
            offset: Offset(0, dy),
            child: Transform.rotate(
              angle: rot,
              child: Text(
                char,
                style: TextStyle(
                  fontFamily: AuroraFonts.display,
                  fontWeight: FontWeight.w900,
                  fontSize: fontSize,
                  color: color,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _sparkle(int i, Size size) {
    final rnd = Random(i * 91);
    final startX = rnd.nextDouble() * size.width;
    final startY = size.height * (0.3 + rnd.nextDouble() * 0.4);
    final sizePx = [3.0, 4.0, 5.0][i % 3];
    final colors = [Colors.white, const Color(0xFFFFB0D0), AuroraColors.acc2];
    final color = colors[i % 3];
    final delay = -i * 0.7;

    return Positioned(
      left: startX,
      top: startY,
      child: AnimatedBuilder(
        animation: _sparkleCtrl,
        builder: (context, child) {
          final t = ((_sparkleCtrl.value * 5 + delay) % 5) / 5;
          final dy = -t * 60;
          final opacity = sin(t * pi);
          return Opacity(
            opacity: opacity.clamp(0, 1),
            child: Transform.translate(
              offset: Offset(sin(t * pi * 2) * 8, dy),
              child: Container(
                width: sizePx,
                height: sizePx,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: color,
                  boxShadow: [
                    BoxShadow(
                      color: color.withValues(alpha: 0.8),
                      blurRadius: 6,
                    ),
                    BoxShadow(
                      color: color.withValues(alpha: 0.5),
                      blurRadius: 14,
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
