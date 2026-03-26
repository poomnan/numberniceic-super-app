import 'package:flutter/material.dart';
import '../utils/colors.dart';

class ShimmeringGoldText extends StatefulWidget {
  final Widget child;
  const ShimmeringGoldText({super.key, required this.child});

  @override
  State<ShimmeringGoldText> createState() => _ShimmeringGoldTextState();
}

class _ShimmeringGoldTextState extends State<ShimmeringGoldText>
    with SingleTickerProviderStateMixin {
  late AnimationController _shimmerController;

  @override
  void initState() {
    super.initState();
    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 6), // Slower, more elegant speed
    )..repeat();
  }

  @override
  void dispose() {
    _shimmerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _shimmerController,
      builder: (context, child) {
        return ShaderMask(
          blendMode: BlendMode.srcIn,
          shaderCallback: (bounds) {
            return LinearGradient(
              colors: [
                Color(0xFF8B4513), // Deep Saddle Brown-Gold
                AppColors.secondary, // Deep Bronze Gold
                Color(0xFFFFD700), // Pure Radiant Gold (Highlight)
                AppColors.secondary, // Deep Bronze Gold
                AppColors.accent, // Rich Gold
                Color(0xFFFFD700), // Pure Radiant Gold
                AppColors.secondary, // Deep Bronze Gold
                Color(0xFF8B4513), // Deep Saddle Brown-Gold
              ],
              stops: const [0.0, 0.2, 0.4, 0.5, 0.6, 0.75, 0.9, 1.0],
              begin: Alignment(-3.0 + (6.0 * _shimmerController.value), -1.5),
              end: Alignment(-1.0 + (6.0 * _shimmerController.value), 1.5),
              tileMode: TileMode.clamp,
            ).createShader(bounds);
          },
          child: widget.child,
        );
      },
      child: widget.child,
    );
  }
}
