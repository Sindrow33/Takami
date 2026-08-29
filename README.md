# anime-player

Модуль видеоплеера для аниме-приложения. Референс по UX/архитектуре плеера —
[Tadami (форк Aniyomi)](https://github.com/andarcanum/Tadami-Aniyomi-fork):
оттуда взят подход "координатор + дисковый кэш + progress-стейт" для фоновых
пайплайнов (там он используется для перевода субтитров, здесь — для ASR и дубляжа).

- `player/` — библиотека: ядро за интерфейсом `PlayerEngine`, Compose-оверлей, скип-сегменты,
  ASR-субтитры (`asr/`), AI-озвучка (`dub/`), общая модель субтитров (`subtitle/`),
  связующий слой `enhance/PlaybackEnhancer`
- `demo/` — отдельное приложение для проверки плеера без парсеров источников

Сборка идёт в GitHub Actions: официальных `aapt2`/`d8` под aarch64 в Android SDK нет,
поэтому на телефоне только редактирование.
APK: Actions → последний run → Artifacts → `demo-apk`.

## Архитектура фоновых пайплайнов (ASR / дубляж)

Оба модуля симметричны и не блокируют воспроизведение:

- `asr/AsrCoordinator` — генерирует субтитры окнами (`windowMs`) с префетчем
  вперёд позиции плеера (`prefetchAheadMs`), кэширует готовый документ на диск
  (`AsrDiskCache`, ключ = видео + движок + язык). `AsrEngine`/`AudioSource` —
  интерфейсы под конкретный backend (sherpa-onnx/whisper.cpp) и декод PCM.
- `dub/DubCoordinator` — синтезирует реплики (`DubLine`, из обычных субтитров
  или из ASR через `PlaybackEnhancer.dubLinesFromAsr`) с тем же принципом
  префетча, кэширует аудио-клипы по хэшу (текст+голос+движок) в `DubDiskCache`.
  `RoundRobinVoiceMapper` закрепляет голос за спикером на весь эпизод.
- `dub/DubDuckingController` — плавно приглушает громкость оригинала
  ([PlayerEngine.setVolume]) на время активной реплики озвучки и возвращает
  её обратно, без резких щелчков (настраиваемый fade).
- `enhance/PlaybackEnhancer` — единая точка, которую дёргает UI на каждый тик
  позиции: двигает ASR/дубляж вперёд, запускает/останавливает синтезированный
  клип через `SynthesizedAudioPlayer`, применяет ducking.

Всё протестировано юнит-тестами на фейковых `AsrEngine`/`TtsProvider`/
`AudioSource`/`SynthesizedAudioPlayer` — без сети и без реальных моделей.

## Дальше (для реальных сборок на устройстве)
- [x] AniSkipProvider вместо FakeSkipProvider (+ маппинг AniList→MAL, см. `skip/net/`)
- [x] ASR-субтитры — координатор с префетчем и кэшем готов (`asr/`), нужно подключить
      реальный `AsrEngine` (sherpa-onnx / whisper.cpp JNI) и `AudioSource` (декод PCM
      через MediaExtractor/FFmpeg — ExoPlayer не отдаёт произвольные окна сэмплов)
- [x] TTS-озвучка закадром с приглушением оригинала — координатор + ducking готовы
      (`dub/`), нужен реальный `TtsProvider` (облачный TTS или on-device) и
      `SynthesizedAudioPlayer` (AudioTrack/MediaPlayer поверх PCM/AAC из TTS)
- [ ] парсер ASS-стилей для определения OP/ED по караоке-тегам
- [ ] подмена ядра на libmpv для полноценного рендера ASS
- [ ] подключить `enhance/PlaybackEnhancer` к `PlayerScreen`/`Media3Engine.ticker`
      (сейчас это независимый, покрытый тестами слой, ещё не завязанный на UI)
