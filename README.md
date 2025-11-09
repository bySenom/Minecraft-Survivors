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
