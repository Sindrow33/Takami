import 'package:flutter/material.dart';
import '../theme/aurora_theme.dart';
import 'takami_icon.dart';

/// AppBar в стиле Takami — минималистичный, с иконками-кнопками.
class TakamiAppBar extends StatelessWidget implements PreferredSizeWidget {
  final VoidCallback? onBack;
  final String title;
  final List<TakamiAction>? actions;
  final Widget? greeting;

  const TakamiAppBar({
    super.key,
    this.onBack,
    this.title = '',
    this.actions,
    this.greeting,
  });

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          if (onBack != null)
            IconButton(
              onPressed: onBack,
              icon: const TakamiIcon(TIcon.back, color: Colors.white),
            )
          else
            const SizedBox(width: 8),
          Expanded(
            child:
                greeting ??
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: AuroraColors.onSurface,
                  ),
                ),
          ),
          ...(actions ?? []).map(
            (a) => IconButton(
              onPressed: a.onClick,
              icon: TakamiIcon(
                a.icon,
                color: a.on ? AuroraColors.acc : Colors.white,
              ),
            ),
          ),
          const SizedBox(width: 4),
        ],
      ),
    );
  }
}

class TakamiAction {
  final TIcon icon;
  final VoidCallback? onClick;
  final bool on;
  const TakamiAction({required this.icon, this.onClick, this.on = false});
}

/// Toast — временное сообщение снизу экрана.
class ToastController {
  static void show(BuildContext context, String message) {
    final overlay = Overlay.of(context);
    final entry = OverlayEntry(
      builder: (ctx) => _ToastWidget(message: message),
    );
    overlay.insert(entry);
    Future.delayed(const Duration(milliseconds: 1600), () {
      entry.remove();
    });
  }
}

class _ToastWidget extends StatefulWidget {
  final String message;
  const _ToastWidget({required this.message});

  @override
  State<_ToastWidget> createState() => _ToastWidgetState();
}

class _ToastWidgetState extends State<_ToastWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _c;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
    _c.forward();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Positioned(
      bottom: 96,
      left: 24,
      right: 24,
      child: FadeTransition(
        opacity: _c,
        child: SlideTransition(
          position: Tween<Offset>(
            begin: const Offset(0, 0.3),
            end: Offset.zero,
          ).animate(CurvedAnimation(parent: _c, curve: Curves.easeOut)),
          child: Material(
            color: Colors.transparent,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: const Color(0xF01A1D23),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AuroraColors.brdEm),
                boxShadow: AuroraShadows.mdShadow,
              ),
              child: Text(
                widget.message,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 13,
                  color: Colors.white,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Cascade stagger animation wrapper — как cascadeIn в прототипе.
class CascadeIn extends StatefulWidget {
  final Widget child;
  final int index;
  const CascadeIn({super.key, required this.child, required this.index});

  @override
  State<CascadeIn> createState() => _CascadeInState();
}

class _CascadeInState extends State<CascadeIn>
    with SingleTickerProviderStateMixin {
  late AnimationController _c;
  late Animation<double> _opacity;
  late Animation<Offset> _slide;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _opacity = CurvedAnimation(parent: _c, curve: Curves.easeOut);
    _slide = Tween<Offset>(
      begin: const Offset(0, -0.06),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _c, curve: Curves.easeOutCubic));
    final delayMs = [0, 40, 80, 120, 160, 200, 235, 265, 290, 310];
    final d = delayMs[widget.index.clamp(0, delayMs.length - 1)];
    Future.delayed(Duration(milliseconds: d), () {
      if (mounted) _c.forward();
    });
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _opacity,
      child: SlideTransition(position: _slide, child: widget.child),
    );
  }
}

/// Секция-заголовок в настройках
class SectionHead extends StatelessWidget {
  final String title;
  const SectionHead({super.key, required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 8),
      child: Text(
        title,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: AuroraColors.acc2,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}

/// Группа настроек — карточка с внутренними разделителями
class SettingsGroup extends StatelessWidget {
  final List<Widget> children;
  const SettingsGroup({super.key, required this.children});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        color: AuroraColors.sub,
        border: Border.all(color: AuroraColors.brd),
        borderRadius: BorderRadius.circular(AuroraRadii.m),
      ),
      child: Column(
        children: [
          for (int i = 0; i < children.length; i++)
            Container(
              decoration: i > 0
                  ? const BoxDecoration(
                      border: Border(
                        top: BorderSide(color: AuroraColors.brd, width: 1),
                      ),
                    )
                  : null,
              child: children[i],
            ),
        ],
      ),
    );
  }
}

class SwitchRow extends StatelessWidget {
  final String label;
  final String? sub;
  final bool value;
  final ValueChanged<bool> onChanged;
  const SwitchRow({
    super.key,
    required this.label,
    this.sub,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: Colors.white,
                  ),
                ),
                if (sub != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 3),
                    child: Text(
                      sub!,
                      style: const TextStyle(
                        fontSize: 11.5,
                        color: AuroraColors.onSurfaceVariant,
                        height: 1.3,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            activeTrackColor: AuroraColors.acc,
          ),
        ],
      ),
    );
  }
}

class ActionRow extends StatelessWidget {
  final String label;
  final String? sub;
  final String? right;
  final bool danger;
  final VoidCallback onClick;
  const ActionRow({
    super.key,
    required this.label,
    this.sub,
    this.right,
    this.danger = false,
    required this.onClick,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onClick,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      color: danger ? AuroraColors.errorStrong : Colors.white,
                    ),
                  ),
                  if (sub != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: Text(
                        sub!,
                        style: const TextStyle(
                          fontSize: 11.5,
                          color: AuroraColors.onSurfaceVariant,
                        ),
                      ),
                    ),
                ],
              ),
            ),
            if (right != null)
              Text(
                right!,
                style: const TextStyle(
                  fontSize: 12,
                  color: AuroraColors.onSurfaceVariant,
                ),
              ),
            const SizedBox(width: 4),
            const TakamiIcon(
              TIcon.chevron,
              size: 16,
              color: AuroraColors.onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }
}

class SegRow<T> extends StatelessWidget {
  final String label;
  final T value;
  final List<(T, String)> options;
  final ValueChanged<T> onChanged;
  const SegRow({
    super.key,
    required this.label,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: Colors.white,
              ),
            ),
          ),
          Container(
            padding: const EdgeInsets.all(3),
            decoration: BoxDecoration(
              color: AuroraColors.surfaceVariant,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: options.map((opt) {
                final isOn = opt.$1 == value;
                return GestureDetector(
                  onTap: () => onChanged(opt.$1),
                  child: AnimatedContainer(
                    duration: AuroraMotion.dFast,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: isOn ? AuroraColors.acc : Colors.transparent,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      opt.$2,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: isOn
                            ? Colors.white
                            : AuroraColors.onSurfaceVariant,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }
}

/// Filter chip row — как .filter-tabs .chip
class FilterChip2 extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const FilterChip2({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: AuroraMotion.dFast,
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? AuroraColors.acc : AuroraColors.sub,
          border: Border.all(
            color: selected ? AuroraColors.acc : AuroraColors.brd,
          ),
          borderRadius: BorderRadius.circular(AuroraRadii.full),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w600,
            color: selected ? Colors.white : AuroraColors.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}
