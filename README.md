# WebIDE — Мобильная IDE с ИИ-агентами и Ubuntu 20.04

![Version](https://img.shields.io/badge/version-0.4.0-blue?style=flat-square)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue?style=flat-square)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-green?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Subsystem](https://img.shields.io/badge/Subsystem-Ubuntu_20.04_PRoot-E95420?style=flat-square)](https://ubuntu.com/)
[![Agents](https://img.shields.io/badge/AI_Agents-Antigravity_|_Claude_|_DeepSeek-4285F4?style=flat-square)](https://github.com/Lesorub361/WebIDE)
[![License](https://img.shields.io/badge/License-GPLv3-orange?style=flat-square)](LICENSE)

[ [**Русский**] ] | [ [English](#english-overview) ]

**WebIDE** — это нативная Android-среда разработки (IDE) нового поколения, объединяющая возможности полноценного мобильного редактора кода, изолированную Linux-подсистему **Ubuntu 20.04 LTS (PRoot)** и автономных **ИИ-агентов для кодинга** (Antigravity CLI, Claude Code, DeepSeek Harness) с глубокой интеграцией **GitHub**.

---

## 🚀 Что нового в этой версии (Новые возможности)

### 🤖 1. Автономные ИИ-агенты кодинга (AI Coding Agents)
* **Google Antigravity CLI (`agy`)**:
  * Вход через официальный Google OAuth Device Flow без необходимости ввода сырых токенов.
  * Выбор моделей (Gemini Pro, Flash, Claude, GPT-4o и др.) и настройка глубины рассуждений (*effort level*).
  * Автоматический запуск, применение патчей и анализ изменений с возможностью подтверждения (*Diff review*).
* **Claude Code**:
  * Официальный агент от Anthropic для анализа проектов, рефакторинга и генерации кода.
  * Поддержка токенов подписки и API-ключей.
* **DeepSeek Harness (DSH)**:
  * Агент DeepSeek с поддержкой официального API и совместимых OpenAI/OpenRouter провайдеров.
* **Чат и Терминал агента**:
  * Интерактивный чат с агентом прямо в контексте открытого проекта.
  * Быстрый переход в CLI-терминал с рабочей директорией проекта.

### 🐧 2. Подсистема Ubuntu 20.04 LTS и менеджер пакетов APT
* Полноценная гостевая ОС **Ubuntu 20.04 LTS** на базе PRoot в изолированном пространстве приложений.
* Установка и обновление пакетов через стандартный `apt` (`python3`, `nodejs`, `npm`, `git`, `curl`, `build-essential`, `clang` и др.).
* Сервис фонового выполнения задач (`RuntimeExecutionService`) с уведомлениями и защитой от выгрузки системой Android.

### 🐙 3. Интеграция с GitHub и клонирование проектов
* **Вход через GitHub Device Flow**:
  * Быстрая авторизация по 8-значному коду на `github.com/login/device`.
* **Менеджер репозиториев**:
  * Мгновенный просмотр списка ваших публичных и приватных репозиториев с поиском и фильтрацией.
  * Клонирование любого проекта в один клик прямо в рабочую область IDE (`/sdcard` или `/Android/data`).
  * Клонирование любых сторонних публичных Git-репозиториев по URL.
* Доступ к GitHub прямо из списков проектов, окна создания и шапки ИИ-агента.

### 🇷🇺 4. Полная русская локализация
* 100% переведённый интерфейс (меню, настройки, логи, подсказки, диалоги создания проектов и карточки агентов).
* Переключатель языка приложения (Русский, Английский, Китайский) в настройках.

### 📁 5. Расширенные шаблоны проектов и современный UI
* Плавные анимации Jetpack Compose и карточный дизайн Bento UI.
* Поддержка шаблонов:
  * **Web**: HTML5, CSS3, JavaScript.
  * **WebApp**: сборка веб-сайта в нативный Android APK с помощью встроенного компилятора.
  * **Website Wrapper**: создание Android-приложения с оболочкой внешнего URL.
  * **Node.js**: проекты на Express, Fastify и npm.
  * **Python**: скрипты и веб-сервисы на Python 3.
  * **Android (Compose)**: шаблоны Android-приложений.

---

## 📱 Сборка Release APK

Проект автоматически собирается на серверах **GitHub Actions**:
* Любой push в `master` автоматически собирает релизный APK для архитектуры `arm64-v8a`.
* При установке тега (`vX.Y.Z`) автоматически собираются релизные APK для всех архитектур (`arm64-v8a` и `armeabi-v7a`) и прикрепляются к [GitHub Releases](https://github.com/Lesorub361/WebIDE/releases).

---

<a name="english-overview"></a>
## 🌐 English Overview

**WebIDE** is a next-generation Android IDE powered by Jetpack Compose. It includes:
* **Embedded Ubuntu 20.04 LTS PRoot Subsystem**: Real Linux environment with APT package management.
* **AI Coding Agents**: Official support for Google Antigravity CLI, Anthropic Claude Code, and DeepSeek Harness.
* **GitHub Integration**: Device Flow OAuth login, repository search, and 1-click clone into workspace.
* **Multilingual UI**: Fully localized in Russian, English, and Chinese.
* **Project Templates**: Web, WebApp (HTML to APK), Node.js, Python, and Android Compose.
* **Automated Cloud CI/CD**: Release APKs compiled automatically via GitHub Actions.

---

## 📄 License

Distributed under the terms of the **GNU General Public License v3 (GPLv3)**. See [LICENSE](LICENSE) for details.
