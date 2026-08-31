// Proxy — встроенный VPN-клиент. Портирован из kit/Proxy.jsx (упрощённая версия)
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../theme/aurora_theme.dart';
import '../../widgets/common.dart';
import '../../widgets/takami_icon.dart';

class ProxyScreen extends StatefulWidget {
  final VoidCallback onBack;
  const ProxyScreen({super.key, required this.onBack});

  @override
  State<ProxyScreen> createState() => _ProxyScreenState();
}

class _ProxyScreenState extends State<ProxyScreen> {
  bool connected = true;
  int activeServerId = 1;
  String dns = 'off';

  void _t(String m) => ToastController.show(context, m);

  @override
  Widget build(BuildContext context) {
    final activeServer = TakamiDB.proxyServers.firstWhere(
      (s) => s.id == activeServerId,
    );

    return Column(
      children: [
        TakamiAppBar(
          onBack: widget.onBack,
          title: 'Proxy / VPN',
          actions: [
            TakamiAction(
              icon: TIcon.menu,
              onClick: () => _t('Импорт · Экспорт конфигов · Статистика'),
            ),
          ],
        ),
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Connection status card
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    gradient: connected
                        ? const LinearGradient(
                            colors: [Color(0xFF1F4636), Color(0xFF141821)],
                          )
                        : const LinearGradient(
                            colors: [Color(0xFF3B2A2A), Color(0xFF141821)],
                          ),
                    borderRadius: BorderRadius.circular(AuroraRadii.l),
                    border: Border.all(color: AuroraColors.brd),
                  ),
                  child: Column(
                    children: [
                      Container(
                        width: 72,
                        height: 72,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: connected
                              ? AuroraColors.ok.withValues(alpha: 0.15)
                              : Colors.white10,
                          border: Border.all(
                            color: connected ? AuroraColors.ok : Colors.white24,
                            width: 2,
                          ),
                        ),
                        child: TakamiIcon(
                          TIcon.shield,
                          size: 32,
                          color: connected ? AuroraColors.ok : Colors.white54,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Text(
                        connected ? 'Защищено' : 'Не защищено',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w700,
                          color: connected ? AuroraColors.ok : Colors.white,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        connected
                            ? '${activeServer.name} · ${activeServer.ping} ms'
                            : 'Нажмите, чтобы подключиться',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AuroraColors.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton(
                          onPressed: () {
                            setState(() => connected = !connected);
                            _t(
                              connected
                                  ? 'Подключено к ${activeServer.name}'
                                  : 'Отключено',
                            );
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: connected
                                ? Colors.white10
                                : AuroraColors.acc,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          child: Text(
                            connected ? 'Отключить' : 'Подключиться',
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                const Text(
                  'Серверы',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 10),
                ...TakamiDB.proxyServers.map((s) {
                  final isActive = s.id == activeServerId;
                  return GestureDetector(
                    onTap: () {
                      setState(() => activeServerId = s.id);
                      _t('Выбран сервер: ${s.name}');
                    },
                    child: Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: isActive
                            ? AuroraColors.acc.withValues(alpha: 0.1)
                            : AuroraColors.sub,
                        border: Border.all(
                          color: isActive
                              ? AuroraColors.acc.withValues(alpha: 0.4)
                              : AuroraColors.brd,
                        ),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 8,
                            height: 8,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: s.ping < 60
                                  ? AuroraColors.ok
                                  : s.ping < 100
                                  ? AuroraColors.warn
                                  : AuroraColors.error,
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  s.name,
                                  style: const TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.white,
                                  ),
                                ),
                                Text(
                                  '${s.type} · ${s.host}',
                                  style: const TextStyle(
                                    fontSize: 10.5,
                                    color: AuroraColors.onSurfaceVariant,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Text(
                            '${s.ping} ms',
                            style: const TextStyle(
                              fontSize: 11.5,
                              color: AuroraColors.onSurfaceVariant,
                            ),
                          ),
                          if (isActive)
                            const Padding(
                              padding: EdgeInsets.only(left: 8),
                              child: TakamiIcon(
                                TIcon.check,
                                size: 14,
                                color: AuroraColors.acc,
                              ),
                            ),
                        ],
                      ),
                    ),
                  );
                }),
                const SizedBox(height: 20),
                const Text(
                  'Настройки',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 8),
                SettingsGroup(
                  children: [
                    SegRow<String>(
                      label: 'DoH DNS',
                      value: dns,
                      options: const [
                        ('off', 'Off'),
                        ('cloudflare', 'CF'),
                        ('google', 'Google'),
                      ],
                      onChanged: (v) => setState(() => dns = v),
                    ),
                    ActionRow(
                      label: 'Добавить сервер',
                      onClick: () =>
                          _t('Введите конфигурацию WireGuard/OpenVPN'),
                    ),
                    ActionRow(
                      label: 'Импорт конфигурации',
                      onClick: () => _t('Выберите файл .conf или .ovpn'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
