// Onboarding — 4 экрана перед первым входом.
// Splash (1.8с) → Policy → Perms → Welcome → onDone()
import 'package:flutter/material.dart';
import '../../theme/aurora_theme.dart';
import 'splash_step.dart';
import 'policy_step.dart';
import 'perms_step.dart';
import 'welcome_step.dart';

enum OnbStep { splash, policy, perms, welcome }

class OnboardingFlow extends StatefulWidget {
  final VoidCallback onDone;
  const OnboardingFlow({super.key, required this.onDone});

  @override
  State<OnboardingFlow> createState() => _OnboardingFlowState();
}

class _OnboardingFlowState extends State<OnboardingFlow> {
  OnbStep step = OnbStep.splash;
  bool agreed = false;
  Map<String, bool> perms = {
    'notify': false,
    'storage': false,
    'battery': false,
  };

  @override
  void initState() {
    super.initState();
    Future.delayed(const Duration(milliseconds: 1800), () {
      if (mounted && step == OnbStep.splash) {
        setState(() => step = OnbStep.policy);
      }
    });
  }

  void _grant(String k) => setState(() => perms[k] = true);

  bool get allGranted => perms.values.every((v) => v);

  int get curIdx => OnbStep.values.indexOf(step);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AuroraColors.surface,
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF1E1B4B), AuroraColors.surface],
          ),
        ),
        child: SafeArea(
          child: AnimatedSwitcher(
            duration: AuroraMotion.dMid,
            child: KeyedSubtree(
              key: ValueKey(step),
              child: switch (step) {
                OnbStep.splash => const SplashStep(),
                OnbStep.policy => PolicyStep(
                  agreed: agreed,
                  onAgreedChanged: (v) => setState(() => agreed = v),
                  curIdx: curIdx,
                  total: 3,
                  onNext: () => setState(() => step = OnbStep.perms),
                ),
                OnbStep.perms => PermsStep(
                  perms: perms,
                  onGrant: _grant,
                  allGranted: allGranted,
                  curIdx: curIdx,
                  total: 3,
                  onNext: () => setState(() => step = OnbStep.welcome),
                ),
                OnbStep.welcome => WelcomeStep(onEnter: widget.onDone),
              },
            ),
          ),
        ),
      ),
    );
  }
}

/// Прогресс-точки, общий компонент — 3 точки, splash не отображается
class ProgressDots extends StatelessWidget {
  final int curIdx; // 0=policy,1=perms,2=welcome
  final int total;
  const ProgressDots({super.key, required this.curIdx, required this.total});

  @override
  Widget build(BuildContext context) {
    final cur = curIdx - 1; // splash не учитываем
    return Row(
      children: List.generate(total, (i) {
        final isOn = i == cur;
        final isDone = i < cur;
        return AnimatedContainer(
          duration: AuroraMotion.dMid,
          margin: const EdgeInsets.only(right: 6),
          width: isOn ? 22 : 6,
          height: 6,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(3),
            gradient: isOn
                ? const LinearGradient(
                    colors: [AuroraColors.acc2, AuroraColors.accDim],
                  )
                : null,
            color: isOn
                ? null
                : isDone
                ? const Color(0x667C5CFF)
                : Colors.white24,
          ),
        );
      }),
    );
  }
}
