// MainShell — контейнер навигации между главными экранами приложения.
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/models.dart';
import '../state/app_state.dart';
import '../theme/aurora_theme.dart';
import '../widgets/tab_bar.dart';
import 'main/home_screen.dart';
import 'main/library_screen.dart';
import 'main/calendar_screen.dart';
import 'main/settings_screen.dart';
import 'main/title_screen.dart';
import 'main/character_screen.dart';
import 'main/reader_screen.dart';
import 'main/player_screen.dart';
import 'main/sources_screen.dart';
import 'main/swipes_screen.dart';
import 'main/search_screen.dart';
import 'main/proxy_screen.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  void _onFabTap(AppState app) async {
    app.setFabLoading(true);
    await Future.delayed(const Duration(milliseconds: 1200));
    if (!mounted) return;
    app.setFabLoading(false);
    app.navigate(AppScreen.swipes);
  }

  bool _showsTabBar(AppScreen s) => [
    AppScreen.home,
    AppScreen.library,
    AppScreen.calendar,
    AppScreen.settings,
  ].contains(s);

  bool _isFullScreen(AppScreen s) =>
      [AppScreen.reader, AppScreen.player].contains(s);

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, app, _) {
        if (_isFullScreen(app.screen)) {
          return _buildScreen(app);
        }

        return Scaffold(
          backgroundColor: AuroraColors.surface,
          body: SafeArea(
            child: KeyedSubtree(
              key: ValueKey(app.screenKey),
              child: _buildScreen(app),
            ),
          ),
          bottomNavigationBar: _showsTabBar(app.screen)
              ? TakamiTabBar(
                  active: app.activeTab,
                  onNav: (s) => app.navigate(s),
                  onFab: () => _onFabTap(app),
                  fabLoading: app.fabLoading,
                  calendarBadge: 3,
                )
              : null,
        );
      },
    );
  }

  Widget _buildScreen(AppState app) {
    switch (app.screen) {
      case AppScreen.home:
        return HomeScreen(
          onGo: (s) => app.navigate(s),
          onOpenTitle: (t) => app.navigate(AppScreen.title, titleId: t.id),
        );
      case AppScreen.library:
        return LibraryScreen(
          onGo: (s) => app.navigate(s),
          onOpenTitle: (t) => app.navigate(AppScreen.title, titleId: t.id),
        );
      case AppScreen.calendar:
        return CalendarScreen(
          onOpenTitle: (t) => app.navigate(AppScreen.title, titleId: t.id),
        );
      case AppScreen.settings:
        return SettingsScreen(onGo: (s) => app.navigate(s));
      case AppScreen.title:
        return TitleDetailScreen(
          itemId: app.openedTitleId ?? 1,
          onBack: () => app.back(),
          onRead: (t) => app.navigate(
            t.type == ContentType.anime ? AppScreen.player : AppScreen.reader,
          ),
          onOpenChar: (c) => app.navigate(AppScreen.character, charId: c.id),
        );
      case AppScreen.character:
        return CharacterScreen(
          charId: app.openedCharId ?? 1,
          onBack: () => app.back(),
          onOpenTitle: (t) => app.navigate(AppScreen.title, titleId: t.id),
        );
      case AppScreen.reader:
        return ReaderScreen(onBack: () => app.back());
      case AppScreen.player:
        return PlayerScreen(onBack: () => app.back());
      case AppScreen.sources:
        return SourcesScreen(onBack: () => app.back());
      case AppScreen.swipes:
        return SwipesScreen(onBack: () => app.back());
      case AppScreen.search:
        return SearchScreen(
          onBack: () => app.back(),
          onOpenTitle: (t) => app.navigate(AppScreen.title, titleId: t.id),
        );
      case AppScreen.proxy:
        return ProxyScreen(onBack: () => app.back());
    }
  }
}
