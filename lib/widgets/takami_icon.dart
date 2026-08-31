// Единый набор SVG-подобных иконок Takami (stroke 1.8, round).
// Портирован из kit/Icons.jsx — рисуется через CustomPainter в 24x24 viewBox.
import 'package:flutter/material.dart';

enum TIcon {
  home,
  library,
  calendar,
  more,
  settings,
  menu,
  back,
  search,
  plus,
  refresh,
  download,
  chevron,
  arrowL,
  arrowR,
  info,
  dot,
  alert,
  check,
  close,
  edit,
  copy,
  paste,
  filter,
  play,
  pause,
  prev,
  next,
  volume,
  music,
  headphones,
  book,
  bookOpen,
  bookmark,
  heart,
  heartFilled,
  brain,
  bell,
  folder,
  battery,
  shield,
  doc,
  eye,
  eyeOff,
  spark,
  spark2,
  spark4,
  swipes,
  news,
  clock,
  chart,
  compass,
  wallet,
  qr,
  usb,
  users,
  send,
  external,
  cast,
  pip,
  awake,
  star,
}

class TakamiIcon extends StatelessWidget {
  final TIcon icon;
  final double size;
  final Color? color;
  final double strokeWidth;

  const TakamiIcon(
    this.icon, {
    super.key,
    this.size = 22,
    this.color,
    this.strokeWidth = 1.8,
  });

  @override
  Widget build(BuildContext context) {
    final c = color ?? IconTheme.of(context).color ?? Colors.white;
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(painter: _TakamiIconPainter(icon, c, strokeWidth)),
    );
  }
}

class _TakamiIconPainter extends CustomPainter {
  final TIcon icon;
  final Color color;
  final double strokeWidth;

  _TakamiIconPainter(this.icon, this.color, this.strokeWidth);

