import 'package:flutter/material.dart';
import 'dart:math' as math;
import 'package:google_fonts/google_fonts.dart';
import '../utils/colors.dart';

class MagicLoadingView extends StatefulWidget {
  final String? message;
  final double height;
  final Color textColor;
  const MagicLoadingView({
    super.key,
    this.message,
    this.height = 120,
    this.textColor = Colors.white,
  });

  @override
  State<MagicLoadingView> createState() => _MagicLoadingViewState();
}

class _MagicLoadingViewState extends State<MagicLoadingView>
    with TickerProviderStateMixin {
  late AnimationController _rotationController;
  late AnimationController _pulseController;
  late AnimationController _particleController;

  final List<_MagicParticle> _particles = [];
  final math.Random _rng = math.Random();

  @override
  void initState() {
    super.initState();

    _rotationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();

    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);

    _particleController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 3),
    )..repeat();

    // Create initial particles
    for (int i = 0; i < 15; i++) {
      _particles.add(_createParticle());
    }
  }

  _MagicParticle _createParticle() {
    return _MagicParticle(
      angle: _rng.nextDouble() * 2 * math.pi,
      radius: 20 + _rng.nextDouble() * 40,
      size: 1.5 + _rng.nextDouble() * 3,
      speed: 0.5 + _rng.nextDouble() * 1.5,
      opacity: _rng.nextDouble(),
    );
  }

  @override
  void dispose() {
    _rotationController.dispose();
    _pulseController.dispose();
    _particleController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scale = widget.height / 120.0;
    return Column(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        SizedBox(
          height: widget.height,
          width: widget.height,
          child: Stack(
            alignment: Alignment.center,
            children: [
              // Rotating Magic Circle 1
              RotationTransition(
                turns: _rotationController,
                child: CustomPaint(
                  painter: _MagicCirclePainter(
                    color: AppColors.primary.withOpacity(0.4),
                    strokeWidth: 2 * scale,
                    dashCount: 8,
                  ),
                  size: Size(100 * scale, 100 * scale),
                ),
              ),
              // Outer boundary ring for better contrast on light backgrounds
              Container(
                width: 110 * scale,
                height: 110 * scale,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: AppColors.textLight.withOpacity(0.08),
                    width: 1 * scale,
                  ),
                ),
              ),
              // Rotating Magic Circle 2 (Reverse)
              RotationTransition(
                turns: ReverseAnimation(_rotationController),
                child: CustomPaint(
                  painter: _MagicCirclePainter(
                    color: AppColors.secondary.withOpacity(0.5), // More opaque
                    strokeWidth: 1.5 * scale,
                    dashCount: 12,
                  ),
                  size: Size(80 * scale, 80 * scale),
                ),
              ),
              // Particle Field
              AnimatedBuilder(
                animation: _particleController,
                builder: (context, _) {
                  return CustomPaint(
                    painter: _ParticleFieldPainter(
                      particles: _particles,
                      progress: _particleController.value,
                      scale: scale,
                    ),
                    size: Size(120 * scale, 120 * scale),
                  );
                },
              ),
              // Central Pulsing Icon
              ScaleTransition(
                scale: Tween<double>(begin: 0.9 * scale, end: 1.1 * scale)
                    .animate(
                      CurvedAnimation(
                        parent: _pulseController,
                        curve: Curves.easeInOut,
                      ),
                    ),
                child: Container(
                  width: 50 * scale,
                  height: 50 * scale,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: const LinearGradient(
                      colors: [AppColors.primary, AppColors.secondary],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.primary.withOpacity(0.5),
                        blurRadius: 15 * scale,
                        spreadRadius: 2 * scale,
                      ),
                    ],
                  ),
                  child: Center(
                    child: Icon(
                      Icons.auto_awesome,
                      color: Colors.white,
                      size: 28 * scale,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        if (widget.message != null) ...[
          SizedBox(height: 12 * scale),
          _AnimatedLoadingText(
            text: widget.message!,
            fontSize: 14 * (scale < 0.8 ? 0.8 : 1.0),
            color: widget.textColor,
          ),
        ],
      ],
    );
  }
}

class _MagicCirclePainter extends CustomPainter {
  final Color color;
  final double strokeWidth;
  final int dashCount;

  _MagicCirclePainter({
    required this.color,
    required this.strokeWidth,
    required this.dashCount,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;

    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;

    for (int i = 0; i < dashCount; i++) {
      final startAngle = (i * 2 * math.pi / dashCount);
      const sweepAngle = math.pi / 6;
      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        startAngle,
        sweepAngle,
        false,
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _MagicParticle {
  double angle;
  double radius;
  double size;
  double speed;
  double opacity;

  _MagicParticle({
    required this.angle,
    required this.radius,
    required this.size,
    required this.speed,
    required this.opacity,
  });
}

class _ParticleFieldPainter extends CustomPainter {
  final List<_MagicParticle> particles;
  final double progress;
  final double scale;

  _ParticleFieldPainter({
    required this.particles,
    required this.progress,
    required this.scale,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);

    for (var p in particles) {
      final currentAngle = p.angle + progress * 2 * math.pi * p.speed;
      final x = center.dx + math.cos(currentAngle) * p.radius * scale;
      final y = center.dy + math.sin(currentAngle) * p.radius * scale;

      final paint = Paint()
        ..color = Color.lerp(
          AppColors.accent,
          Colors.white,
          p.opacity,
        )!.withOpacity(0.6 * math.sin(progress * math.pi));

      canvas.drawCircle(Offset(x, y), p.size * scale, paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}

class _AnimatedLoadingText extends StatefulWidget {
  final String text;
  final double fontSize;
  final Color color;
  const _AnimatedLoadingText({
    required this.text,
    this.fontSize = 14,
    required this.color,
  });

  @override
  State<_AnimatedLoadingText> createState() => _AnimatedLoadingTextState();
}

class _AnimatedLoadingTextState extends State<_AnimatedLoadingText>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);
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
      builder: (context, _) {
        return ShaderMask(
          shaderCallback: (bounds) {
            return LinearGradient(
              colors: [
                widget.color.withOpacity(0.7),
                AppColors.accent,
                widget.color.withOpacity(0.7),
              ],
              stops: [
                _controller.value - 0.2,
                _controller.value,
                _controller.value + 0.2,
              ],
            ).createShader(bounds);
          },
          child: Text(
            widget.text,
            style: GoogleFonts.prompt(
              color: widget.color,
              fontSize: widget.fontSize,
              fontWeight: FontWeight.w500,
              letterSpacing: 0.5,
            ),
          ),
        );
      },
    );
  }
}
