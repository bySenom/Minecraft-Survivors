# MinecraftSurvivors

Eine minimalistische Grundlage für ein "Vampire Survivors"‑inspiriertes Minecraft‑Plugin (Paper/Spigot). Dieses Repository enthält das Plugin‑Projekt, die Standard‑Config und eine einfache GitHub Pages Landingpage unter `docs/`.

Kurz: ein strukturiertes, erweiterbares Fundament — Wellen, Spawns, Klassen, GUI und ein Ability‑System sind bereits implementiert.

---

## Übersicht

- Sprache: Java (für Bukkit / Paper API)
- Build: Gradle
- Getestet mit: Paper 1.21.10 (siehe `build.gradle`)
- Ziel: Minigame „MinecraftSurvivors" — Waves vs. Player, Klassen & Powerups

---

## Features (kurz)

- Wave‑Spawning mit Markierung (PersistentDataContainer)
- GUI (Hauptmenü, Klassenwahl, Info) mit Adventure‑Components
- Ability‑System (Beispiel: Shamanen‑Blitz)
- Konfigurierbare Partikel, Presets & GitHub Pages‑Workflow
- Commands:
  - `/msmenu` – öffnet das Hauptmenü
  - `/msstart` – startet das Spiel (Admin/Op)
  - `/msconfig` – config management (reload, preset list/show/apply)

---

## Installation / Test (lokal)

1. Build das Plugin:

```bat
cd C:\Users\SashaW\IdeaProjects\MinecraftSurvivors
.\gradlew.bat clean build
```

2. Kopiere das erzeugte JAR aus `build/libs/` in dein Paper/Purpur Server `plugins/` Ordner.
3. Starte den Server neu (`/stop` + Start) — benutze kein `/reload` zum Neuerstellen der Remapped JARs.
4. Verbinde dich mit dem Server und teste die Commands (als OP):

```text
/msmenu
/msstart
/msconfig preset list
/msconfig preset epic
```

---

## Konfiguration

Standard‑Config: `plugins/MinecraftSurvivors/config.yml` (wird per `saveDefaultConfig()` beim ersten Start erstellt). Wichtige Bereiche:

- `ability.interval-ticks` — Interval für Ability‑Ticks (20 ticks = 1s)
- `shaman.*` — Shaman‑spezifische Werte (Schaden, Radius, Strikes)
- `spawn.*` — Spawn‑Partikel, glow, Presets
- `presets` — vordefinierte Partikel‑Presets (subtle/flashy/epic)

Ändere die Config, dann führe als OP aus:

```text
/msconfig reload
```

Oder wende ein Preset an (wird automatisch reloaden):

```text
/msconfig preset flashy
```

---

## Entwicklung

Projektstruktur (Wesentliches):

```
src/main/java/...            Java Quellcode (Managers, GUI, Abilities, Listeners)
src/main/resources           config.yml, plugin.yml
docs/                       GitHub Pages Inhalt (index.html)
```

Nützliche Befehle:

- Build: `./gradlew.bat clean build`
- Compile only: `./gradlew.bat compileJava`

Wenn du am Code arbeitest, führe Unit‑/Manual‑Tests lokal auf einem Test‑Server aus.

---

## GitHub Pages

Die Page wird vom `docs/`‑Ordner deployed. Ein GitHub Actions Workflow (`.github/workflows/gh-pages.yml`) pusht `docs/` nach Branch `gh-pages` bei Push auf `main`.

Seite (nach erfolgreichem Deploy):

```
https://bySenom.github.io/Minecraft-Survivors/
```

---

## Mitwirken

Wenn du mitmachen möchtest:

1. Forke das Repo oder erstelle ein Issue mit einer Idee.
2. Erstelle einen Branch `feature/<kurzbeschreibung>`.
3. Committe klein, dokumentiere das Verhalten und öffne einen Pull Request.

Coding‑Konventionen: Java 17/21 Kompatibilität, Gradle, Adventure API für Texte.

---

## Lizenz

Füge hier eine Lizenz ein (z. B. MIT), wenn du das Projekt öffentlich freigeben möchtest.

---

Wenn du willst, kann ich jetzt die Datei für dich committen und pushen (ich kann es nicht ohne deine Git‑Credentials), oder ich zeige dir gern die exakten Befehle, die du in `cmd.exe` ausführen musst. Welche Option möchtest du? ("Commit & push Schritte" oder "Mach du"?)

