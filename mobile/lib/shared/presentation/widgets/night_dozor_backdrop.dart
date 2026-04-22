import 'dart:ui';

import 'package:flutter/material.dart';

import '../../../app/theme/app_theme.dart';

class NightDozorBackdrop extends StatelessWidget {
  const NightDozorBackdrop({
    super.key,
    required this.child,
  });

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: RadialGradient(
          center: Alignment.topCenter,
          radius: 1.45,
          colors: [
            Color(0xFF1F3A5F),
            Color(0xFF101827),
            AppTheme.background,
          ],
          stops: [0.0, 0.45, 1.0],
        ),
      ),
      child: Stack(
        children: [
          const _GlowOrb(
            top: -120,
            left: -90,
            color: Color(0x33F59E0B),
            size: 280,
          ),
          const _GlowOrb(
            bottom: -120,
            right: -80,
            color: Color(0x282196F3),
            size: 260,
          ),
          Positioned.fill(
            child: Container(color: const Color(0x26050816)),
          ),
          child,
        ],
      ),
    );
  }
}

class GlassCard extends StatelessWidget {
  const GlassCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(24),
    this.radius = 28,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final double radius;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Container(
          padding: padding,
          decoration: BoxDecoration(
            color: AppTheme.surface,
            borderRadius: BorderRadius.circular(radius),
            border: Border.all(color: AppTheme.border),
            boxShadow: const [
              BoxShadow(
                color: Color(0x52000000),
                blurRadius: 40,
                offset: Offset(0, 20),
              ),
            ],
          ),
          child: child,
        ),
      ),
    );
  }
}

class EyebrowText extends StatelessWidget {
  const EyebrowText(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text.toUpperCase(),
      style: const TextStyle(
        color: AppTheme.accent,
        fontSize: 11,
        fontWeight: FontWeight.w800,
        letterSpacing: 3.4,
      ),
    );
  }
}

class _GlowOrb extends StatelessWidget {
  const _GlowOrb({
    this.top,
    this.left,
    this.right,
    this.bottom,
    required this.color,
    required this.size,
  });

  final double? top;
  final double? left;
  final double? right;
  final double? bottom;
  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: top,
      left: left,
      right: right,
      bottom: bottom,
      child: IgnorePointer(
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: color,
          ),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 26, sigmaY: 26),
            child: const SizedBox.expand(),
          ),
        ),
      ),
    );
  }
}
