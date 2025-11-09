# Installation

Systemanforderungen

- Java 21 (OpenJDK / Temurin empfohlen)
- Paper 1.21.4
- Mindestens 2–4 GB RAM für Server + Plugin

Build & Installation

1. Projekt bauen

```bat
cd C:\Users\SashaW\IdeaProjects\MinecraftSurvivors
.\gradlew.bat clean build
```

2. Kopiere das Plugin

```bat
copy build\libs\MinecraftSurvivors-*.jar <YOUR_PAPER_SERVER>\plugins\
```

3. Starte deinen Paper‑Server und überprüfe `logs/latest.log` auf Fehler

Konfigurationsdatei: `plugins/MinecraftSurvivors/config.yml`

- Einstellungen werden beim ersten Start generiert. Passe Werte wie `spawn.*`, `scaling.*` und `party.*` nach Bedarf an.
