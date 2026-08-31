import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/takami_icon.dart';
import 'onboarding_flow.dart';

class PolicyStep extends StatelessWidget {
  final bool agreed;
  final ValueChanged<bool> onAgreedChanged;
  final int curIdx;
  final int total;
  final VoidCallback onNext;

  const PolicyStep({
    super.key,
    required this.agreed,
    required this.onAgreedChanged,
    required this.curIdx,
    required this.total,
    required this.onNext,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 44, 24, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ProgressDots(curIdx: curIdx, total: total),
          const SizedBox(height: 24),
          const Text(
            'Пара слов, прежде чем начнём',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              fontFamily: AuroraFonts.display,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Takami — открытый клиент. Мы уважаем вас и просим уважать наши условия.',
            style: TextStyle(
              fontSize: 13,
              color: AuroraColors.onSurfaceVariant,
              height: 1.6,
            ),
          ),
          const SizedBox(height: 20),
          _card(
            'Мы не хостим контент',
            'Приложение — только инструмент просмотра. Все главы, эпизоды и тексты загружаются с внешних источников через открытые парсеры. Права на контент принадлежат их владельцам.',
          ),
          const SizedBox(height: 10),
          _card(
            'Встроенный VPN — для удобства',
            'Работает как обычный прокси-клиент. Мы не логируем трафик и не храним историю запросов на серверах. Ключи хранятся только на вашем устройстве.',
          ),
          const SizedBox(height: 10),
          _card(
            'За контент отвечает источник',
            'Если парсер сломался или источник ушёл — это не вина приложения. Автопарсер попробует восстановиться, но чуда не гарантируем.',
          ),
          const SizedBox(height: 20),
          GestureDetector(
            onTap: () => onAgreedChanged(!agreed),
            child: AnimatedContainer(
              duration: AuroraMotion.dMid,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: agreed
                    ? const Color(0x0F7C5CFF)
                    : const Color(0x0F7C5CFF),
                border: Border.all(
                  color: agreed
                      ? AuroraColors.acc.withValues(alpha: 0.5)
                      : const Color(0x337C5CFF),
                ),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AnimatedContainer(
                    duration: AuroraMotion.dFast,
                    width: 22,
                    height: 22,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(6),
                      gradient: agreed
                          ? const LinearGradient(
                              colors: [
                                AuroraColors.accGradA,
                                AuroraColors.accGradB,
                              ],
                            )
                          : null,
                      border: Border.all(
                        color: agreed
                            ? Colors.transparent
                            : AuroraColors.outline,
                      ),
                    ),
                    child: agreed
                        ? const TakamiIcon(
                            TIcon.check,
                            size: 16,
                            color: Colors.white,
                            strokeWidth: 3,
                          )
                        : null,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: RichText(
                      text: TextSpan(
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: Colors.white,
                          height: 1.4,
                        ),
                        children: const [
                          TextSpan(
                            text:
                                'Я прочитал(а) и согласен(-а). Претензий по контенту к приложению ',
                          ),
                          TextSpan(
                            text: 'не имею',
                            style: TextStyle(
                              color: AuroraColors.acc2,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          TextSpan(text: '.'),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: agreed ? onNext : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: AuroraColors.acc,
                disabledBackgroundColor: Colors.white.withValues(alpha: 0.06),
                foregroundColor: Colors.white,
                disabledForegroundColor: Colors.white.withValues(alpha: 0.35),
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
                elevation: 0,
              ),
              child: const Text(
                'Продолжить',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _card(String title, String desc) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.03),
        border: Border.all(color: AuroraColors.brd),
        borderRadius: BorderRadius.circular(14),
      ),
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
          const SizedBox(height: 4),
          Text(
            desc,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xB8FFFFFF),
              height: 1.55,
            ),
          ),
        ],
      ),
    );
  }
}
