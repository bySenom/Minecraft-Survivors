# Minecraft Survivors

Minecraft Survivors bringt das Gameplay von "Vampire Survivors" in Minecraft (Paper/Spigot): kontinuierliche Gegner‑Spawns, Klassen mit einzigartigen Fähigkeiten, Glyphen, Lootchests, Party‑System, Shop und einen DPS/HPS‑Meter.

Live‑Demo (GitHub Pages): /docs/index.html

Badges

[![Build](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/build.yml/badge.svg)](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/build.yml)
[![Release](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/release.yml/badge.svg)](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/release.yml)

---

## Kurzfassung

- Klassen: Shaman, Pyromancer, Ranger, Paladin (jeweils eigene Fähigkeiten)
- Continuous Spawns mit Scaling & Elite‑Gegnern
- Level‑Ups mit Raritäten (Common / Rare / Epic) und sockelbaren Glyphen
- Party‑Flow mit Ready‑Vote für Multiplayer
- Shop / Daily Offers / Meta‑Progression
- DPS/HPS Meter (ActionBar | BossBar | Scoreboard)

---

## Schnellstart (Windows / Paper)

1. Projekt bauen

    ```bat
    cd C:\Users\SashaW\IdeaProjects\MinecraftSurvivors
    .\gradlew.bat clean build
    ```

2. JAR kopieren

    ```bat
    copy build\libs\*.jar <YOUR_PAPER_SERVER>\plugins\
    ```

3. Server starten (Paper 1.21.4 empfohlen) und als OP verbinden

4. In‑Game: `/msmenu` → Klasse wählen → Spiel starten

---

## Admin / Dev

- Config neu laden: `/msconfig reload` (oder über GUI)
- Tests / Build (lokal): `./gradlew spotlessApply clean build`
- Release: Tag push (`git tag vX.Y.Z && git push origin vX.Y.Z`) — CI erstellt das Release

---

## Konfiguration (Auszug)

Wichtige config‑Keys (siehe `plugins/MinecraftSurvivors/config.yml`):
- `spawn.continuous.*` — Continuous spawn settings
- `spawn.elite.*` — Elite chance/scale/health
- `scaling.*` — Gegner‑Scaling pro Minute
- `levelup.*` — Upgrade‑Values & Rarities
- `party.*` — Party / ready flow
- `stats.*` — DPS/HPS meter settings
- `data.autosave-interval-seconds` — autosave interval (default 120)

---

## LobbySystem Quickstart

Das optionale `LobbySystem` Plugin steuert Warteschlange & gestaffelten Eintritt in Minecraft Survivors.

1. JAR in denselben Server laden (Plugins: MinecraftSurvivors + LobbySystem).
2. Wichtige Config-Keys (`plugins/LobbySystem/config.yml`):
   - `admission.enabled` (true/false) – aktiviert Auto-Zulassung.
   - `admission.interval-seconds` – Sekunden zwischen Zulassungen.
   - `admission.max-admitted` – maximale parallele zugelassene Spieler (0 = unbegrenzt).
   - `admission.timeout-seconds` – Zeit bis Admission verfällt, wenn Spieler nicht startet.
   - `admission.timeout-action` – `remove` oder `recycle` (zurück ans Ende).
   - `queue.rejoin-cooldown-seconds` – Anti-Spam Cooldown für /queue join.
   - `queue.persist-enabled` – Queue beim Server-Neustart erhalten.
   - `queue.debug` – ausführliche Logausgaben.
3. Spieler nutzen `/queue join` → Auto-Zulassung in Wellen. Status mit `/queue status`.
4. Admins: `/queue stats`, `/queue next`.
5. Auto-Start Survivors nach Mindestanzahl via `survivors.auto-dispatch-enabled=true` in LobbySystem Config.

---

## Architektur (Kurz)

Wichtige Pakete:
- `manager/` — GameManager, SpawnManager, AbilityManager, PartyManager, etc.
- `gui/` — Menüs (Hauptmenü, LevelUp, Shop, Party)
- `listener/` — Event‑Hooks (Damage, Death, Lootchest, GlyphPickup)
- `model/` — SurvivorPlayer, Abilities, Glyphs

---

## Lizenz

MIT © 2025 bySenom

(Full license text retained in the repository.)
