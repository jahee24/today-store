import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';

class DottedLinedCard extends StatelessWidget {
  final VoidCallback onTap;

  const DottedLinedCard({
    super.key,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(h(220)),
      child: CustomPaint(
        foregroundPainter: _DashedBorderPainter(
          color: const Color(0xFFCDD3FF),
          strokeWidth: 2,
          dashWidth: 4,
          dashSpace: 5,
          radius: h(24),
        ),
        child: Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: const Color(0xFFF8F8FC),
            borderRadius: BorderRadius.circular(h(24)),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '📷',
                style: TextStyle(fontSize: f(42)),
              ),
              SizedBox(height: h(8)),
              Text(
                '사진 추가',
                style: TextStyle(
                  fontSize: f(20),
                  fontWeight: FontWeight.w800,
                  color: AppTheme.primaryColor,
                ),
              ),
              SizedBox(height: h(4)),
              Text(
                '갤러리에서 선택하거나 카메라로 촬영',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  fontSize: f(15),
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DashedBorderPainter extends CustomPainter {
  final Color color;
  final double strokeWidth;
  final double dashWidth;
  final double dashSpace;
  final double radius;

  _DashedBorderPainter({
    required this.color,
    required this.strokeWidth,
    required this.dashWidth,
    required this.dashSpace,
    required this.radius,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Rect.fromLTWH(
      strokeWidth / 2,
      strokeWidth / 2,
      size.width - strokeWidth,
      size.height - strokeWidth
    );

    final rrect = RRect.fromRectAndRadius(rect, Radius.circular(radius));
    final path = Path()..addRRect(rrect);

    final paint = Paint()
    ..color = color
    ..style = PaintingStyle.stroke
    ..strokeWidth = strokeWidth
    ..strokeCap = StrokeCap.round;

    for (final metric in path.computeMetrics()) {
      double distance = 0;
      while (distance < metric.length) {
        final next = distance + dashWidth;
        canvas.drawPath(metric.extractPath(distance, next), paint);
        distance += dashWidth + dashSpace;
      }
    }
  }

  @override
  bool shouldRepaint(covariant _DashedBorderPainter oldDelegate) {
    return oldDelegate.color != color ||
    oldDelegate.strokeWidth != strokeWidth ||
    oldDelegate.dashWidth != dashWidth ||
    oldDelegate.dashSpace != dashSpace ||
    oldDelegate.radius != radius;
  }
}