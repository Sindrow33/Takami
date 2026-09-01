# `:app` — оболочка приложения Takami

Модуль-контейнер: тема Aurora, онбординг, главная, таббар. Модульные ветки
(`manga-reader`, `anime-player`, `anime-scene-search`, `autoheal`) подключаются
сюда как экраны — пока на их местах `ModulePlaceholder`.

## Что реализовано по `design_handoff_takami_v4` (v4.1)

| Раздел хендоффа | Где в коде |
|---|---|
| Design Tokens (цвета, радиусы, motion) | `ui/theme/Tokens.kt` |
| Тип-шкала, `MaterialTheme` | `ui/theme/Theme.kt` |
| Icons Set (inline SVG → Canvas) | `ui/components/Icons.kt` |
| 0.1 Splash (авто-переход 1800 мс, `logoBreath`) | `onboarding/Onboarding.kt` |
| 0.2 Policy (3 карточки, чекбокс, CTA disabled) | `onboarding/Onboarding.kt` |
| 0.3 Permissions (bell / folder / battery, чипы) | `onboarding/Onboarding.kt` |
| 0.4 Welcome (ореолы, иероглифы, пузырёк, CTA-пульс) | `onboarding/Onboarding.kt` |
| 1. Home: топ-бар, hero, quick actions, рельсы | `home/HomeScreen.kt` |
| 2. AI Indicator + bottom-sheet, кольцо, лог | `home/AiIndicator.kt` |
| 3. TabBar с FAB (spinner 1200 мс, `fab-pulse-strong`) | `ui/components/TabBar.kt` |
| Screen transitions (cascadeIn) | `MainActivity.kt` (`AnimatedContent`) |
| `localStorage` → `SharedPreferences` | `data/TakamiPrefs.kt` |
| Моки `kit/data.js` | `home/Data.kt` |

## Ещё не перенесено

- Welcome-иллюстрация (`assets/welcome-girl.png`) и искры — нужен ассет в `res/drawable`.
- Шрифты Zen Kaku Gothic Antique / JetBrains Mono — пока системный sans.
- Календарь с цветными точками, настройки с Support-кнопкой, Library / Reader /
  Player / Search / Sources / Proxy — приезжают из модульных ветек.

## Сборка

CI: workflow `app` (`.github/workflows/app.yml`) — юнит-тесты + debug APK
в артефакте `takami-app-debug`.

Локально:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
