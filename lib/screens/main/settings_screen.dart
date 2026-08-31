// Settings — портирована из kit/Settings.jsx (без ai-key блока: заменён на упрощённый)
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../state/app_state.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/support_button.dart';
import '../../widgets/takami_icon.dart';

class SettingsScreen extends StatefulWidget {
  final void Function(AppScreen) onGo;
  const SettingsScreen({super.key, required this.onGo});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  // Внешний вид
  String theme = 'dark';
  String accent = 'violet';
  bool reducedMotion = false;
  // Библиотека
  String libLayout = 'grid';
  String sortBy = 'updated';
  bool coverBadges = true;
  bool showUnread = true;
  // Источники
  bool autoSource = true;
  String preferLang = 'ru';
  // Загрузки
  bool wifiOnly = true;
  bool dlAuto = false;
  String dlLimit = '3';
  String dlLocation = 'internal';
  // Трекеры
  bool trackMal = false;
  bool trackAnilist = true;
  bool trackShiki = false;
  // Уведомления
  bool notifyRelease = true;
  bool groupNotify = true;
  bool dndNight = true;
  // Приватность
  bool history = true;
  bool spoilers = true;
  bool blurNsfw = false;
  // Безопасность
  bool pin = false;
  bool biometric = false;
  // Сеть
  String dohProvider = 'off';
  // Хранилище
  int cacheLimit = 512;
  // Продвинутые
  bool verboseLogs = false;

