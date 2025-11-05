# MinecraftSurvivors

Ein kompaktes, erweiterbares Minecraft (Paper/Spigot) Minigame im Stil von "Vampire Survivors". Enthält Klassen, kontinuierliche Gegner-Spawns, Level‑Ups mit Upgrades, ein ausgebautes GUI (Party/Stats/Config steuerbar) und einen DPS/HPS‑Meter mit umschaltbaren Anzeigemodi.

---

## Highlights

- Klassen-System mit einzigartigen Fähigkeiten
  - Shaman: Kettenblitz‑Strikes mit Partikelbogen
  - Pyromancer: Flammenwirbel + Feuerstrahl (DoT)
  - Ranger: Fernschuss mit Partikelspur und Knockback
  - Paladin: Heilige Nova (AoE‑Schaden) + Heilung für sich und nahestehende Team‑Spieler (HPS zählt nur eigene Heilung)
- GUI (erweitert, 45 Slots)
  - Hauptmenü: Start, Status, Klassenwahl, Powerups, Party, Stats, Config (Config nur mit Permission)
  - Party: Erstellen, Einladung annehmen, Verlassen/Auflösen, Einladen über klickbare Spieler‑Köpfe (ohne Chat‑Eingabe)
  - Stats: Moduswahl ActionBar/BossBar/Scoreboard/Aus mit Live‑Markierung
  - Config: Reload & visuelle Presets anwenden (z. B. flashy/epic)
- Spawns (Vampire Survivors‑like)
  - Continuous Spawns (statt starrer Waves), sanfter Warmup fürs Early Game
  - Gegner‑Variety/Weights mit minMinute‑Gates (z. B. Husk/Stray/Drowned erst später)
  - Dynamische Caps pro Spielerzahl, feinere Difficulty‑Steps (z. B. alle 30s)
- Komfort & Polishing
  - 5‑Sekunden Start‑Countdown nach „Start Spiel“
  - Deutlich aufgewertete Partikel/Sounds/Animationen (u. a. Paladin Pulse)
- DPS/HPS‑Meter (10s Sliding‑Window)
  - Anzeige‑Modi: ActionBar | BossBar (2 dezente Bars, Auto‑Cap) | Scoreboard | Off
  - Modus per GUI oder Command umschalten; optional periodischer „Top DPS/HPS“‑Broadcast
- Scoreboard (Sidebar)
  - Zeigt Status, Klasse, Lvl/XP, Kills/Coins, Online, Party und Stats‑Modus (Wave entfernt)

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
- Level‑Up Menü: wähle aus 2–3 Optionen (z. B. +Schaden, +Treffer, +Size, +Attackpower, klassen‑spezifische Upgrades)
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

Die Standard‑Config liegt in `plugins/MinecraftSurvivors/config.yml`. Wichtige Auszüge:

- Ability‑Taktung
  - `ability.interval-ticks` (Default 30 ≈ 1.5s)
- Klassen‑Werte
  - `shaman.*`, `pyromancer.*`, `ranger.*`, `paladin.*` (Schaden, Radius, Ziele etc.)
- Level‑Up Upgrades
  - `levelup.values.*` (additive Boni)
  - `levelup.weapon.size-step`, `levelup.weapon.attackpower-step`
  - `levelup.pyromancer.ignite-step`, `levelup.ranger.knockback-step`, `levelup.paladin.heal-step`
- Spawns (Continuous)
  - `spawn.continuous.enabled`: true
  - `spawn.continuous.step-seconds`, `spawn.continuous.growth-per-step`
  - `spawn.continuous.warmup-seconds`, `warmup-mult-start` (sanfter Start)
  - Dynamische Caps: `spawn.continuous.cap.*`
  - Spawn‑Distanzen: `spawn.min-distance`, `spawn.max-distance`
  - Variety/Weights mit minMinute: `spawnMobTypes: [...]`
- Scaling (Gegnerstärke über Zeit): `scaling.*`
- Stats (DPS/HPS)
  - `stats.window-seconds`: Sliding‑Window Länge
  - `stats.mode`: actionbar | bossbar | scoreboard | off
  - `stats.update-interval-ticks`: Updatefrequenz
  - `stats.auto-cap.dps/hps`: Skala für Bossbar‑Progress
  - `stats.broadcast-top.enabled`, `interval-seconds`, `n`: optionaler Broadcast
