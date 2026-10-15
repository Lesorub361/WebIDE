# WebIDE — Мобильная IDE с поддержкой Flutter, ИИ-агентами и Ubuntu 20.04

![Version](https://img.shields.io/badge/version-0.4.4-blue?style=flat-square)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue?style=flat-square)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-green?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Flutter](https://img.shields.io/badge/Flutter-Ready-02569B?style=flat-square)](https://flutter.dev/)
[![Subsystem](https://img.shields.io/badge/Subsystem-Ubuntu_20.04_PRoot-E95420?style=flat-square)](https://ubuntu.com/)
[![Agents](https://img.shields.io/badge/AI_Agents-Antigravity_|_Claude_|_DeepSeek-4285F4?style=flat-square)](https://github.com/Lesorub361/WebIDE)
[![License](https://img.shields.io/badge/License-GPLv3-orange?style=flat-square)](LICENSE)

[ [**Русский**] ] | [ [English](#english-overview) ]

**WebIDE** — это нативная Android-среда разработки (IDE) нового поколения, объединяющая возможности полноценного мобильного редактора кода, поддержку разработки на **Flutter & Dart**, изолированную Linux-подсистему **Ubuntu 20.04 LTS (PRoot)**, автономных **ИИ-агентов для кодинга** (Antigravity CLI, Claude Code, DeepSeek Harness) и глубокую интеграцию с **GitHub**.

---

## 🚀 Основные возможности WebIDE

### 💙 1. Полноценная разработка на Flutter & Dart
* **Создание проектов Flutter**: Генерация готовой структуры проекта (`pubspec.yaml`, `lib/main.dart`, `web/index.html`, `analysis_options.yaml`).
* **Flutter DevStack в Ubuntu**: Установка стабильного Flutter SDK и компилятора Dart в изолированную подсистему в один клик.
* **Панель инструментов Flutter**:
  * Загрузка пакетов и зависимостей (`flutter pub get`).
  * Запуск локального веб-сервера разработки (`flutter run -d web-server --web-port=8080`).
  * Сборка нативного Android APK (`flutter build apk`).
  * Анализ кода и проверка готовности окружения (`flutter doctor`, `flutter analyze`, `flutter clean`).

### 🖥 2. Встроенная нижняя панель (Docked Bottom Panel)
* Терминал больше не отдельный экран — теперь он доступен прямо снизу под кодом!
* **Интерактивный Терминал**: Полноценный терминал Linux с контекстом текущего проекта и быстрыми чипами (`ls -la`, `pwd`, `git status`, `clear`).
* **Вкладка «Проблемы» (Diagnostics)**: Список ошибок и предупреждений LSP-анализатора в коде в реальном времени с бейджем количества и кликом для мгновенного перехода к нужной строке.
* **Вкладка «Flutter»**: Панель быстрых действий для сборки и запуска приложений Flutter.
* **Вкладка «Сборка»**: Логи компилятора и процесса сборки.

### 📂 3. Новый проводник файлов и вкладки редактора
* **Проводник в стиле VS Code**:
  * Компактные отступы и наглядное дерево файлов.
  * Плавные поворотные шевроны папок.
  * Стильное выделение выбранного файла с акцентной полосой.
  * Панель действий: создание файлов и папок, обновление дерева и сворачивание всех каталогов.
* **Вкладки редактора**:
  * Нативные цветные иконки расширений (`.dart`, `.yaml`, `.js`, `.py`, `.html`, `.kt`, `.json` и др.).
  * Индикатор несохранённых изменений (dirty dot).
  * Быстрое закрытие вкладок по клику на крестик.

### 📱 4. Обновлённая главная страница проектов
* **Информативные карточки проектов**:
  * Иконка и цветной бейдж технологии (Flutter, Web, Node.js, Python, Android, Прочие).
  * Отображение текущей Git-ветки проекта.
  * Относительное время изменения («Только что», «5 мин назад», «Вчера»).
* **Быстрая фильтрация**: Чипы-фильтры вверху экрана (`[ Все ] [ Flutter ] [ Web ] [ Node.js ] [ Python ] [ Android ]`).
* **Интеграция с GitHub**: Доступ к клонированию своих и публичных репозиториев прямо с главной страницы.

### 🤖 5. Автономные ИИ-агенты кодинга (AI Coding Agents)
* **Google Antigravity CLI (`agy`)**:
  * Вход через официальный Google OAuth Device Flow без ручного ввода токенов.
  * Выбор моделей (Gemini 2.5 Pro / Flash, Claude 3.7 Sonnet, GPT-4o) и настройка глубины рассуждений (*thinking budget*).
  * Автоматический запуск, применение патчей и анализ изменений с возможностью подтверждения (*Diff review*).
* **Anthropic Claude Code**:
  * Официальный агент от Anthropic для архитектурного анализа, рефакторинга и генерации тестов.
* **DeepSeek Harness (DSH)**:
  * Агент DeepSeek с поддержкой официального API и OpenAI-совместимых провайдеров.
* **Чат и Терминал**: Общение с ИИ-агентом прямо в проекте с переходом в консоль.

### 🐧 6. Подсистема Ubuntu 20.04 LTS и менеджер APT
* Полноценная гостевая ОС **Ubuntu 20.04 LTS** на базе PRoot.
* Установка любых пакетов через стандартный `apt` (`python3`, `nodejs`, `npm`, `git`, `curl`, `clang`, `build-essential` и др.).
* Сервис фонового выполнения задач с защитой от выгрузки системой Android.

### 🐙 7. GitHub и клонирование проектов
* **Вход через Device Flow**: Авторизация по коду на `github.com/login/device`.
* **Менеджер репозиториев**: Просмотр списка репозиториев пользователя с поиском и клонированием в один клик.
* **Клонирование по URL**: Скачивание любого публичного Git-репозитория.

### 🇷🇺 8. Полная русская локализация
* 100% переведённый интерфейс (меню, настройки, логи, проводник, подсказки, диалоги создания проектов и карточки агентов).

---

## 📱 Сборка Release APK

Проект автоматически собирается на серверах **GitHub Actions**:
* Любой push в ветку `master` запускает сборку релизного APK (`arm64-v8a`).
* При публикации тега (`vX.Y.Z`) автоматически собираются релизные APK для всех архитектур (`arm64-v8a` и `armeabi-v7a`) и публикуются в [GitHub Releases](https://github.com/Lesorub361/WebIDE/releases).

---

<a name="english-overview"></a>
## 🌐 English Overview

**WebIDE** is a next-generation Android IDE powered by Jetpack Compose.

### Key Highlights:
* **Flutter & Dart Support**: Project templates, automated Flutter SDK installation in Ubuntu PRoot, dev web-server running, `flutter pub get`, and APK build actions.
* **Docked Bottom Panel**: Integrated bottom panel with Linux Terminal, LSP Diagnostics, Flutter Tools, and Build Logs directly beneath the editor.
* **Redesigned File Explorer & Tabs**: VS Code-inspired file tree, action bar, colored file icons, dirty dot indicators, and close buttons.
* **Modernized Home Screen**: Project cards with technology badges, Git branches, relative timestamps, and stack filter chips.
* **Embedded Ubuntu 20.04 LTS PRoot Subsystem**: Real Linux environment with APT package management.
* **AI Coding Agents**: Google Antigravity CLI, Anthropic Claude Code, and DeepSeek Harness with Google OAuth Device Flow.
* **GitHub Integration**: Device Flow OAuth login, repository search, and 1-click clone into workspace.
* **Multilingual UI**: Fully localized in Russian, English, and Chinese.
* **Automated Cloud CI/CD**: Release APKs compiled automatically via GitHub Actions.

---

## 📄 License

Distributed under the terms of the **GNU General Public License v3 (GPLv3)**. See [LICENSE](LICENSE) for details.
