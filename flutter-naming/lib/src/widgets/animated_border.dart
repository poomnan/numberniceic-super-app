import 'package:flutter/material.dart';

class AnimatedBorderContainer extends StatefulWidget {
  final Widget child;
  final bool isActive;
  final double borderWidth;
  final double borderRadius;
  final List<Color> gradientColors;
  final Duration duration;

  const AnimatedBorderContainer({
    super.key,
    required this.child,
    this.isActive = false,
    this.borderWidth = 3.0,
    this.borderRadius = 12.0,
    this.gradientColors = const [
      Color(0xFF6366F1), // Indigo
      Color(0xFF8B5CF6), // Violet
      Color(0xFFD946EF), // Fuchsia
      Color(0xFF6366F1), // Back to Indigo
    ],
    this.duration = const Duration(seconds: 2),
  });

  @override
  State<AnimatedBorderContainer> createState() =>
      _AnimatedBorderContainerState();
}

class _AnimatedBorderContainerState extends State<AnimatedBorderContainer>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: widget.duration);
    if (widget.isActive) {
      _controller.repeat();
    }
  }

  @override
  void didUpdateWidget(AnimatedBorderContainer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isActive && !oldWidget.isActive) {
      _controller.repeat();
    } else if (!widget.isActive && oldWidget.isActive) {
      _controller.stop();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return CustomPaint(
          painter: _BorderPainter(
            animationValue: _controller.value,
            colors: widget.gradientColors,
            width: widget.borderWidth,
            radius: widget.borderRadius,
            isActive: widget.isActive,
          ),
          child: widget.child,
        );
      },
    );
  }
}

class _BorderPainter extends CustomPainter {
  final double animationValue;
  final List<Color> colors;
  final double width;
  final double radius;
  final bool isActive;

  _BorderPainter({
    required this.animationValue,
    required this.colors,
    required this.width,
    required this.radius,
    required this.isActive,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (!isActive) return;

    final rect = Offset.zero & size;
    final rrect = RRect.fromRectAndRadius(rect, Radius.circular(radius));

    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth =
          width +
          0.5 // Slightly thicker
      ..strokeCap = StrokeCap.round;

    paint.shader = SweepGradient(
      colors: colors,
      transform: GradientRotation(animationValue * 2 * 3.14159),
    ).createShader(rect);

    canvas.drawRRect(rrect, paint);

    // HEAVIER GLOW EFFECT
    // Outer glow
    final glowPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth =
          width *
          3.0 // Much wider glow
      ..maskFilter = const MaskFilter.blur(
        BlurStyle.normal,
        8.0,
      ); // Stronger blur

    glowPaint.shader = SweepGradient(
      colors: colors.map((c) => c.withValues(alpha: 0.5)).toList(), // Higher opacity
      transform: GradientRotation(animationValue * 2 * 3.14159),
    ).createShader(rect);

    canvas.drawRRect(rrect, glowPaint);

    // Additional inner neon glow for "heaviness"
    final neonPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = width * 1.5
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 2.0);

    neonPaint.shader = SweepGradient(
      colors: colors.map((c) => c.withValues(alpha: 0.8)).toList(),
      transform: GradientRotation(animationValue * 2 * 3.14159),
    ).createShader(rect);

    canvas.drawRRect(rrect, neonPaint);
  }

  @override
  bool shouldRepaint(_BorderPainter oldDelegate) =>
      oldDelegate.animationValue != animationValue ||
      oldDelegate.isActive != isActive;
}
