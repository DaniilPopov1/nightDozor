import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/presentation/widgets/night_dozor_backdrop.dart';
import '../../theme/app_theme.dart';

class ParticipantShellPage extends StatelessWidget {
  const ParticipantShellPage({
    super.key,
    required this.child,
    required this.location,
  });

  final Widget child;
  final String location;

  int get _selectedIndex {
    if (location.startsWith('/team')) {
      return 1;
    }
    if (location.startsWith('/profile')) {
      return 2;
    }
    return 0;
  }

  void _onTap(BuildContext context, int index) {
    switch (index) {
      case 0:
        context.go('/game');
        return;
      case 1:
        context.go('/team');
        return;
      case 2:
        context.go('/profile');
        return;
      default:
        context.go('/game');
        return;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      backgroundColor: Colors.transparent,
      body: NightDozorBackdrop(child: child),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: GlassCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
          radius: 26,
          child: Row(
            children: [
              _NavItem(
                icon: Icons.flash_on_rounded,
                label: 'Игра',
                selected: _selectedIndex == 0,
                onTap: () => _onTap(context, 0),
              ),
              _NavItem(
                icon: Icons.groups_rounded,
                label: 'Команда',
                selected: _selectedIndex == 1,
                onTap: () => _onTap(context, 1),
              ),
              _NavItem(
                icon: Icons.person_rounded,
                label: 'Профиль',
                selected: _selectedIndex == 2,
                onTap: () => _onTap(context, 2),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
          decoration: BoxDecoration(
            color: selected ? const Color(0x2EF59E0B) : Colors.transparent,
            borderRadius: BorderRadius.circular(18),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                icon,
                color: selected ? AppTheme.accent : AppTheme.textMuted,
              ),
              const SizedBox(height: 6),
              Text(
                label,
                style: TextStyle(
                  color: selected ? AppTheme.textPrimary : AppTheme.textMuted,
                  fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
