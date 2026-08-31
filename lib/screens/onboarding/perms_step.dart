import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/takami_icon.dart';
import 'onboarding_flow.dart';

class PermsStep extends StatelessWidget {
  final Map<String, bool> perms;
  final void Function(String) onGrant;
  final bool allGranted;
  final int curIdx;
  final int total;
  final VoidCallback onNext;

  const PermsStep({
    super.key,
    required this.perms,
    required this.onGrant,
    required this.allGranted,
    required this.curIdx,
    required this.total,
    required this.onNext,
  });

  @override
  Widget build(BuildContext context) {
    final items = [
      (
        'notify',
        TIcon.bell,
        'Уведомления',
        'О выходе новых глав и эпизодов из вашей библиотеки.',
      ),
      (
        'storage',
        TIcon.folder,
        'Доступ к хранилищу',
        'Для оффлайн-загрузки глав и эпизодов, экспорта бэкапов.',
      ),
      (
        'battery',
        TIcon.battery,
        'Без экономии заряда',
        'Чтобы автопарсер и загрузки не отключались в фоне.',
      ),
    ];

    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 44, 24, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ProgressDots(curIdx: curIdx, total: total),
          const SizedBox(height: 24),
          const Text(
            'Нужны разрешения',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              fontFamily: AuroraFonts.display,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Дайте согласие сейчас, потом настройки можно поменять в системе.',
            style: TextStyle(
              fontSize: 13,
              color: AuroraColors.onSurfaceVariant,
              height: 1.5,
            ),
          ),
          const SizedBox(height: 20),
          ...items.map((it) {
            final (key, icon, title, desc) = it;
            final granted = perms[key] ?? false;
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: GestureDetector(
                onTap: () => onGrant(key),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AuroraColors.sub,
                    border: Border.all(color: AuroraColors.brd),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Row(
                    children: [
                      AnimatedContainer(
                        duration: AuroraMotion.dMid,
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(12),
                          gradient: granted
                              ? const LinearGradient(
                                  colors: [
                                    Color(0xFF3DD68C),
                                    Color(0xFF24A566),
                                  ],
                                )
                              : const LinearGradient(
                                  colors: [
                                    Color(0x387C5CFF),
                                    Color(0x1A5B3BE8),
                                  ],
                                ),
                          border: Border.all(
                            color: granted
                                ? Colors.transparent
                                : const Color(0x477C5CFF),
                          ),
                          boxShadow: granted
                              ? [
                                  BoxShadow(
                                    color: const Color(0x803DD68C),
                                    blurRadius: 16,
                                  ),
                                ]
                              : null,
                        ),
                        child: TakamiIcon(
                          icon,
                          color: granted ? Colors.white : AuroraColors.acc2,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              title,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              desc,
                              style: const TextStyle(
                                fontSize: 11,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 7,
                        ),
                        decoration: BoxDecoration(
                          color: granted
                              ? const Color(0x263DD68C)
                              : AuroraColors.acc,
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          granted ? 'Выдано' : 'Разрешить',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w600,
                            color: granted ? AuroraColors.ok : Colors.white,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: onNext,
              style: ElevatedButton.styleFrom(
                backgroundColor: AuroraColors.acc,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
                elevation: 0,
              ),
              child: Text(
                allGranted ? 'Отлично, дальше' : 'Пропустить и продолжить',
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
          if (!allGranted)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Center(
                child: Text(
                  'Некоторые функции могут работать некорректно',
                  style: TextStyle(
                    fontSize: 11,
                    color: Colors.white.withValues(alpha: 0.4),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
