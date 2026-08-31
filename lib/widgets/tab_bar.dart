import 'package:flutter/material.dart';
import '../state/app_state.dart';
import '../theme/aurora_theme.dart';
import 'takami_icon.dart';

class TakamiTabBar extends StatefulWidget {
  final AppScreen active;
  final void Function(AppScreen) onNav;
  final VoidCallback onFab;
  final bool fabLoading;
  final int? calendarBadge;

  const TakamiTabBar({
    super.key,
    required this.active,
    required this.onNav,
    required this.onFab,
    required this.fabLoading,
    this.calendarBadge,
  });

  @override
  State<TakamiTabBar> createState() => _TakamiTabBarState();
}

class _TakamiTabBarState extends State<TakamiTabBar>
    with TickerProviderStateMixin {
  late AnimationController _pulseCtrl;
  late AnimationController _spinCtrl;

  @override
  void initState() {
    super.initState();
    _pulseCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 550),
    )..repeat(reverse: true);
    _spinCtrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 1),
    )..repeat();
  }

  @override
  void dispose() {
    _pulseCtrl.dispose();
    _spinCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tabs = <(AppScreen, TIcon, String)>[
      (AppScreen.home, TIcon.home, 'Главная'),
      (AppScreen.library, TIcon.library, 'Библиотека'),
    ];
    final tabs2 = <(AppScreen, TIcon, String)>[
      (AppScreen.calendar, TIcon.calendar, 'Календарь'),
      (AppScreen.settings, TIcon.settings, 'Настройки'),
    ];

    return Container(
      height: 72,
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(
        color: Color(0xF20F1116),
        border: Border(top: BorderSide(color: AuroraColors.brd, width: 1)),
      ),
      child: Row(
        children: [
          ...tabs.map((t) => Expanded(child: _tabButton(t))),
          Expanded(child: _fabSlot()),
          ...tabs2.map((t) => Expanded(child: _tabButton(t))),
        ],
      ),
    );
  }

  Widget _tabButton((AppScreen, TIcon, String) t) {
    final (screen, icon, label) = t;
    final isOn = widget.active == screen;
    final showBadge =
        screen == AppScreen.calendar &&
        widget.calendarBadge != null &&
        widget.calendarBadge! > 0;
    return GestureDetector(
      onTap: () => widget.onNav(screen),
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              TakamiIcon(
                icon,
                size: 22,
                color: isOn ? AuroraColors.acc : AuroraColors.onSurfaceVariant,
              ),
              if (showBadge)
                Positioned(
                  top: -4,
                  right: -8,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 4,
                      vertical: 1,
                    ),
                    decoration: BoxDecoration(
                      color: AuroraColors.error,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '${widget.calendarBadge}',
                      style: const TextStyle(
                        fontSize: 9,
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w600,
              color: isOn ? AuroraColors.acc : AuroraColors.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _fabSlot() {
    return Center(
      child: GestureDetector(
        onTap: widget.fabLoading ? null : widget.onFab,
        child: AnimatedBuilder(
          animation: _pulseCtrl,
          builder: (context, child) {
            final scale = widget.fabLoading
                ? 1.0 + _pulseCtrl.value * 0.08
                : 1.0;
            return Transform.translate(
              offset: const Offset(0, -26),
              child: Transform.scale(
                scale: scale,
                child: Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: AuroraColors.fabGradient,
                    border: Border.all(
                      color: AuroraColors.surfaceContainer,
                      width: 6,
                    ),
                    boxShadow: widget.fabLoading
                        ? AuroraShadows.fabPulse
                        : AuroraShadows.fab,
                  ),
                  child: Center(
                    child: widget.fabLoading
                        ? SizedBox(
                            width: 26,
                            height: 26,
                            child: RotationTransition(
                              turns: _spinCtrl,
                              child: const CircularProgressIndicator(
                                strokeWidth: 2.4,
                                color: Colors.white,
                                value: 0.75,
                              ),
                            ),
                          )
                        : const TakamiIcon(
                            TIcon.swipes,
                            size: 24,
                            color: Colors.white,
                          ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}