- Party
  - `party.xp-share.enabled`: true (XP wird unter Online‑Mitgliedern geteilt)
  - `party.xp-share.split-evenly`: true (gleichmäßig teilen)
  - `party.xp-share.min-per-member`: 1 (Mindest‑XP pro Mitglied)
- Scoreboard
  - `scoreboard.update-interval-ticks`: Update‑Tickrate der Sidebar

Hinweis: Der Start‑Countdown ist derzeit fest 5s (konfigurierbar auf Wunsch nachrüstbar).

---

## Architektur (Kurzüberblick)

- `manager/`
  - `GameManager` – Spielzustand, Start/Stop, Countdown, Pausen (per GUI), Ability‑/HUD‑Tasks
  - `SpawnManager` – Continuous Spawns, Glowing, Partikel, Caps, Freeze pro Spieler
  - `AbilityManager` – tickt die Klassen‑Abilities
  - `ScoreboardManager` – Sidebar + HUD (ActionBar) im SCOREBOARD‑Modus
  - `StatsMeterManager` – DPS/HPS Sliding‑Window pro Spieler
  - `StatsDisplayManager` – Anzeige‑Modus (ActionBar/BossBar/Scoreboard/Off)
  - `PartyManager` – Partys, Invites, XP‑Share
- `ability/` – Shaman, Pyromancer, Ranger, Paladin Effekte und Balancing‑Hooks
- `gui/` – Hauptmenü, Klassenwahl, Party/Stats/Config‑Menüs, Level‑Up Menü, Listener
- `listener/` – Entity‑Death (XP‑Vergabe inkl. Party‑Share), Damage/Heal‑Tracking, Freeze‑Logik u. a.
- `model/` – `SurvivorPlayer` inkl. Upgrade‑Werte (radiusMult, damageMult, igniteBonusTicks, healBonus, …)

---

## Entwickeln & Build

- Projekt bauen

```bat
.\gradlew.bat clean build
```

- Nur kompilieren

```bat
.\gradlew.bat compileJava
```

- Deploy (manuell): kopiere `build\libs\*.jar` nach `plugins/` deines Test‑Servers und starte neu.

---

## Permissions

- `minecraftsurvivors.admin` – Admin‑Funktionen (z. B. `/msstart`, `/msconfig reload`, Config‑Menü im GUI, `/msstats`)

---

## Roadmap / Ideen

Kurzfristig
- Klassen‑Auswahl erzwingen (Start erst nach Wahl) – Start‑Button bleibt deaktiviert bis eine Klasse gewählt ist; deutlicher Hinweis im GUI.
- Itemshop (GUI‑Shop, Coins) – Kategorien: Waffen‑Upgrades, Klassen‑Perks, Utility (Heals/Revives/Movement). Öffnung über Hauptmenü; Preise/Pool per config.
- Optionale Pausen zwischen Wellen/Phasen – auch bei Continuous‑Spawns kurze Verschnaufpausen (config): z. B. `spawn.continuous.rest-every-seconds`, `rest-duration-seconds`; währenddessen Spawn‑Rate→0, Aggro resetten, dezente Effekte.
- Projectile‑Schaden sauber dem Schützen zuordnen (für Ranger‑Pfeile) – Pfeile mit Shooter/Ability‑Tags; DPS/HPS klar dem Verursacher gutschreiben.

Mittelfristig
- Persistenz: Klasse/Upgrades zwischen Sessions abspeichern (YAML/JSON via PlayerData; Schema‑Versionierung/Migration berücksichtigen).
- Optionale Bossbar‑Skalen: dynamisch an „Max der letzten X Sekunden“ koppeln (Auto‑Zoom), per Config und GUI umschaltbar.

Langfristig
- Mehr Klassen/Perks & Synergien, Events/Bosse, Map‑Affixe, Meta‑Progression.

---

## Lizenz

Füge hier bei Bedarf eine Lizenz hinzu (z. B. MIT), wenn das Projekt öffentlich freigegeben wird.
