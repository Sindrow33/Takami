import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';

class SplashStep extends StatefulWidget {
  const SplashStep({super.key});

  @override
  State<SplashStep> createState() => _SplashStepState();
}

class _SplashStepState extends State<SplashStep> with TickerProviderStateMixin {
  late AnimationController _breathCtrl;
  late AnimationController _loadCtrl;

  @override
  void initState() {
    super.initState();
    _breathCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2500),
    )..repeat(reverse: true);
    _loadCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1600),
    )..repeat();
  }

  @override
  void dispose() {
    _breathCtrl.dispose();
    _loadCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedBuilder(
              animation: _breathCtrl,
              builder: (context, child) {
                final scale = 1.0 + _breathCtrl.value * 0.05;
                return Transform.scale(
                  scale: scale,
                  child: Container(
                    width: 108,
                    height: 108,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(30),
                      gradient: const LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [AuroraColors.accGradA, AuroraColors.accGradB],
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0x995B3BE8),
                          blurRadius: 60 + _breathCtrl.value * 20,
                          offset: const Offset(0, 20),
                        ),
                        BoxShadow(
                          color: const Color(0x807C5CFF),
                          blurRadius: 60 + _breathCtrl.value * 20,
                        ),
                      ],
                    ),
                    child: Center(
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(18),
                        child: Image.asset(
                          'assets/images/logo.jpg',
                          width: 72,
                          height: 72,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 28),
            const Text(
              'Takami',
              style: TextStyle(
                fontFamily: AuroraFonts.display,
                fontWeight: FontWeight.w900,
                fontSize: 44,
                letterSpacing: -0.02 * 44,
                color: Colors.white,
                height: 1.0,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              '高見 · 見る',
              style: TextStyle(
                fontSize: 15,
                letterSpacing: 4.5,
                color: AuroraColors.acc2,
              ),
            ),
            const SizedBox(height: 16),
            const SizedBox(
              width: 260,
              child: Text(
                'Манга, аниме и ранобэ — в одном приложении, с общим прогрессом.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 13,
                  color: AuroraColors.onSurfaceVariant,
                  height: 1.4,
                ),
              ),
            ),
            const SizedBox(height: 28),
            SizedBox(
              width: 120,
              height: 3,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(2),
                child: Container(
                  color: Colors.white.withValues(alpha: 0.06),
                  child: AnimatedBuilder(
                    animation: _loadCtrl,
                    builder: (context, child) {
                      return Align(
                        alignment: Alignment(-1 + _loadCtrl.value * 3.5, 0),
                        child: Container(
                          width: 40,
                          height: 3,
                          decoration: const BoxDecoration(
                            gradient: LinearGradient(
                              colors: [
                                Colors.transparent,
                                AuroraColors.acc,
                                Colors.transparent,
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
