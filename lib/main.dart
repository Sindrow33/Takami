import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'state/app_state.dart';
import 'theme/aurora_theme.dart';
import 'screens/onboarding/onboarding_flow.dart';
import 'screens/main_shell.dart';

void main() {
  runApp(const TakamiApp());
}

class TakamiApp extends StatelessWidget {
  const TakamiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppState(),
      child: MaterialApp(
        title: 'Takami',
        debugShowCheckedModeBanner: false,
        theme: buildAuroraTheme(),
        home: const _RootRouter(),
      ),
    );
  }
}

class _RootRouter extends StatelessWidget {
  const _RootRouter();

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, app, _) {
        if (app.loading) {
          return const Scaffold(
            backgroundColor: AuroraColors.surface,
            body: Center(
              child: CircularProgressIndicator(color: AuroraColors.acc),
            ),
          );
        }
        if (!app.onboarded) {
          return OnboardingFlow(onDone: () => app.completeOnboarding());
        }
        return const MainShell();
      },
    );
  }
}