  @override
  void paint(Canvas canvas, Size size) {
    final scale = size.width / 24.0;
    canvas.save();
    canvas.scale(scale, scale);

    final stroke = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    final fill = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    void line(double x1, double y1, double x2, double y2) {
      canvas.drawLine(Offset(x1, y1), Offset(x2, y2), stroke);
    }

    void circleStroke(double cx, double cy, double r) {
      canvas.drawCircle(Offset(cx, cy), r, stroke);
    }

    void circleFill(double cx, double cy, double r) {
      canvas.drawCircle(Offset(cx, cy), r, fill);
    }

    switch (icon) {
      case TIcon.home:
        final p = Path()
          ..moveTo(3, 11.5)
          ..lineTo(12, 4)
          ..lineTo(21, 11.5)
          ..lineTo(21, 20)
          ..cubicTo(21, 20.5, 20.5, 21, 20, 21)
          ..lineTo(15, 21)
          ..lineTo(15, 15)
          ..lineTo(9, 15)
          ..lineTo(9, 21)
          ..lineTo(4, 21)
          ..cubicTo(3.5, 21, 3, 20.5, 3, 20)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.library:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(4, 4, 4.5, 16),
            const Radius.circular(1),
          ),
          stroke,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(10, 6, 4.5, 14),
            const Radius.circular(1),
          ),
          stroke,
        );
        canvas.save();
        canvas.translate(17.6, 14.2);
        canvas.rotate(0.22);
        canvas.drawRect(const Rect.fromLTWH(-1.1, -6.4, 2.2, 12.8), fill);
        canvas.restore();
        break;
      case TIcon.calendar:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(3.5, 5, 17, 15),
            const Radius.circular(2.5),
          ),
          stroke,
        );
        line(3.5, 10, 20.5, 10);
        line(8, 3, 8, 7);
        line(16, 3, 16, 7);
        circleFill(12, 15, 1.4);
        break;
      case TIcon.more:
        circleFill(6, 12, 1.8);
        circleFill(12, 12, 1.8);
        circleFill(18, 12, 1.8);
        break;
      case TIcon.settings:
        circleStroke(12, 12, 3);
        final p = Path()
          ..moveTo(19.4, 12)
          ..cubicTo(19.4, 11.5, 19.35, 11, 19.3, 10.6)
          ..lineTo(21.3, 9.1)
          ..lineTo(19.3, 5.7)
          ..lineTo(17, 6.6)
          ..cubicTo(16.35, 6.05, 15.6, 5.6, 14.8, 5.3)
          ..lineTo(14, 2.5)
          ..lineTo(10, 2.5)
          ..lineTo(9.4, 5)
          ..cubicTo(8.6, 5.3, 7.85, 5.75, 7, 6.4)
          ..lineTo(4.7, 5.5)
          ..lineTo(2.7, 8.9)
          ..lineTo(4.7, 10.4)
          ..cubicTo(4.6, 10.8, 4.6, 11.3, 4.6, 11.8)
          ..cubicTo(4.6, 12.3, 4.6, 12.8, 4.7, 13.2)
          ..lineTo(2.7, 14.7)
          ..lineTo(4.7, 18.1)
          ..lineTo(7, 17.2)
          ..cubicTo(7.85, 17.85, 8.6, 18.3, 9.4, 18.6)
          ..lineTo(10, 21.5)
          ..lineTo(14, 21.5)
          ..lineTo(14.6, 19)
          ..cubicTo(15.4, 18.7, 16.15, 18.25, 17, 17.6)
          ..lineTo(19.3, 18.5)
          ..lineTo(21.3, 15.1)
          ..lineTo(19.3, 13.6)
          ..cubicTo(19.4, 13.2, 19.4, 12.5, 19.4, 12)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.menu:
        circleFill(12, 5, 1.8);
        circleFill(12, 12, 1.8);
        circleFill(12, 19, 1.8);
        break;
      case TIcon.back:
        final p = Path()
          ..moveTo(15, 6)
          ..lineTo(9, 12)
          ..lineTo(15, 18);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.search:
        circleStroke(11, 11, 6.5);
        line(20, 20, 16.5, 16.5);
        break;
      case TIcon.plus:
        line(12, 5, 12, 19);
        line(5, 12, 19, 12);
        break;
      case TIcon.refresh:
        final p = Path()
          ..moveTo(4, 12)
          ..arcToPoint(
            const Offset(18.9, 8),
            radius: const Radius.circular(8),
            clockwise: true,
          );
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(20, 12)
          ..arcToPoint(
            const Offset(5.1, 16),
            radius: const Radius.circular(8),
            clockwise: true,
          );
        canvas.drawPath(p2, stroke);
        final p3 = Path()
          ..moveTo(19, 4)
          ..lineTo(19, 8)
          ..lineTo(15, 8);
        canvas.drawPath(p3, stroke);
        final p4 = Path()
          ..moveTo(5, 20)
          ..lineTo(5, 16)
          ..lineTo(9, 16);
        canvas.drawPath(p4, stroke);
        break;
      case TIcon.download:
        line(12, 4, 12, 15);
        final p = Path()
          ..moveTo(7, 10)
          ..lineTo(12, 15)
          ..lineTo(17, 10);
        canvas.drawPath(p, stroke);
        line(5, 19, 19, 19);
        break;
      case TIcon.chevron:
        final p = Path()
          ..moveTo(9, 6)
          ..lineTo(15, 12)
          ..lineTo(9, 18);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.arrowL:
        final p = Path()
          ..moveTo(14, 6)
          ..lineTo(8, 12)
          ..lineTo(14, 18);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.arrowR:
        final p = Path()
          ..moveTo(10, 6)
          ..lineTo(16, 12)
          ..lineTo(10, 18);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.info:
        circleStroke(12, 12, 9);
        circleFill(12, 8.1, 0.9);
        line(11, 12, 12, 12);
        line(12, 12, 12, 17);
        line(11, 17, 13, 17);
        break;
      case TIcon.dot:
        circleFill(12, 12, 2.5);
        break;
      case TIcon.alert:
        final p = Path()
          ..moveTo(12, 3)
          ..lineTo(2, 20)
          ..lineTo(22, 20)
          ..close();
        canvas.drawPath(p, stroke);
        line(12, 10, 12, 15);
        circleFill(12, 18, 0.8);
        break;
      case TIcon.check:
        final p = Path()
          ..moveTo(5, 12)
          ..lineTo(10, 17)
          ..lineTo(20, 7);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.close:
        line(6, 6, 18, 18);
        line(6, 18, 18, 6);
        break;
      case TIcon.edit:
        final p = Path()
          ..moveTo(4, 20)
          ..lineTo(8, 20)
          ..lineTo(20, 8)
          ..lineTo(16, 4)
          ..lineTo(4, 16)
          ..close();
        canvas.drawPath(p, stroke);
        line(14, 6, 18, 10);
        break;
      case TIcon.copy:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(8, 8, 12, 12),
            const Radius.circular(2),
          ),
          stroke,
        );
        final p = Path()
          ..moveTo(16, 8)
          ..lineTo(16, 6)
          ..cubicTo(16, 4.9, 15.1, 4, 14, 4)
          ..lineTo(6, 4)
          ..cubicTo(4.9, 4, 4, 4.9, 4, 6)
          ..lineTo(4, 14)
          ..cubicTo(4, 15.1, 4.9, 16, 6, 16)
          ..lineTo(8, 16);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.paste:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(8, 4, 8, 4),
            const Radius.circular(1),
          ),
          stroke,
        );
        final p = Path()
          ..moveTo(16, 6)
          ..lineTo(18, 6)
          ..cubicTo(19.1, 6, 20, 6.9, 20, 8)
          ..lineTo(20, 19)
          ..cubicTo(20, 20.1, 19.1, 21, 18, 21)
          ..lineTo(6, 21)
          ..cubicTo(4.9, 21, 4, 20.1, 4, 19)
          ..lineTo(4, 8)
          ..cubicTo(4, 6.9, 4.9, 6, 6, 6)
          ..lineTo(8, 6);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.filter:
        final p = Path()
          ..moveTo(4, 5)
          ..lineTo(20, 5)
          ..lineTo(14, 13)
          ..lineTo(14, 19)
          ..lineTo(10, 17)
          ..lineTo(10, 13)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.play:
        final p = Path()
          ..moveTo(8, 5)
          ..lineTo(8, 19)
          ..lineTo(19, 12)
          ..close();
        canvas.drawPath(p, fill);
        break;
      case TIcon.pause:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(7, 5, 3.5, 14),
            const Radius.circular(0.8),
          ),
          fill,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(13.5, 5, 3.5, 14),
            const Radius.circular(0.8),
          ),
          fill,
        );
        break;
      case TIcon.prev:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(5, 6, 2, 12),
            const Radius.circular(0.6),
          ),
          fill,
        );
        final p = Path()
          ..moveTo(9, 12)
          ..lineTo(20, 5)
          ..lineTo(20, 19)
          ..close();
        canvas.drawPath(p, fill);
        break;
      case TIcon.next:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(17, 6, 2, 12),
            const Radius.circular(0.6),
          ),
          fill,
        );
        final p = Path()
          ..moveTo(15, 12)
          ..lineTo(4, 5)
          ..lineTo(4, 19)
          ..close();
        canvas.drawPath(p, fill);
        break;
      case TIcon.volume:
        final p = Path()
          ..moveTo(4, 10)
          ..lineTo(4, 14)
          ..lineTo(8, 14)
          ..lineTo(13, 18)
          ..lineTo(13, 6)
          ..lineTo(8, 10)
          ..close();
        canvas.drawPath(p, fill);
        final p2 = Path()
          ..moveTo(17, 9)
          ..cubicTo(18.2, 10, 18.2, 14, 17, 15);
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.music:
        circleStroke(6, 18, 2.5);
        circleStroke(17, 16, 2.5);
        final p = Path()
          ..moveTo(8.5, 18)
          ..lineTo(8.5, 6)
          ..lineTo(19.5, 4)
          ..lineTo(19.5, 16);
        canvas.drawPath(p, stroke);
        break;
      case TIcon.headphones:
        final p = Path()
          ..moveTo(4, 15)
          ..lineTo(4, 12)
          ..cubicTo(4, 7.6, 7.6, 4, 12, 4)
          ..cubicTo(16.4, 4, 20, 7.6, 20, 12)
          ..lineTo(20, 15);
        canvas.drawPath(p, stroke);
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(2, 13, 4, 6),
            const Radius.circular(2),
          ),
          stroke,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(18, 13, 4, 6),
            const Radius.circular(2),
          ),
          stroke,
        );
        break;
      case TIcon.book:
        final p = Path()
          ..moveTo(4, 5)
          ..cubicTo(4, 4.4, 4.4, 4, 5, 4)
          ..lineTo(11, 4)
          ..lineTo(11, 20)
          ..lineTo(5, 20)
          ..cubicTo(4.4, 20, 4, 19.6, 4, 19)
          ..close();
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(13, 4)
          ..lineTo(19, 4)
          ..cubicTo(19.6, 4, 20, 4.4, 20, 5)
          ..lineTo(20, 19)
          ..cubicTo(20, 19.6, 19.6, 20, 19, 20)
          ..lineTo(13, 20)
          ..close();
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.bookOpen:
        final p = Path()
          ..moveTo(2, 5)
          ..cubicTo(2, 4.4, 2.4, 4, 3, 4)
          ..lineTo(9, 4)
          ..cubicTo(10.6, 4, 12, 5.4, 12, 7)
          ..lineTo(12, 21)
          ..cubicTo(12, 19.9, 11.1, 19, 10, 19)
          ..lineTo(2, 19)
          ..close();
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(22, 5)
          ..cubicTo(22, 4.4, 21.6, 4, 21, 4)
          ..lineTo(15, 4)
          ..cubicTo(13.4, 4, 12, 5.4, 12, 7)
          ..lineTo(12, 21)
          ..cubicTo(12, 19.9, 12.9, 19, 14, 19)
          ..lineTo(22, 19)
          ..close();
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.bookmark:
        final p = Path()
          ..moveTo(6, 4)
          ..lineTo(18, 4)
          ..lineTo(18, 21)
          ..lineTo(12, 17)
          ..lineTo(6, 21)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.heart:
        final p = _heartPath();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.heartFilled:
        final p = _heartPath();
        canvas.drawPath(p, fill);
        break;
      case TIcon.brain:
        final p = Path()
          ..moveTo(8, 4)
          ..cubicTo(6.1, 4, 4.5, 5.6, 4.5, 7.5)
          ..cubicTo(4.5, 8, 4.6, 8.5, 4.8, 8.9)
          ..cubicTo(4.3, 9.5, 4, 10.2, 4, 12)
          ..cubicTo(4, 13.3, 4.4, 14.5, 5, 14.5)
          ..cubicTo(5, 16.9, 6.3, 19, 8, 19)
          ..cubicTo(9.1, 19, 10.5, 18.5, 11, 18)
          ..lineTo(11, 4)
          ..cubicTo(10.2, 3.6, 9, 4, 8, 4)
          ..close();
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(16, 4)
          ..cubicTo(17.9, 4, 19.5, 5.6, 19.5, 7.5)
          ..cubicTo(19.5, 8, 19.4, 8.5, 19.2, 8.9)
          ..cubicTo(19.7, 9.5, 20, 10.2, 20, 12)
          ..cubicTo(20, 13.3, 19.6, 14.5, 19, 14.5)
          ..cubicTo(19, 16.9, 17.7, 19, 16, 19)
          ..cubicTo(14.9, 19, 13.5, 18.5, 13, 18)
          ..lineTo(13, 4)
          ..cubicTo(13.8, 3.6, 15, 4, 16, 4)
          ..close();
        canvas.drawPath(p2, stroke);
        circleFill(9, 8, 0.6);
        circleFill(9, 12, 0.6);
        circleFill(9, 16, 0.6);
        circleFill(15, 8, 0.6);
        circleFill(15, 12, 0.6);
        circleFill(15, 16, 0.6);
        break;
      case TIcon.bell:
        final p = Path()
          ..moveTo(6, 8)
          ..cubicTo(6, 4.7, 8.7, 2, 12, 2)
          ..cubicTo(15.3, 2, 18, 4.7, 18, 8)
          ..cubicTo(18, 14, 20, 16, 20, 16)
          ..lineTo(4, 16)
          ..cubicTo(4, 16, 6, 14, 6, 8)
          ..close();
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(10, 20)
          ..cubicTo(10, 21.1, 10.9, 22, 12, 22)
          ..cubicTo(13.1, 22, 14, 21.1, 14, 20);
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.folder:
        final p = Path()
          ..moveTo(3, 6)
          ..cubicTo(3, 5.4, 3.4, 5, 4, 5)
          ..lineTo(9, 5)
          ..lineTo(11, 7)
          ..lineTo(20, 7)
          ..cubicTo(20.6, 7, 21, 7.4, 21, 8)
          ..lineTo(21, 19)
          ..cubicTo(21, 19.6, 20.6, 20, 20, 20)
          ..lineTo(4, 20)
          ..cubicTo(3.4, 20, 3, 19.6, 3, 19)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.battery:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(2, 7, 18, 10),
            const Radius.circular(2),
          ),
          stroke,
        );
        line(22, 10, 22, 14);
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(4, 9, 6, 6),
            const Radius.circular(0.5),
          ),
          fill,
        );
        break;
      case TIcon.shield:
        final p = Path()
          ..moveTo(12, 3)
          ..lineTo(4, 6)
          ..lineTo(4, 12)
          ..cubicTo(4, 17, 7.5, 20, 12, 21)
          ..cubicTo(16.5, 20, 20, 17, 20, 12)
          ..lineTo(20, 6)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.doc:
        final p = Path()
          ..moveTo(6, 3)
          ..lineTo(14, 3)
          ..lineTo(19, 8)
          ..lineTo(19, 20)
          ..cubicTo(19, 20.6, 18.6, 21, 18, 21)
          ..lineTo(6, 21)
          ..cubicTo(5.4, 21, 5, 20.6, 5, 20)
          ..lineTo(5, 4)
          ..cubicTo(5, 3.4, 5.4, 3, 6, 3)
          ..close();
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(14, 3)
          ..lineTo(14, 8)
          ..lineTo(19, 8);
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.eye:
        final p = Path()
          ..moveTo(2, 12)
          ..cubicTo(2, 12, 5.5, 5, 12, 5)
          ..cubicTo(18.5, 5, 22, 12, 22, 12)
          ..cubicTo(22, 12, 18.5, 19, 12, 19)
          ..cubicTo(5.5, 19, 2, 12, 2, 12)
          ..close();
        canvas.drawPath(p, stroke);
        circleStroke(12, 12, 3);
        break;
      case TIcon.eyeOff:
        line(3, 3, 21, 21);
        final p = Path()
          ..moveTo(10.6, 6.2)
          ..cubicTo(11, 6.1, 11.5, 6, 12, 6)
          ..cubicTo(18.5, 6, 22, 12, 22, 12)
          ..cubicTo(22, 12, 21, 13.9, 19.4, 15.4);
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(6.7, 6.8)
          ..cubicTo(4.3, 8.3, 2, 12, 2, 12)
          ..cubicTo(2, 12, 5.5, 19, 12, 19)
          ..cubicTo(13.4, 19, 14.7, 18.7, 15.9, 18.3);
        canvas.drawPath(p2, stroke);
        final p3 = Path()
          ..moveTo(9.4, 9.4)
          ..cubicTo(8.9, 9.9, 8.6, 10.6, 8.6, 11.4)
          ..cubicTo(8.6, 12.9, 9.8, 14.1, 11.3, 14.1)
          ..cubicTo(12.1, 14.1, 12.8, 13.8, 13.3, 13.3);
        canvas.drawPath(p3, stroke);
        break;
      case TIcon.spark:
        line(12, 3, 12, 9);
        line(12, 15, 12, 21);
        line(3, 12, 9, 12);
        line(15, 12, 21, 12);
        line(6, 6, 9.5, 9.5);
        line(14.5, 14.5, 18, 18);
        line(6, 18, 9.5, 14.5);
        line(14.5, 9.5, 18, 6);
        break;
      case TIcon.spark2:
        line(12, 3, 12, 6);
        line(12, 18, 12, 21);
        line(4.5, 4.5, 6.5, 6.5);
        line(17.5, 17.5, 19.5, 19.5);
        line(3, 12, 6, 12);
        line(18, 12, 21, 12);
        line(4.5, 19.5, 6.5, 17.5);
        line(17.5, 6.5, 19.5, 4.5);
        circleFill(12, 12, 3);
        break;
      case TIcon.spark4:
        final p = Path()
          ..moveTo(12, 3)
          ..lineTo(14, 9)
          ..lineTo(20, 12)
          ..lineTo(14, 15)
          ..lineTo(12, 21)
          ..lineTo(10, 15)
          ..lineTo(4, 12)
          ..lineTo(10, 9)
          ..close();
        canvas.drawPath(p, stroke);
        break;
      case TIcon.swipes:
        final p = Path()
          ..moveTo(9, 3)
          ..lineTo(5, 7)
          ..lineTo(9, 11);
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(15, 21)
          ..lineTo(19, 17)
          ..lineTo(15, 13);
        canvas.drawPath(p2, stroke);
        final p3 = Path()
          ..moveTo(5, 7)
          ..lineTo(16, 7)
          ..cubicTo(18.2, 7, 20, 8.8, 20, 11);
        canvas.drawPath(p3, stroke);
        final p4 = Path()
          ..moveTo(19, 17)
          ..lineTo(8, 17)
          ..cubicTo(5.8, 17, 4, 15.2, 4, 13);
        canvas.drawPath(p4, stroke);
        break;
      case TIcon.news:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(3, 4, 18, 16),
            const Radius.circular(2),
          ),
          stroke,
        );
        line(7, 8, 17, 8);
        line(7, 12, 17, 12);
        line(7, 16, 13, 16);
        break;
      case TIcon.clock:
        circleStroke(12, 12, 9);
        line(12, 7, 12, 12);
        line(12, 12, 15, 14);
        break;
      case TIcon.chart:
        line(3, 3, 3, 21);
        line(3, 21, 21, 21);
        line(7, 17, 7, 9);
        line(12, 17, 12, 6);
        line(17, 17, 17, 12);
        break;
      case TIcon.compass:
        circleStroke(12, 12, 9);
        final p = Path()
          ..moveTo(9.5, 14.5)
          ..lineTo(14.5, 12.5)
          ..lineTo(12.5, 9.5)
          ..lineTo(14.5, 14.5)
          ..lineTo(9.5, 14.5)
          ..close();
        canvas.drawPath(p, fill);
        break;
      case TIcon.wallet:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(3, 6, 18, 14),
            const Radius.circular(2),
          ),
          stroke,
        );
        line(3, 10, 21, 10);
        circleFill(18, 15, 0.9);
        break;
      case TIcon.qr:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(3, 3, 7, 7),
            const Radius.circular(1),
          ),
          stroke,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(14, 3, 7, 7),
            const Radius.circular(1),
          ),
          stroke,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(3, 14, 7, 7),
            const Radius.circular(1),
          ),
          stroke,
        );
        canvas.drawRect(const Rect.fromLTWH(14, 14, 3, 3), fill);
        canvas.drawRect(const Rect.fromLTWH(19, 14, 2, 2), fill);
        canvas.drawRect(const Rect.fromLTWH(14, 19, 2, 2), fill);
        canvas.drawRect(const Rect.fromLTWH(17.5, 17, 2, 4), fill);
        break;
      case TIcon.usb:
        circleStroke(6, 18, 2);
        final p = Path()
          ..moveTo(6, 16)
          ..lineTo(6, 8)
          ..lineTo(12, 11)
          ..lineTo(12, 14)
          ..lineTo(16, 12)
          ..lineTo(16, 6);
        canvas.drawPath(p, stroke);
        canvas.drawRect(const Rect.fromLTWH(14, 4, 6, 4), stroke);
        break;
      case TIcon.users:
        circleStroke(9, 8, 3.5);
        final p = Path()
          ..moveTo(2, 20)
          ..cubicTo(2, 16.1, 5.1, 13, 9, 13)
          ..cubicTo(12.9, 13, 16, 16.1, 16, 20);
        canvas.drawPath(p, stroke);
        circleStroke(17, 9, 2.5);
        final p2 = Path()
          ..moveTo(22, 20)
          ..cubicTo(22, 17.2, 19.8, 15, 17, 15);
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.send:
        final p = Path()
          ..moveTo(22, 2)
          ..lineTo(11, 13);
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(22, 2)
          ..lineTo(15, 22)
          ..lineTo(11, 13)
          ..lineTo(2, 9)
          ..close();
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.external:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(4, 4, 12, 6),
            const Radius.circular(0),
          ),
          Paint()..color = Colors.transparent,
        );
        final p = Path()
          ..moveTo(14, 4)
          ..lineTo(20, 4)
          ..lineTo(20, 10);
        canvas.drawPath(p, stroke);
        line(20, 4, 10, 14);
        final p2 = Path()
          ..moveTo(20, 14)
          ..lineTo(20, 19)
          ..cubicTo(20, 19.6, 19.6, 20, 19, 20)
          ..lineTo(5, 20)
          ..cubicTo(4.4, 20, 4, 19.6, 4, 19)
          ..lineTo(4, 5)
          ..cubicTo(4, 4.4, 4.4, 4, 5, 4)
          ..lineTo(10, 4);
        canvas.drawPath(p2, stroke);
        break;
      case TIcon.cast:
        final p = Path()
          ..moveTo(3, 8)
          ..lineTo(3, 6)
          ..cubicTo(3, 4.9, 3.9, 4, 5, 4)
          ..lineTo(19, 4)
          ..cubicTo(20.1, 4, 21, 4.9, 21, 6)
          ..lineTo(21, 18)
          ..cubicTo(21, 19.1, 20.1, 20, 19, 20)
          ..lineTo(12, 20);
        canvas.drawPath(p, stroke);
        final p2 = Path()
          ..moveTo(3, 12)
          ..cubicTo(8, 12, 12, 16, 12, 21);
        canvas.drawPath(p2, stroke);
        final p3 = Path()
          ..moveTo(3, 16)
          ..cubicTo(5.8, 16, 8, 18.2, 8, 21);
        canvas.drawPath(p3, stroke);
        circleFill(3, 20, 1);
        break;
      case TIcon.pip:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(3, 5, 18, 14),
            const Radius.circular(2),
          ),
          stroke,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(12, 12, 8, 6),
            const Radius.circular(1),
          ),
          fill,
        );
        break;
      case TIcon.awake:
        circleStroke(12, 12, 4);
        line(12, 3, 12, 5);
        line(12, 19, 12, 21);
        line(3, 12, 5, 12);
        line(19, 12, 21, 12);
        line(5.6, 5.6, 7, 7);
        line(17, 17, 18.4, 18.4);
        line(5.6, 18.4, 7, 17);
        line(17, 7, 18.4, 5.6);
        break;
      case TIcon.star:
        final p = Path()
          ..moveTo(12, 3)
          ..lineTo(14, 9)
          ..lineTo(20, 9.4)
          ..lineTo(15.2, 13.2)
          ..lineTo(17, 19.5)
          ..lineTo(12, 16)
          ..lineTo(7, 19.5)
          ..lineTo(8.8, 13.2)
          ..lineTo(4, 9.4)
          ..lineTo(10, 9)
          ..close();
        canvas.drawPath(p, fill);
        break;
    }

    canvas.restore();
  }

  Path _heartPath() {
    return Path()
      ..moveTo(12, 21)
      ..cubicTo(12, 21, 4, 15.5, 4, 10)
      ..cubicTo(4, 7.2, 6.2, 5, 8.5, 5)
      ..cubicTo(10, 5, 11.3, 5.8, 12, 7)
      ..cubicTo(12.7, 5.8, 14, 5, 15.5, 5)
      ..cubicTo(17.8, 5, 20, 7.2, 20, 10)
      ..cubicTo(20, 15.5, 12, 21, 12, 21)
      ..close();
  }

  @override
  bool shouldRepaint(covariant _TakamiIconPainter oldDelegate) {
    return oldDelegate.icon != icon ||
        oldDelegate.color != color ||
        oldDelegate.strokeWidth != strokeWidth;
  }
}
