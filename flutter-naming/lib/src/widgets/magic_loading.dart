import 'package:flutter/material.dart';
import 'dart:math' as math;
import 'package:google_fonts/google_fonts.dart';
import '../utils/colors.dart';

class MagicLoadingView extends StatefulWidget {
  final String? message;
  final String? subtitle;
  final double height;
  final Color textColor;
  final bool minimal;
  const MagicLoadingView({
    super.key,
    this.message,
    this.subtitle,
    this.height = 120,
    this.textColor = Colors.white,
    this.minimal = false,
  });

  @override
  State<MagicLoadingView> createState() => _MagicLoadingViewState();
}

class _MagicLoadingViewState extends State<MagicLoadingView>
    with TickerProviderStateMixin {
  late AnimationController _rotationController;
  late AnimationController _slowRotationController;
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

    _slowRotationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 8),
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
    for (int i = 0; i < 30; i++) {
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

  Widget _glowingOrbDot(double scale, Color color) {
    return Container(
      width: 6 * scale,
      height: 6 * scale,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: color,
            blurRadius: 6 * scale,
            spreadRadius: 1.5 * scale,
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _rotationController.dispose();
    _slowRotationController.dispose();
    _pulseController.dispose();
    _particleController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final bool hasHeightLimit = constraints.hasBoundedHeight;
        final bool compact = hasHeightLimit && constraints.maxHeight < 260;
        final double baseSize = compact
            ? math.min(widget.height * 0.62, constraints.maxHeight * 0.40)
            : widget.height;
        final double scale = baseSize / 120.0;

        return Center(
          child: SizedBox(
            height: baseSize,
            width: baseSize,
            child: Stack(
              alignment: Alignment.center,
              children: [
                DecoratedBox(
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: RadialGradient(
                      colors: [
                        const Color(0xFFFFECC2).withValues(alpha: 0.95),
                        const Color(0xFFE5D5FF).withValues(alpha: 0.45),
                        Colors.transparent,
                      ],
                    ),
                  ),
                  child: SizedBox(width: 120 * scale, height: 120 * scale),
                ),
                // Outer Ring: Electric Magenta Astrological Compass with zodiac tick markers
                RotationTransition(
                  turns: _rotationController,
                  child: CustomPaint(
                    painter: _CosmicCompassPainter(
                      colors: const [
                        Color(0xFFD500F9), // Electric Magenta
                        Color(0xFF00E5FF), // Neon Cyan
                        Color(0xFFD500F9),
                      ],
                      strokeWidth: 2.2 * scale,
                      rotationValue: _rotationController.value,
                      drawZodiacTicks: true,
                    ),
                    size: Size(110 * scale, 110 * scale),
                  ),
                ),
                // Middle Ring: Reverse rotating neon cyan gradient ring
                RotationTransition(
                  turns: ReverseAnimation(_rotationController),
                  child: CustomPaint(
                    painter: _CosmicCompassPainter(
                      colors: const [
                        Color(0xFF00E5FF), // Neon Cyan
                        Color(0xFFFFD54F), // Golden Yellow
                        Color(0xFF00E5FF),
                      ],
                      strokeWidth: 1.6 * scale,
                      rotationValue: _rotationController.value,
                    ),
                    size: Size(86 * scale, 86 * scale),
                  ),
                ),
                // Inner Ring: Slow rotating golden runic compass
                RotationTransition(
                  turns: _slowRotationController,
                  child: CustomPaint(
                    painter: _CosmicCompassPainter(
                      colors: const [
                        Color(0xFFFFD54F), // Golden Yellow
                        Color(0xFFFF6D00), // Orange/Gold
                        Color(0xFFFFD54F),
                      ],
                      strokeWidth: 1.2 * scale,
                      rotationValue: _slowRotationController.value,
                    ),
                    size: Size(68 * scale, 68 * scale),
                  ),
                ),
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
                // Pulsing Cosmic Central Orb with orbiting elemental light points
                ScaleTransition(
                  scale: Tween<double>(begin: 0.92 * scale, end: 1.08 * scale)
                      .animate(
                        CurvedAnimation(
                          parent: _pulseController,
                          curve: Curves.easeInOut,
                        ),
                      ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      Container(
                        width: 54 * scale,
                        height: 54 * scale,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: const LinearGradient(
                            colors: [
                              Color(0xFFD500F9), // Electric Magenta
                              Color(0xFF00E5FF), // Neon Cyan
                            ],
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: const Color(
                                0xFFD500F9,
                              ).withValues(alpha: 0.65),
                              blurRadius: 18 * scale,
                              spreadRadius: 2 * scale,
                            ),
                            BoxShadow(
                              color: const Color(
                                0xFF00E5FF,
                              ).withValues(alpha: 0.55),
                              blurRadius: 28 * scale,
                              spreadRadius: 1 * scale,
                            ),
                            BoxShadow(
                              color: const Color(
                                0xFFFFD54F,
                              ).withValues(alpha: 0.45),
                              blurRadius: 10 * scale,
                              spreadRadius: 0.5 * scale,
                            ),
                          ],
                        ),
                        child: Center(
                          child: Icon(
                            Icons.auto_awesome,
                            color: Colors.white,
                            size: 30 * scale,
                          ),
                        ),
                      ),
                      // 4 cardinal glowing elemental dots orbiting the center orb
                      RotationTransition(
                        turns: _rotationController,
                        child: SizedBox(
                          width: 76 * scale,
                          height: 76 * scale,
                          child: Stack(
                            children: [
                              Align(
                                alignment: Alignment.topCenter,
                                child: _glowingOrbDot(
                                  scale,
                                  const Color(0xFFFFD54F),
                                ),
                              ),
                              Align(
                                alignment: Alignment.bottomCenter,
                                child: _glowingOrbDot(
                                  scale,
                                  const Color(0xFFFFD54F),
                                ),
                              ),
                              Align(
                                alignment: Alignment.centerLeft,
                                child: _glowingOrbDot(
                                  scale,
                                  const Color(0xFF00E5FF),
                                ),
                              ),
                              Align(
                                alignment: Alignment.centerRight,
                                child: _glowingOrbDot(
                                  scale,
                                  const Color(0xFFD500F9),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

// ignore: unused_element
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

class _CosmicCompassPainter extends CustomPainter {
  final List<Color> colors;
  final double strokeWidth;
  final double rotationValue;
  final bool drawZodiacTicks;

  _CosmicCompassPainter({
    required this.colors,
    required this.strokeWidth,
    required this.rotationValue,
    this.drawZodiacTicks = false,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;
    final rect = Rect.fromCircle(center: center, radius: radius);

    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..shader = SweepGradient(
        colors: colors,
        stops: const [0.0, 0.5, 1.0],
        transform: GradientRotation(rotationValue * 2 * math.pi),
      ).createShader(rect);

    canvas.drawCircle(center, radius, paint);

    if (drawZodiacTicks) {
      final tickPaint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth * 1.5
        ..shader = SweepGradient(
          colors: colors.reversed.toList(),
        ).createShader(rect);

      for (int i = 0; i < 12; i++) {
        final angle = i * 2 * math.pi / 12;
        final startOffset = Offset(
          center.dx + math.cos(angle) * (radius - 5 * strokeWidth),
          center.dy + math.sin(angle) * (radius - 5 * strokeWidth),
        );
        final endOffset = Offset(
          center.dx + math.cos(angle) * radius,
          center.dy + math.sin(angle) * radius,
        );
        canvas.drawLine(startOffset, endOffset, tickPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _CosmicCompassPainter oldDelegate) =>
      oldDelegate.rotationValue != rotationValue ||
      oldDelegate.colors != colors ||
      oldDelegate.strokeWidth != strokeWidth;
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

      Color baseColor;
      if (p.opacity < 0.35) {
        baseColor = const Color(0xFFE040FB); // Vibrant Purple
      } else if (p.opacity < 0.7) {
        baseColor = const Color(0xFF00E5FF); // Vibrant Cyan
      } else {
        baseColor = const Color(0xFFFFD54F); // Vibrant Gold
      }

      final paint = Paint()
        ..color = Color.lerp(
          baseColor,
          Colors.white,
          p.opacity * 0.4,
        )!.withValues(alpha: 0.8 * math.sin(progress * math.pi));

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
    // ignore: unused_element_parameter
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
                widget.color.withValues(alpha: 0.7),
                AppColors.accent,
                widget.color.withValues(alpha: 0.7),
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
