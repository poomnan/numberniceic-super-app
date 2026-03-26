import 'package:flutter/material.dart';
import '../utils/colors.dart';
import 'naming_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // Global Background
          Container(
            decoration: const BoxDecoration(
              gradient: RadialGradient(
                center: Alignment(0, -0.5),
                radius: 1.5,
                colors: [
                  Color(0xFF2E1065),
                  AppColors.bgDark,
                ], // Darker purple start
              ),
            ),
          ),
          // Add a semi-transparent black overlay to dim background
          Container(color: Colors.white.withOpacity(0.3)),
          Positioned(
            top: -100,
            left: -100,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.primary.withOpacity(0.2),
                backgroundBlendMode: BlendMode.screen,
                boxShadow: const [
                  BoxShadow(blurRadius: 100, color: AppColors.primary),
                ],
              ),
            ),
          ),
          Positioned(
            top: 200,
            right: -100,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.secondary.withOpacity(0.2),
                backgroundBlendMode: BlendMode.screen,
                boxShadow: const [
                  BoxShadow(blurRadius: 100, color: AppColors.secondary),
                ],
              ),
            ),
          ),

          // Content - Single Page Application Style (Naming Search Only)
          const NamingScreen(),
        ],
      ),
    );
  }
}
