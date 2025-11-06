# MinecraftSurvivors

[![Build](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/build.yml/badge.svg)](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/build.yml)
[![Release](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/release.yml/badge.svg)](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/release.yml)
[![CodeQL](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/codeql.yml/badge.svg)](https://github.com/bySenom/Minecraft-Survivors/actions/workflows/codeql.yml)

Ein kompaktes, erweiterbares Minecraft (Paper/Spigot) Minigame im Stil von "Vampire Survivors". Enthält Klassen, kontinuierliche Gegner-Spawns, Level‑Ups mit Upgrades, ein ausgebautes GUI (Party/Stats/Config steuerbar) und einen DPS/HPS‑Meter mit umschaltbaren Anzeigemodi.

---

## Highlights

- Klassen-System mit einzigartigen Fähigkeiten
    - Shaman: Kettenblitz‑Strikes mit Partikelbogen
    - Pyromancer: Flammenwirbel + Feuerstrahl (DoT)
    - Ranger: Fernschuss mit Partikelspur und Knockback
    - Paladin: Heilige Nova (AoE‑Schaden) + Heilung für sich und nahestehende Team‑Spieler (HPS zählt nur eigene Heilung); Pulse folgt dem Spieler, sehr kurze Implosions-Endwelle (optional)
- GUI (erweitert, 45 Slots)
    - Hauptmenü: Start, Status, Klassenwahl, Powerups, Party, Stats, Config (Config nur mit Permission)
    - Party: Erstellen, Einladung annehmen, Verlassen/Auflösen, Einladen über klickbare Spieler‑Köpfe (ohne Chat‑Eingabe)
    - Stats: Moduswahl ActionBar/BossBar/Scoreboard/Aus mit Live‑Markierung; „Top DPS/HPS“ via Command
    - Config: Reload & visuelle Presets anwenden (z. B. flashy/epic)
- Powerups & Raritäten
    - Level‑Up‑Optionen mit Raritäten (Common/Rare/Epic) und Multiplikatoren; neue Stats wie Bewegung, Angriffstempo, Resistenz, Glück
    - Health‑Upgrade vergibt echte Herzen (Max‑Health + aktuelles Health sinnvoll angehoben)
- Spawns (Vampire Survivors‑like)
    - Continuous Spawns (statt starrer Waves), sanfter Warmup fürs Early Game
    - Gegner‑Variety/Weights mit minMinute‑Gates (z. B. Husk/Stray/Drowned erst später)
    - Dynamische Caps pro Spielerzahl, feinere Difficulty‑Steps (z. B. alle 30s)
    - Elite‑Gegner: größer, seltener, skalierende HP (über Zeit)
- Komfort & Polishing
    - 5‑Sekunden Start‑Countdown nach „Start Spiel“
    - Deutlich aufgewertete Partikel/Sounds/Animationen (u. a. Paladin Pulse)
- DPS/HPS‑Meter (10s Sliding‑Window)
    - Anzeige‑Modi: ActionBar | BossBar (2 dezente Bars, Auto‑Cap) | Scoreboard | Off
    - Modus per GUI oder Command umschalten; optional periodischer „Top DPS/HPS“‑Broadcast
- Scoreboard (Sidebar)
    - Zeigt Status, Klasse, Lvl/XP, Kills/Coins, Online, Party und Stats‑Modus (Wave entfernt)

---

## Neu/aktuell

### Powerups & Raritäten
- Raritäten: Common (1.0×), Rare (1.2×), Epic (1.5×). Gewichte in der Config.
- Beispiele (konfigurierbare Schritte):
  - +Schaden (additiv), +FlatDmg, +Treffer (mehr Ziele)
  - +Size (Radius‑Multiplikator), +Attackpower (Schaden‑Multiplikator)
  - +Bewegung (Movespeed), +Angriffstempo, +Resistenz (Schadensreduktion), +Glück (für zukünftige Pools/Loot)
  - Klassen‑spezifisch: z. B. Pyromancer +Burn Dauer, Paladin +Heilung
- Health‑Upgrade: vergibt echte Herzen (erhöht GENERIC_MAX_HEALTH und füllt etwas auf)

### AttackSpeed‑Skalierung der Klassen
- AttackSpeed wirkt jetzt auf die Fähigkeiten:
  - Shaman: mehr Strikes pro Tick (bis ~3× gecappt)
  - Pyromancer: mehr Ziele pro Tick (bis ~2.5×)
  - Ranger: mehr Pfeile (Multi) und leicht mehr Piercing (Caps: Multi ~3×, Pierce ≤8)
  - Paladin: Damage/Heal‑Loop wird häufiger pro Tick ausgeführt (bis ~2.5×), Pulse‑Visual nur 1× zur Performance

### Shop (Daily Offers & Limits)
- Shop‑GUI im Hauptmenü; zeigt tägliche Angebote (Daily‑Refresh) mit Reset‑Timer im Hauptmenü‑Footer
- Limits: Max Käufe pro Run/Tag sowie pro Itemkategorie (konfigurierbar)
- Kategorien (Beispiel): Weapons (DamageMult, RadiusMult), Class‑spezifische Perks (z. B. Paladin Heal)

### Elite‑Gegner
- Seltene Elite‑Spawns, größer (size‑scale), höhere HP
- HP‑Multiplikator skaliert zusätzlich pro Minute (je länger das Spiel, desto tankiger)

### Slotmaschine/Lootchest
- Lootchests öffnen eine Slotmaschinen‑GUI mit realen Reels/Symbolgewichten; Powerups/Belohnungen sind zufällig

---

## Version & Release

- Version anzeigen im Spiel:
  - Befehl: `/msversion`
- Versionierung im Build:
  - Bei CI-Builds aus Tags wie `vX.Y.Z` wird automatisch die Projektversion auf `X.Y.Z` gesetzt.
- Release erstellen (GitHub Actions):
  - Tag setzen und pushen:
    - `git tag v1.0.0`
    - `git push origin v1.0.0`
  - Der Workflow erstellt einen Release und hängt das JAR an.
- Auto-Changelog (Release Drafter):
  - Auf `main` gepflegte PRs/Commits werden zu einem Entwurfschangelog zusammengeführt.

---

## Autosave (Spieler-Daten)

- Spieler-Daten werden periodisch asynchron gespeichert.
- Konfiguration in `config.yml`:
  - `data.autosave-interval-seconds`: Standard `120`. `0` deaktiviert Autosave.
- Beim Server-Stop werden alle Daten final gespeichert.

---

## CI/CD & Qualität

- Build (Windows + Ubuntu):
  - Linter/Format: `spotlessCheck`
  - Tests + Build: `clean test build`
  - Artefakte: Plugin-JAR und Testberichte werden hochgeladen
- Release: siehe Abschnitt „Version & Release“ (inkl. Auto-Release-Notes und Testreports)

---

## Installation (Windows / Paper)

1) Build

