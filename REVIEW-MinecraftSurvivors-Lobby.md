Code Review: MinecraftSurvivors — Survivors & Lobby

Datum: 2025-11-09

Kurzfassung
- Build: Gradle-Build lief erfolgreich ("BUILD SUCCESSFUL").
- Fokus: Lobby-/Survivors-bezogene Klassen geprüft (GameManager, PlayerManager, PlayerDataManager, ShopNpcManager, StatsDisplayManager, MinecraftSurvivors).
- Ergebnis: Keine Kompilationsfehler. Einige kleinere Logik-/Robustheits-Hinweise (weiter unten). Keine Änderungen am laufenden Code vorgenommen, nur diese Review-Datei hinzugefügt.

Geöffnete Dateien
- `src/main/java/org/bysenom/minecraftSurvivors/MinecraftSurvivors.java`
- `src/main/java/org/bysenom/minecraftSurvivors/manager/GameManager.java`
- `src/main/java/org/bysenom/minecraftSurvivors/manager/PlayerManager.java`
- `src/main/java/org/bysenom/minecraftSurvivors/manager/ShopNpcManager.java`
- `src/main/java/org/bysenom/minecraftSurvivors/util/PlayerDataManager.java`
- `src/main/java/org/bysenom/minecraftSurvivors/manager/StatsDisplayManager.java`

Wichtige Beobachtungen und Empfehlungen

1) Build und Tests
- Gradle-Build erfolgreich (keine Compiler- oder Testfehler). Gute Grundlage.

2) Threading / Scheduler
- `PlayerDataManager.saveAsync` nutzt `Bukkit.getScheduler().runTaskAsynchronously` — korrekt. Falls beim speicherintensiven I/O weitere Last entsteht, kann ein dedizierter Executor-Service in Betracht gezogen werden.

3) Spielzustand / Pausen-Logik
- `GameManager` trennt globale GUI-Pause (`pauseForGui` / `resumeFromGui`) und per-player Pause (`pauseForPlayer` / `resumeForPlayer`) sauber, was gewünscht ist.
- Hinweis: `pauseForGui` erhöht `pauseCounter` und setzt `state = PAUSED` nur, wenn vorher RUNNING, was sinnvoll ist; aber wenn `pauseForGui` mehrfach aufgerufen wird während `state != RUNNING`, wird nur `pauseCounter` erhöht. Die damit verbundene Logik ist konsistent, aber die Kommentierung sollte klarstellen, dass `pauseCounter` unabhängig vom aktuellen state gezählt wird.

4) Schutz- / Timeout-Tasks
- `protectPlayer` und `pauseForPlayer` verwalten per-player Tasks in Maps; alte Tasks werden entfernt und gecancelt, was korrekt ist.
- Mögliche Race-Condition: `protectedUntil` prüft wall-clock via `System.currentTimeMillis()`. Bei server tick-abhängigen Timern wäre auch eine tick-basierte Lösung möglich, aktuell ist die Mischung aus Realzeit und Scheduler-Ticks jedoch bereits sinnvoll und funktioniert in der Praxis.

5) Konfigurations-Reload
- `GameManager.reloadConfigAndApply` reloadet `ConfigUtil` und restarts `AbilityManager` bei `RUNNING` - gute Idee; evtl. sollten weitere Manager berücksichtigt werden (z.B. SpawnManager / ShopNpcManager) je nach welchen Config-Keys geändert werden.

6) PlayerDataManager
- `save` schreibt erwartete Felder; `load` erlaubt optionales Laden der Klasse (Konfig-key `profile.load-class`), wodurch erzwungene Klasse vermieden wird — sinnvolle Sicherheitsmaßnahme.
- `saveCoins` speichert nur `coins` (überschreibt keine anderen Felder) — das ist dokumentiert und plausibel.

7) ShopNpcManager
- Spawn-Logik (worldspawn / config / first-player) ist robust; Entity- und Hologramm-Entities werden in Collections gehalten und in `despawnAll()` entfernt.
- API-Kompatibilität: Aufrufe wie `v.customName(...)` / `as.customName(...)` kompilierten in diesem Projekt (verwendete Bukkit/Paper-API passt). Falls Du auf andere Serverversionen targetest, prüfe ggf. die korrekten Methoden (`setCustomName` vs `customName`) und das Component-API-Verhalten.

8) StatsDisplayManager
- Mode-Handling (ACTIONBAR/BOSSBAR/SCOREBOARD/OFF) ist klar; BossBar-Updates nutzen Meter-Statistiken.
- `smoothSticky` begrenzt Abfall der geschätzten Caps, das ist sinnvoll.

9) Kleinigkeiten / Vorschläge
- Logging: Viele try/catch-Blocks fangen Throwable und ignorieren sie; für Debugging wäre es hilfreich, wenigstens im Debug-Log den Stacktrace oder `getMessage()` aufzunehmen (z. B. getLogger().finest/finestf), statt komplett zu verschlucken.
- Konstante Strings: Wiederholt verwendete Config-Pfade könnten als Konstanten in `ConfigUtil` zentrale Verwaltung bekommen.
- Unit-Tests: Es existiert bereits ein `NumberUtilTest`; für GameManager-Logik (ohne Bukkit) könnten zusätzliche Unit-Tests extrahiert werden, z. B. pause/ resume state machine.

Fazit
- Keine kritischen Fehler gefunden. Code ist robust gegenüber NullPointer und Scheduler-Aufgaben. Der Build ist sauber.
- Empfehlungen sind primär Stil/Robustheit/Observability.

Nächste Schritte (optional)
- Wenn Du möchtest, implementiere kleine Verbesserungen (z. B. besseres Logging an den Stellen, an denen Throwables geschluckt werden) und ich wende sie an und commite/pushe sie.

---
(Automatisch erzeugt durch Code-Review-Run)

