# Manga / Anime / Ranobe — UI prototype

HTML-прототип интерфейса перед портированием на Kotlin + Jetpack Compose.
Не приложение: сеть и парсеры не подключены, данные в data.js, состояние в localStorage.

## Запуск
    busybox httpd -p 8080 -h .
    # → http://127.0.0.1:8080/hub.html

hub.html — карта всех экранов.

## Структура
tokens.css / anim.css — дизайн-токены и анимации
data.js / app.js / cfg.js — данные, хелперы, реестр настроек
srcpick.js — автовыбор источника по максимальному номеру главы
migrate.js — перенос прогресса между источниками
hist.js / calendar.js — история и расписание релизов
stats.js — статистика и геймификация
auth.js — макет входа (не настоящая авторизация)
