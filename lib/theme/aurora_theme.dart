// Takami — Aurora Design Tokens
// Портированы из design_handoff_takami_v4/colors_and_type.css
import 'package:flutter/material.dart';

class AuroraColors {
  AuroraColors._();

  // ---------- Акценты ----------
  static const acc = Color(0xFF7C5CFF); // Violet — primary
  static const accDim = Color(0xFF5B3BE8); // VioletDim — pressed
  static const acc2 = Color(0xFFA78BFA); // VioletSoft
  static const acc3 = Color(0xFF00E5FF); // Cyan — secondary
  static const accBlue = Color(0xFF0095FF); // Blue — tertiary
  static const accGradA = Color(0xFF8E72FF);
  static const accGradB = Color(0xFF5B3BE8);

  static const primary = acc;
  static const onPrimary = Color(0xFFFFFFFF);

  // ---------- Семафор ----------
  static const ok = Color(0xFF3DD68C);
  static const warn = Color(0xFFFFB020);
  static const error = Color(0xFFF87171);
  static const errorStrong = Color(0xFFFF5C5C);

  // ---------- Поверхности (dark) ----------
  static const surface = Color(0xFF0F1116);
  static const surfaceContainer = Color(0xFF1A1D23);
  static const surfaceVariant = Color(0xFF252931);
  static const scLowest = Color(0xFF0A0C0F);
  static const scLow = Color(0xFF13151A);
  static const scHigh = Color(0xFF24272E);
  static const scHighest = Color(0xFF2F3239);

  // ---------- Текст ----------
  static const onSurface = Color(0xFFFFFFFF);
  static const onSurfaceVariant = Color(0xFF94A3B8);

  // ---------- Обводки ----------
  static const outline = Color(0xFF334155);
  static const outlineVar = Color(0xFF1E293B);

  // ---------- Стекло / субстраты ----------
  static const sub = Color(0x0DFFFFFF); // rgba(255,255,255,.05)
  static const glass = Color(0x38FFFFFF); // rgba(255,255,255,.22)
  static const brd = Color(0x14FFFFFF); // rgba(255,255,255,.08)
  static const brdEm = Color(0x29FFFFFF); // rgba(255,255,255,.16)

  // ---------- Семантика типов контента ----------
  static const typeAnime = Color(0xFF0095FF);
  static const typeManga = Color(0xFF3DD68C);
  static const typeNovel = Color(0xFFA78BFA);

  static const heartDonate = Color(0xFFFF6B8A);

  // ---------- Градиенты ----------
  static const heroGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF1E1B4B), surface],
  );

  static const fabGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [accGradA, accGradB],
  );

  static const bubbleGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFFFFFFFF), Color(0xFFF4EBFF)],
  );
}

class AuroraRadii {
  AuroraRadii._();
  static const s = 8.0;
  static const m = 12.0;
  static const l = 20.0;
  static const full = 999.0;
}

class AuroraSpacing {
  AuroraSpacing._();
  static const sp1 = 4.0;
  static const sp2 = 8.0;
  static const sp3 = 12.0;
  static const sp4 = 16.0;
  static const sp6 = 24.0;
  static const sp8 = 32.0;
}

class AuroraMotion {
  AuroraMotion._();
  static const dFast = Duration(milliseconds: 140);
  static const dMid = Duration(milliseconds: 240);
  static const dSlow = Duration(milliseconds: 420);
  static const ease = Cubic(0.2, 0.8, 0.2, 1);
  static const easeOut = Cubic(0.16, 1, 0.3, 1);
  static const bounce = Cubic(0.34, 1.56, 0.64, 1);
}

class AuroraFonts {
  AuroraFonts._();
  static const sans = 'Zen Kaku Gothic Antique';
  static const display = 'Zen Kaku Gothic Antique';
  static const mono = 'JetBrains Mono';
}

class AuroraShadows {
  AuroraShadows._();

  static List<BoxShadow> fab = const [
    BoxShadow(color: Color(0x8C5B3BE8), blurRadius: 24, offset: Offset(0, 8)),
    BoxShadow(color: Color(0x617C5CFF), blurRadius: 22),
  ];

  static List<BoxShadow> fabPulse = const [
    BoxShadow(color: Color(0xE65B3BE8), blurRadius: 40, offset: Offset(0, 12)),
    BoxShadow(color: Color(0xD97C5CFF), blurRadius: 50),
  ];

  static List<BoxShadow> ctaGlow = const [
    BoxShadow(color: Color(0x805B3BE8), blurRadius: 24, offset: Offset(0, 8)),
    BoxShadow(color: Color(0x597C5CFF), blurRadius: 24),
  ];

  static List<BoxShadow> ctaGlowPeak = const [
    BoxShadow(color: Color(0xBF5B3BE8), blurRadius: 32, offset: Offset(0, 12)),
    BoxShadow(color: Color(0x997C5CFF), blurRadius: 36),
  ];

  static List<BoxShadow> mdShadow = const [
    BoxShadow(color: Color(0x59000000), blurRadius: 24, offset: Offset(0, 8)),
  ];

  static List<BoxShadow> welcomeBubble = const [
    BoxShadow(color: Color(0x59000000), blurRadius: 24, offset: Offset(0, 8)),
    BoxShadow(color: Color(0x597C5CFF), blurRadius: 24),
  ];
}

ThemeData buildAuroraTheme() {
  final base = ThemeData.dark(useMaterial3: true);
  return base.copyWith(
    scaffoldBackgroundColor: AuroraColors.surface,
    primaryColor: AuroraColors.acc,
    colorScheme: base.colorScheme.copyWith(
      primary: AuroraColors.acc,
      secondary: AuroraColors.acc3,
      surface: AuroraColors.surface,
      error: AuroraColors.error,
      onSurface: AuroraColors.onSurface,
    ),
    textTheme: base.textTheme.apply(
      fontFamily: AuroraFonts.sans,
      bodyColor: AuroraColors.onSurface,
      displayColor: AuroraColors.onSurface,
    ),
    splashFactory: NoSplash.splashFactory,
    highlightColor: Colors.transparent,
    dividerColor: AuroraColors.brd,
    cardTheme: const CardThemeData(
      color: AuroraColors.surfaceContainer,
      elevation: 0,
    ),
    dialogTheme: const DialogThemeData(
      backgroundColor: AuroraColors.surfaceContainer,
    ),
  );
}