  void _t(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TakamiAppBar(
          title: 'Настройки',
          actions: [
            TakamiAction(
              icon: TIcon.search,
              onClick: () =>
                  _t('Введите название настройки для быстрого перехода'),
            ),
          ],
        ),
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Профиль
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  child: Row(
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(999),
                        child: Image.asset(
                          'assets/images/logo.jpg',
                          width: 52,
                          height: 52,
                          fit: BoxFit.cover,
                        ),
                      ),
                      const SizedBox(width: 14),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Читатель',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w700,
                                color: Colors.white,
                              ),
                            ),
                            Text(
                              'Локальный профиль · без учётной записи',
                              style: TextStyle(
                                fontSize: 11.5,
                                color: AuroraColors.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),

                const SectionHead(title: 'Внешний вид'),
                SettingsGroup(
                  children: [
                    SegRow<String>(
                      label: 'Тема',
                      value: theme,
                      options: const [
                        ('dark', 'Тёмная'),
                        ('light', 'Светлая'),
                        ('amoled', 'AMOLED'),
                      ],
                      onChanged: (v) => setState(() => theme = v),
                    ),
                    SegRow<String>(
                      label: 'Цвет акцента',
                      value: accent,
                      options: const [
                        ('violet', 'Aurora'),
                        ('cyan', 'Cyan'),
                        ('amber', 'Amber'),
                      ],
                      onChanged: (v) => setState(() => accent = v),
                    ),
                    SwitchRow(
                      label: 'Уменьшенная анимация',
                      sub: 'Оставит только базовые переходы',
                      value: reducedMotion,
                      onChanged: (v) => setState(() => reducedMotion = v),
                    ),
                  ],
                ),

                const SectionHead(title: 'Библиотека'),
                SettingsGroup(
                  children: [
                    SegRow<String>(
                      label: 'Вид',
                      value: libLayout,
                      options: const [
                        ('grid', 'Сетка'),
                        ('compact', 'Комп.'),
                        ('list', 'Список'),
                      ],
                      onChanged: (v) => setState(() => libLayout = v),
                    ),
                    SegRow<String>(
                      label: 'Сортировка',
                      value: sortBy,
                      options: const [
                        ('updated', 'Обновл.'),
                        ('added', 'Добавл.'),
                        ('title', 'Назв.'),
                      ],
                      onChanged: (v) => setState(() => sortBy = v),
                    ),
                    SwitchRow(
                      label: 'Бейджи на обложках',
                      sub: 'Число новых глав, отметки NEW, err и off',
                      value: coverBadges,
                      onChanged: (v) => setState(() => coverBadges = v),
                    ),
                    SwitchRow(
                      label: 'Показывать непрочитанные',
                      value: showUnread,
                      onChanged: (v) => setState(() => showUnread = v),
                    ),
                    ActionRow(
                      label: 'Категории',
                      right: '4 категории',
                      onClick: () =>
                          _t('Категории: Читаю · Планы · Заброшено · Любимые'),
                    ),
                  ],
                ),

                const SectionHead(title: 'Источники'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'Автовыбор источника',
                      sub: 'Берётся живой источник с наибольшим номером главы',
                      value: autoSource,
                      onChanged: (v) => setState(() => autoSource = v),
                    ),
                    SegRow<String>(
                      label: 'Приоритет языка',
                      value: preferLang,
                      options: const [
                        ('ru', 'Русский'),
                        ('en', 'English'),
                        ('any', 'Любой'),
                      ],
                      onChanged: (v) => setState(() => preferLang = v),
                    ),
                    ActionRow(
                      label: 'Управление источниками',
                      right: '6 установлено',
                      onClick: () => widget.onGo(AppScreen.sources),
                    ),
                  ],
                ),

                const SectionHead(title: 'Загрузки'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'Только по Wi-Fi',
                      value: wifiOnly,
                      onChanged: (v) => setState(() => wifiOnly = v),
                    ),
                    SwitchRow(
                      label: 'Автозагрузка новых глав',
                      sub: 'Для тайтлов в библиотеке',
                      value: dlAuto,
                      onChanged: (v) => setState(() => dlAuto = v),
                    ),
                    SegRow<String>(
                      label: 'Одновременных загрузок',
                      value: dlLimit,
                      options: const [('1', '1'), ('3', '3'), ('5', '5')],
                      onChanged: (v) => setState(() => dlLimit = v),
                    ),
                    SegRow<String>(
                      label: 'Куда сохранять',
                      value: dlLocation,
                      options: const [
                        ('internal', 'Внутр.'),
                        ('sd', 'SD'),
                        ('ask', 'Спраш.'),
                      ],
                      onChanged: (v) => setState(() => dlLocation = v),
                    ),
                    ActionRow(
                      label: 'Управление загрузками',
                      right: '6 глав · 42 МБ',
                      onClick: () => _t('Открываем список активных загрузок'),
                    ),
                  ],
                ),

                const SectionHead(title: 'Отслеживание'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'MyAnimeList',
                      sub: 'MAL · рейтинг, прогресс, планы',
                      value: trackMal,
                      onChanged: (v) => setState(() => trackMal = v),
                    ),
                    SwitchRow(
                      label: 'AniList',
                      sub: 'Синхронизация двусторонняя',
                      value: trackAnilist,
                      onChanged: (v) => setState(() => trackAnilist = v),
                    ),
                    SwitchRow(
                      label: 'Shikimori',
                      sub: 'Шикимори · русскоязычный трекер',
                      value: trackShiki,
                      onChanged: (v) => setState(() => trackShiki = v),
                    ),
                  ],
                ),

                const SectionHead(title: 'Уведомления'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'Оповещать о выходе',
                      sub: 'По расчётному расписанию из календаря',
                      value: notifyRelease,
                      onChanged: (v) => setState(() => notifyRelease = v),
                    ),
                    SwitchRow(
                      label: 'Группировать в одно уведомление',
                      value: groupNotify,
                      onChanged: (v) => setState(() => groupNotify = v),
                    ),
                    SwitchRow(
                      label: 'Не беспокоить ночью',
                      sub: 'С 23:00 до 08:00',
                      value: dndNight,
                      onChanged: (v) => setState(() => dndNight = v),
                    ),
                  ],
                ),

                const SectionHead(title: 'Приватность'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'Вести историю чтения',
                      value: history,
                      onChanged: (v) => setState(() => history = v),
                    ),
                    SwitchRow(
                      label: 'Скрывать спойлеры',
                      sub:
                          'Биографии персонажей и комментарии размываются до нажатия',
                      value: spoilers,
                      onChanged: (v) => setState(() => spoilers = v),
                    ),
                    SwitchRow(
                      label: 'Размывать обложки 18+',
                      value: blurNsfw,
                      onChanged: (v) => setState(() => blurNsfw = v),
                    ),
                    ActionRow(
                      label: 'Очистить историю',
                      danger: true,
                      onClick: () => _t('История чтения очищена'),
                    ),
                  ],
                ),

                const SectionHead(title: 'Безопасность'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'PIN-код на вход',
                      sub: pin ? 'Установлен · четыре цифры' : 'Не установлен',
                      value: pin,
                      onChanged: (v) => setState(() => pin = v),
                    ),
                    SwitchRow(
                      label: 'Биометрия',
                      sub: 'Отпечаток или Face Unlock вместо PIN',
                      value: biometric,
                      onChanged: (v) => setState(() => biometric = v),
                    ),
                    ActionRow(
                      label: 'Заблокировать сейчас',
                      onClick: () =>
                          _t('Приложение заблокировано · разблокируйте PIN-ом'),
                    ),
                  ],
                ),

                const SectionHead(title: 'Сеть'),
                SettingsGroup(
                  children: [
                    ActionRow(
                      label: 'Proxy / VPN',
                      sub: 'Aurora VPN · Amsterdam · WireGuard',
                      right: 'вкл',
                      onClick: () => widget.onGo(AppScreen.proxy),
                    ),
                    SegRow<String>(
                      label: 'DoH DNS',
                      value: dohProvider,
                      options: const [
                        ('off', 'Off'),
                        ('cloudflare', 'CF'),
                        ('google', 'Google'),
                      ],
                      onChanged: (v) => setState(() => dohProvider = v),
                    ),
                  ],
                ),

                const SectionHead(title: 'Хранилище'),
                SettingsGroup(
                  children: [
                    ActionRow(
                      label: 'Кеш изображений',
                      right: '184 МБ',
                      onClick: () => _t('184 МБ · 1 247 файлов'),
                    ),
                    SegRow<String>(
                      label: 'Лимит кеша',
                      value: '$cacheLimit',
                      options: const [
                        ('256', '256'),
                        ('512', '512'),
                        ('1024', '1024'),
                      ],
                      onChanged: (v) =>
                          setState(() => cacheLimit = int.parse(v)),
                    ),
                    ActionRow(
                      label: 'Очистить кеш',
                      onClick: () => _t('Кеш очищен · 184 МБ освобождено'),
                    ),
                  ],
                ),

                const SectionHead(title: 'Резервное копирование'),
                SettingsGroup(
                  children: [
                    ActionRow(
                      label: 'Создать резервную копию',
                      sub: 'Прогресс, категории, источники, настройки',
                      onClick: () => _t('Создаём резервную копию…'),
                    ),
                    ActionRow(
                      label: 'Восстановить из файла',
                      onClick: () => _t('Выберите .bak файл'),
                    ),
                    ActionRow(
                      label: 'Автобэкапы',
                      right: 'раз в неделю',
                      onClick: () =>
                          _t('Автобэкапы каждое воскресенье в 03:00'),
                    ),
                  ],
                ),

                const SectionHead(title: 'Продвинутые'),
                SettingsGroup(
                  children: [
                    SwitchRow(
                      label: 'Подробные логи',
                      sub: 'Для отправки в поддержку',
                      value: verboseLogs,
                      onChanged: (v) => setState(() => verboseLogs = v),
                    ),
                    ActionRow(
                      label: 'Экспорт логов',
                      onClick: () => _t('Логи готовятся · takami-log.txt'),
                    ),
                    ActionRow(
                      label: 'Сбросить все настройки',
                      danger: true,
                      onClick: () =>
                          _t('Настройки сброшены к значениям по умолчанию'),
                    ),
                  ],
                ),

                const SectionHead(title: 'О приложении'),
                SettingsGroup(
                  children: [
                    GestureDetector(
                      onLongPress: () async {
                        final appState = context.read<AppState>();
                        await appState.resetOnboarding();
                        if (context.mounted)
                          _t('Онбординг сброшен · перезапустите приложение');
                      },
                      child: ActionRow(
                        label: 'Версия приложения',
                        right: '1.0.0',
                        onClick: () =>
                            _t('Takami · последняя версия установлена'),
                      ),
                    ),
                    ActionRow(
                      label: 'Лицензии с открытым кодом',
                      onClick: () => _t('Открываем список лицензий'),
                    ),
                  ],
                ),

                const SupportButton(),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
