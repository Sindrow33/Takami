// Модели данных Takami — портированы из kit/data.js
import 'package:flutter/material.dart';

enum ContentType { manga, anime, novel }

extension ContentTypeX on ContentType {
  String get ruName => switch (this) {
    ContentType.manga => 'Манга',
    ContentType.anime => 'Аниме',
    ContentType.novel => 'Ранобэ',
  };

  String get shortName => switch (this) {
    ContentType.manga => 'М',
    ContentType.anime => 'А',
    ContentType.novel => 'Р',
  };

  Color get color => switch (this) {
    ContentType.anime => const Color(0xFF0095FF),
    ContentType.manga => const Color(0xFF3DD68C),
    ContentType.novel => const Color(0xFFA78BFA),
  };
}

class TitleItem {
  final int id;
  final ContentType type;
  final String title;
  final String source;
  final int year;
  final double rating;
  final String count;
  final int progress; // 0-100
  final String sub;
  final String badge; // "12" | "NEW" | "off" | "err" | ""
  final String sourceUrl;
  final Gradient bg;
  final bool broken;

  const TitleItem({
    required this.id,
    required this.type,
    required this.title,
    required this.source,
    required this.year,
    required this.rating,
    required this.count,
    required this.progress,
    required this.sub,
    required this.badge,
    required this.sourceUrl,
    required this.bg,
    this.broken = false,
  });
}

class Franchise {
  final String id;
  final String title;
  final Gradient bg;
  final List<int> itemIds;
  final List<String> genres;
  final String description;

  const Franchise({
    required this.id,
    required this.title,
    required this.bg,
    required this.itemIds,
    required this.genres,
    required this.description,
  });
}

class Seiyuu {
  final int id;
  final String name;
  final String jp;
  final int birthYear;
  final int roles;
  final String note;

  const Seiyuu({
    required this.id,
    required this.name,
    required this.jp,
    required this.birthYear,
    required this.roles,
    required this.note,
  });
}

class CharacterItem {
  final int id;
  final String name;
  final String jp;
  final String role;
  final bool main;
  final int? age;
  final String height;
  final String birthday;
  final String zodiac;
  final String bloodType;
  final String affiliation;
  final String origin;
  final String bio;
  final List<String> quotes;
  final int seiyuuId;
  final List<int> appearsIn;

  const CharacterItem({
    required this.id,
    required this.name,
    required this.jp,
    required this.role,
    required this.main,
    required this.age,
    required this.height,
    required this.birthday,
    required this.zodiac,
    required this.bloodType,
    required this.affiliation,
    required this.origin,
    required this.bio,
    required this.quotes,
    required this.seiyuuId,
    required this.appearsIn,
  });
}

class NewsItem {
  final String id;
  final String category;
  final String title;
  final String sub;
  final String time;
  final String source;
  final Gradient bg;
  final String tone; // primary | cyan | warn | ok

  const NewsItem({
    required this.id,
    required this.category,
    required this.title,
    required this.sub,
    required this.time,
    required this.source,
    required this.bg,
    required this.tone,
  });
}

class SourceItem {
  final String name;
  final String type;
  final String version;
  final bool active;
  final String status; // ok | warn | err

  const SourceItem({
    required this.name,
    required this.type,
    required this.version,
    required this.active,
    required this.status,
  });
}

class ProxyServer {
  final int id;
  final String name;
  final String type;
  final String host;
  final int ping;
  final bool active;
  final String kind; // pinned | sub | manual

  const ProxyServer({
    required this.id,
    required this.name,
    required this.type,
    required this.host,
    required this.ping,
    required this.active,
    required this.kind,
  });
}
