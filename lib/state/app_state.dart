import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum AppScreen {
  home,
  library,
  calendar,
  settings,
  title,
  character,
  reader,
  player,
  sources,
  swipes,
  search,
  proxy,
}

class AppState extends ChangeNotifier {
  bool onboarded = false;
  bool loading = true;

  AppScreen screen = AppScreen.home;
  AppScreen prevScreen = AppScreen.home;
  AppScreen activeTab = AppScreen.home;

  int? openedTitleId;
  int? openedCharId;

  bool fabLoading = false;
  bool focusAi = false;

  int screenKey = 0;

  AppState() {
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    onboarded = prefs.getBool('takami:onboarded') ?? false;
    loading = false;
    notifyListeners();
  }

  Future<void> completeOnboarding() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('takami:onboarded', true);
    onboarded = true;
    notifyListeners();
  }

  Future<void> resetOnboarding() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('takami:onboarded', false);
    onboarded = false;
    notifyListeners();
  }

  void navigate(AppScreen next, {int? titleId, int? charId}) {
    prevScreen = screen;
    screen = next;
    if (titleId != null) openedTitleId = titleId;
    if (charId != null) openedCharId = charId;
    if ([
      AppScreen.home,
      AppScreen.library,
      AppScreen.calendar,
      AppScreen.settings,
    ].contains(next)) {
      activeTab = next;
    }
    screenKey++;
    notifyListeners();
  }

  void back() {
    navigate(prevScreen == screen ? AppScreen.home : prevScreen);
  }

  void setFabLoading(bool v) {
    fabLoading = v;
    notifyListeners();
  }

  void goSettingsFocusAi() {
    focusAi = true;
    navigate(AppScreen.settings);
  }

  void clearFocusAi() {
    focusAi = false;
  }
}