```bat
cd C:\Users\SashaW\IdeaProjects\MinecraftSurvivors
.\gradlew.bat clean build
```

2) Kopiere das JAR aus `build\libs\` nach `<dein-paper-server>\plugins\`

3) Server starten und als OP verbinden

---

## Quickstart In‑Game

- `/msmenu` öffnen → Klasse wählen → „Start Spiel“ (5s Countdown)
- Gegner spawnen kontinuierlich; Level‑Ups erscheinen automatisch
- Level‑Up Menü: wähle aus 2–3 Optionen – inkl. Raritäten (Common/Rare/Epic) mit farbiger Anzeige
- Paladin heilt im Nova‑Radius auch Team‑Mitspieler (HPS zählt nur beim Heiler)

Wichtige Wege/Commands:
- GUI
    - Party-Verwaltung komplett im Menü (Party → Einladen (Liste) → Kopf anklicken)
    - Stats-Modus im Menü „Stats“ umschalten
    - Config (nur Admin): Reload & Presets
- Commands (ergänzend)
    - `/msmenu` – Hauptmenü öffnen
    - `/msstart` – Spiel starten (Admin)
    - `/msconfig reload` – Config neu laden
    - `/msstats mode <actionbar|bossbar|scoreboard|off>` | `toggle` | `show` | `top [dps|hps] [n]`
    - `/party create|invite|join|leave|disband|list` (Invite auch bequem per GUI möglich)

---

## Konfiguration

Die Standard‑Config liegt in `plugins/MinecraftSurvivors/config.yml`. Wichtige Auszüge/neu:

- Ability‑Taktung
  - `ability.interval-ticks` (Default 30 ≈ 1.5s)
- Klassen‑Werte
  - `shaman.*`, `pyromancer.*`, `ranger.*`, `paladin.*`
  - Paladin: `paladin.implosion-enabled` (kurze End‑Implosion um den Spieler)
- Level‑Up Upgrades
  - `levelup.values.*`: `bonus-damage`, `flat-damage`, `bonus-strikes`, `extra-hearts`, `move-speed`, `attack-speed`, `resist`, `luck`
  - `levelup.weapon.size-step`, `levelup.weapon.attackpower-step`
  - `levelup.pyromancer.ignite-step`, `levelup.ranger.knockback-step`, `levelup.paladin.heal-step`
  - Rarität: `levelup.rarity.common|rare|epic` (Gewichte in %)
- Spawns (Continuous)
  - `spawn.continuous.enabled`
  - Steps/Scaling: `spawn.continuous.step-seconds`, `growth-per-step` (oder `growth-per-minute`)
  - Warmup: `spawn.continuous.warmup-seconds`, `warmup-mult-start`
  - Caps: `spawn.continuous.cap.*` (dynamic)
  - Distanzen: `spawn.min-distance`, `spawn.max-distance`
  - Variety: `spawnMobTypes: [{type, weight, minMinute}, …]`
- Scaling (Gegnerstärke)
  - `scaling.health-mult-per-minute`
  - Mid/Late‑Gates: `scaling.health-mid-minute`, `scaling.health-late-minute`
  - Multiplikatoren: `scaling.health-mid-multiplier`, `scaling.health-late-multiplier`
  - Damage: `scaling.damage-add-per-minute`, `scaling.damage-mid-multiplier`, `scaling.damage-late-multiplier`
- Elite‑Gegner
  - `spawn.elite.chance-percentage`, `spawn.elite.size-scale`
  - `spawn.elite.base-health-mult`, `spawn.elite.extra-health-mult-per-minute`
- Stats (DPS/HPS)
  - `stats.window-seconds`, `stats.mode`, `stats.update-interval-ticks`
  - Auto‑Caps: `stats.auto-cap.dps`, `stats.auto-cap.hps`
  - Dynamic‑Max (optional): `stats.dynamic-cap-enabled`, `stats.dynamic-cap-window-seconds`, `stats.dynamic-cap-smoothing`
- Party
  - `party.xp-share.enabled`, `split-evenly`, `min-per-member`
- Shop
  - `shop.enabled`
  - Lauf/Tageslimits: `shop.limits.max-per-run|max-per-day`
  - Pro‑Item‑Limits: `shop.item-limits.weapons-per-run|weapons-per-day` (analog für class)
  - Daily: `shop.daily.max-weapons`
  - Kategorien/Items: `shop.categories.*`

---

## Architektur (Kurzüberblick)

- `manager/`
  - `GameManager` – Spielzustand, Start/Stop, Countdown, Pausen (per GUI), Ability‑/HUD‑Tasks
  - `SpawnManager` – Continuous Spawns, Glowing, Partikel, Caps, Freeze pro Spieler, Elite‑/Scaling
  - `AbilityManager` – tickt die Klassen‑Abilities (AttackSpeed‑Skalierung integriert)
  - `ScoreboardManager` – Sidebar + HUD (ActionBar) im SCOREBOARD‑Modus
  - `StatsMeterManager` – DPS/HPS Sliding‑Window pro Spieler
  - `StatsDisplayManager` – Anzeige‑Modus (ActionBar/BossBar/Scoreboard/Off)
  - `PartyManager` – Partys, Invites, XP‑Share
  - `ShopManager` – Daily Offers, Limits, Kaufabwicklung
- `ability/` – Shaman, Pyromancer, Ranger, Paladin
- `gui/` – Hauptmenü, Klassenwahl, Party/Stats/Config, Level‑Up, Shop
- `listener/` – Entity‑Death (XP‑Vergabe inkl. Party‑Share), Damage/Heal‑Tracking, Lootchest (Slotmaschine), Freeze
- `model/` – `SurvivorPlayer` inkl. Upgrades und Persistenz

---

## Entwickeln & Build

- Projekt bauen

```bat
.\gradlew.bat spotlessApply
.\gradlew.bat clean build -x test
```

- Dev-Server starten (Paper 1.21.4)

```bat
.\gradlew.bat runServer
```

- Deploy (manuell)

```bat
copy build\libs\*.jar <DEIN_PAPER_SERVER>\plugins\
```

---

## Permissions

- `minecraftsurvivors.admin` – Admin‑Funktionen (z. B. `/msstart`, `/msconfig reload`, Config‑Menü im GUI, `/msstats`)

---

## Roadmap / Ideen

Kurzfristig
- Klassen‑Auswahl erzwingen (Start erst nach Wahl) – Start‑Button bleibt deaktiviert bis eine Klasse gewählt ist (UI‑Hint im Menü)
- Weitere Klassen‑Upgrades/Evolutionspfade mit Lore (+Size, Attackpower, Speed)
- DamageResist aktiv im Combat‑Hook verrechnen (optional per Config)

Mittelfristig
- Luck‑Einfluss auf Lootchest/Shop‑Rotationspools; personalisierte Daily‑Listen
- Bossbar‑Skalen dynamisch an Max der letzten X Sekunden koppeln (Sticky Maxima/Moving Max)

Langfristig
- Mehr Klassen/Perks & Synergien, Events/Bosse, Map‑Affixe, Meta‑Progression

---

## Lizenz

MIT License

Copyright (c) 2025 bySenom

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
