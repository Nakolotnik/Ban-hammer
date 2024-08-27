# Ban Mace

## English
## Description

**BanMace** is a Minecraft plugin that introduces a unique weapon called **Ban Mace**. This item provides players with various functions for managing other players, including teleportation, temporary bans, kicks, and freezing.

## Features

- **Teleportation**:
  - **SPAWN**: Teleports the player to the world's spawn point.
  - **BED**: Teleports the player to their bed or to spawn if no bed is set.

   - **Temporary Ban**: Temporarily bans the player and adds them to the ban list.

   - **Kick**: Kicks the player from the server.

   - **Freeze**: Freezes the player, blocking their movement, and removes the freeze after a set time.

## Commands

- **/banmace**:
  - **Description**: Obtain the **Ban Mace** item.
  - **Permissions**: `banmace.give`

## Configuration

The plugin uses configuration files to adjust behavior:

- `banmace.mode` — The mode of the Ban Mace (SPAWN, BED, BAN, KICK, FREEZE).
- `banmace.toggle_key` — The key to switch the Ban Mace mode (e.g., RIGHT_CLICK_AIR).
- `banmace_messages` — Messages sent to players in various situations:
  - `mace_received` — Message when receiving the Ban Mace.
  - `no_permission` — Message when lacking permission.
  - `ban` — Message for banning.
  - `kick` — Message for kicking.
  - `freeze` — Message for freezing.
- `teleport_effect` — Effects during teleportation:
  - `particle` — Particles (e.g., PORTAL).
  - `count` — Number of particles.
  - `offset` — Offset of particles.
  - `speed` — Speed of particles.
- `teleport_sound` — Sound during teleportation.

## Permissions

- `banmace.use` — Permission to use the **Ban Mace**.
- `banmace.give` — Permission to receive the **Ban Mace**.

## Installation

1. Download the **BanMace** plugin.
2. Place it in the `plugins` folder of your Minecraft server.
3. Restart the server or run the `/reload` command.

## Notes

- Ensure the main world is available on the server for teleportation functions to work correctly.
- The plugin supports major versions of Minecraft. Compatibility with other versions may require additional testing.

## Contact

If you have any questions or suggestions, you can contact me via [github issues](https://github.com/Nakolotnik/Ban-hammer/issues).

---

**BanMace** is a powerful tool for Minecraft administrators, allowing effective management of players and maintaining order on the server.

________________________________________________________________


## Russian | Русский
## Описание

**BanMace** — это плагин для Minecraft, который добавляет в игру уникальное оружие под названием **Ban Mace** (Булова бана). Этот предмет предоставляет игрокам различные функции для управления другими игроками, включая телепортацию, временные баны, кики и заморозку.

## Функции

- **Телепортация**:
  - **SPAWN**: Телепортирует игрока на точку спавна мира.
  - **BED**: Телепортирует игрока к его кровати или на спавн, если кровать не установлена.
  - **Временный бан**: Временно банит игрока и добавляет его в список забаненных.
  - **Кик**: Удаляет игрока с сервера.

- **Заморозка**: Замораживает игрока, блокируя его движение, и отменяет заморозку через заданное время.

## Команды

- **/banmace**:
  - **Описание**: Получите предмет **Ban Mace**.
  - **Разрешения**: `banmace.give`

## Конфигурация

Плагин использует конфигурационные файлы для настройки поведения:

- `banmace.mode` — Режим действия буловы бана (SPAWN, BED, BAN, KICK, FREEZE).
- `banmace.toggle_key` — Ключ для переключения режима действия буловы бана (например, RIGHT_CLICK_BLOCK).
- `banmace_messages` — Сообщения, отправляемые игрокам в различных ситуациях:
  - `mace_received` — Сообщение при получении буловы бана.
  - `no_permission` — Сообщение при отсутствии прав на использование.
  - `ban` — Сообщение о бане.
  - `kick` — Сообщение о кике.
  - `freeze` — Сообщение о заморозке.
- `teleport_effect` — Эффекты при телепортации:
  - `particle` — Частицы (например, PORTAL).
  - `count` — Количество частиц.
  - `offset` — Смещение частиц.
  - `speed` — Скорость частиц.
- `teleport_sound` — Звук при телепортации.

## Разрешения

- `banmace.use` — Разрешение на использование **Ban Mace**.
- `banmace.give` — Разрешение на получение **Ban Mace**.

## Установка

1. Скачайте плагин **BanMace**.
2. Поместите его в папку `plugins` вашего сервера Minecraft.
3. Перезагрузите сервер.

## Примечания

- Убедитесь, что основной мир доступен на сервере для корректной работы функций телепортации.
- Плагин поддерживает основные версии Minecraft. Совместимость с другими версиями может потребовать дополнительной проверки.

## Контакты

Если у вас есть вопросы или предложения, вы можете связаться со мной через  [github issues](https://github.com/Nakolotnik/Ban-hammer/issues).
---

**BanMace** — это мощный инструмент для администраторов Minecraft, позволяющий эффективно управлять игроками и поддерживать порядок на сервере.
