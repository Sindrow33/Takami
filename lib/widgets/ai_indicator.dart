// AiIndicator — индикатор обучаемости автопарсера.
// Портирован из kit/AiIndicator.jsx
import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../theme/aurora_theme.dart';
import 'takami_icon.dart';

class AiIndicator extends StatefulWidget {
  const AiIndicator({super.key});

  @override
  State<AiIndicator> createState() => _AiIndicatorState();
}

class _AiIndicatorState extends State<AiIndicator> {
  int pct = 72;
  Timer? _timer;
  final _rnd = Random();

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 8), (_) {
      if (_rnd.nextDouble() > 0.6 && pct < 99) {
        setState(() => pct++);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _openSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => _AiSheet(pct: pct),
    );
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _openSheet,
      child: Container(
        height: 32,
        padding: const EdgeInsets.fromLTRB(6, 4, 10, 4),
        margin: const EdgeInsets.only(right: 6),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0x387C5CFF), Color(0x145B3BE8)],
          ),
          border: Border.all(color: const Color(0x597C5CFF)),
          borderRadius: BorderRadius.circular(AuroraRadii.full),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 20,
              height: 20,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  colors: [AuroraColors.acc2, AuroraColors.accDim],
                ),
              ),
              child: const Padding(
                padding: EdgeInsets.all(3.5),
                child: TakamiIcon(TIcon.brain, size: 13, color: Colors.white),
              ),
            ),
            const SizedBox(width: 6),
            const _MiniBars(),
            const SizedBox(width: 6),
            Text(
              '$pct%',
              style: const TextStyle(
                fontSize: 10.5,
                fontWeight: FontWeight.w600,
                color: Color(0xFFE4DAFF),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MiniBars extends StatefulWidget {
  const _MiniBars();

  @override
  State<_MiniBars> createState() => _MiniBarsState();
}

class _MiniBarsState extends State<_MiniBars> with TickerProviderStateMixin {
  late List<AnimationController> _ctrls;

  @override
  void initState() {
    super.initState();
    _ctrls = List.generate(5, (i) {
      final c = AnimationController(
        vsync: this,
        duration: const Duration(milliseconds: 2000),
      );
      Future.delayed(Duration(milliseconds: (i * 150)), () {
        if (mounted) c.repeat(reverse: true);
      });
      return c;
    });
  }

  @override
  void dispose() {
    for (final c in _ctrls) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 12,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(5, (i) {
          return AnimatedBuilder(
            animation: _ctrls[i],
            builder: (context, child) {
              final scale = 0.6 + _ctrls[i].value * 0.4;
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 1),
                child: Align(
                  alignment: Alignment.bottomCenter,
                  child: Transform.scale(
                    scaleY: scale,
                    alignment: Alignment.bottomCenter,
                    child: Container(
                      width: 3,
                      height: 12,
                      decoration: BoxDecoration(
                        color: AuroraColors.acc2,
                        borderRadius: BorderRadius.circular(1),
                      ),
                    ),
                  ),
                ),
              );
            },
          );
        }),
      ),
    );
  }
}

class _AiSheet extends StatelessWidget {
  final int pct;
  const _AiSheet({required this.pct});

  @override
  Widget build(BuildContext context) {
    final stats = [
      ('14', 'Источников', 'парсеров активно', null),
      ('38', 'Самопочинок', 'за 30 дней', AuroraColors.ok),
      ('96%', 'Точность', 'по последним 500 запросам', AuroraColors.ok),
      ('2', 'Аномалий', 'ждут разметки', AuroraColors.warn),
    ];
    final log = [
      ('3 мин', 'ReadManga · сменилась структура кнопок глав', 'w'),
      ('18 мин', 'AniLibria · автовосстановление плейлиста', 'ok'),
      ('1 ч', 'RanobeLib · обновлена модель парсинга v2.14', 't'),
      ('3 ч', 'Shikimori · синхронизация трекера', 'ok'),
      ('7 ч', 'MintManga · перебалансирован таймаут (12→14 c)', 't'),
    ];

    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFF1A1D25), Color(0xFF12141A)],
        ),
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        border: Border(top: BorderSide(color: Color(0x597C5CFF), width: 1)),
      ),
      padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    colors: [AuroraColors.acc2, AuroraColors.accDim],
                  ),
                ),
                child: const Padding(
                  padding: EdgeInsets.all(8),
                  child: TakamiIcon(TIcon.brain, size: 22, color: Colors.white),
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Автопарсер · обучаемость',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      'Самовосстанавливающийся движок. Учится на каждом запросе.',
                      style: TextStyle(
                        fontSize: 11.5,
                        color: AuroraColors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const TakamiIcon(TIcon.close, color: Colors.white),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              SizedBox(
                width: 92,
                height: 92,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    CustomPaint(
                      size: const Size(92, 92),
                      painter: _RingPainter(pct / 100),
                    ),
                    RichText(
                      text: TextSpan(
                        children: [
                          TextSpan(
                            text: '$pct',
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w700,
                              color: Colors.white,
                            ),
                          ),
                          const TextSpan(
                            text: '%',
                            style: TextStyle(
                              fontSize: 13,
                              color: AuroraColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Средний уровень уверенности',
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    SizedBox(height: 6),
                    Text(
                      'Модель учится на успешных и провальных парсингах, подстраивает селекторы и таймауты. Данные не покидают устройство.',
                      style: TextStyle(
                        fontSize: 11.5,
                        color: AuroraColors.onSurfaceVariant,
                        height: 1.4,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 8,
            crossAxisSpacing: 8,
            childAspectRatio: 2.6,
            children: stats.map((s) {
              final (v, l, hint, tone) = s;
              return Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AuroraColors.sub,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      v,
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: tone ?? Colors.white,
                      ),
                    ),
                    Text(
                      '$l · $hint',
                      style: const TextStyle(
                        fontSize: 10.5,
                        color: AuroraColors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(10),
            constraints: const BoxConstraints(maxHeight: 110),
            decoration: BoxDecoration(
              color: const Color(0x47000000),
              borderRadius: BorderRadius.circular(10),
            ),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: log.map((l) {
                  final (t, m, k) = l;
                  final color = k == 'ok'
                      ? AuroraColors.ok
                      : k == 'w'
                      ? AuroraColors.warn
                      : Colors.white70;
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: RichText(
                      text: TextSpan(
                        style: const TextStyle(
                          fontFamily: AuroraFonts.mono,
                          fontSize: 10.5,
                        ),
                        children: [
                          TextSpan(
                            text: '[$t] ',
                            style: const TextStyle(color: AuroraColors.acc2),
                          ),
                          TextSpan(
                            text: m,
                            style: TextStyle(color: color),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _RingPainter extends CustomPainter {
  final double progress;
  _RingPainter(this.progress);

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final radius = size.width / 2 - 4;
    final bg = Paint()
      ..color = Colors.white.withValues(alpha: 0.08)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 8;
    canvas.drawCircle(center, radius, bg);

    final fg = Paint()
      ..shader = const LinearGradient(
        colors: [AuroraColors.acc2, AuroraColors.accDim],
      ).createShader(Rect.fromCircle(center: center, radius: radius))
      ..style = PaintingStyle.stroke
      ..strokeWidth = 8
      ..strokeCap = StrokeCap.round;

    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -pi / 2,
      2 * pi * progress,
      false,
      fg,
    );
  }

  @override
  bool shouldRepaint(covariant _RingPainter oldDelegate) =>
      oldDelegate.progress != progress;
}
