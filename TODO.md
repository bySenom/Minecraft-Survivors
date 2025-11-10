# Minecraft Survivors TODO

## Prioritätsskala
- P1 = Release-kritisch (vor erstem öffentlichen Release erledigen)
- P2 = Wichtige Qualitäts-/Spielerlebnisverbesserung (bald danach)
- P3 = Komfort / Nice-to-have / spätere Version

## Übersicht
Dies ist die Aufgabenliste für das Kernspiel (Minecraft Survivors). Das LobbySystem hat eine eigene TODO unter `lobby/TODO.md`.

---
Status-Update (2025-11-10)
- Erledigt (Code): MAX_HEALTH Reset; CombatEngine zentralisiert; Endboss: Meteor-Cleanup, Telegraphs, ArmorStand-Name Fixes; Persistenz: `PlayerDataManager` & `SurvivorPlayer` (Grundlegende Felder + save/load) implemented; viele Particle-Aufrufe wurden auf `ParticleUtil` umgestellt; mehrere Hintergrund-Tasks (Lootchest, GlyphPickup, Skill charge, continuous spawn/wave tasks) werden beim Stop/Disable abgebrochen.
- In Arbeit: FX-Throttling Rate-Limiter Feintuning; Wellen/Continuous Stabilität (Audit abgeschlossen, fixes eingepflegt — weiteres QA erforderlich); Dokumentation (README/Docs).

## P1 (Release-kritisch)

### 1. Endboss (ERLEDIGT - 2025-11-10)
Beschreibung: Custom Boss mit Phasen (P1/P2/P3), Mechaniken (Meteor Barrage, Lightning Beam, Summons), klare Telegraphs.
Status: Erledigt — Boss-System ist implementiert und konfigurierbar.
Was implementiert wurde:
- Boss spawnt verlässlich bei Enrage >= 1.0 (`BossManager.trySpawnBoss`).
- Phasen-Logik (P1/P2/P3) mit `onPhaseEnter` und konfigurierbaren Phase-Multiplikatoren (`endgame.boss.phase.*`).
- Boss-Fähigkeiten: Meteor, Meteor Barrage, Lightning Chain, Shockwave, Summons (Debug-API in `/msboss`).
- Ability-Weights & Cooldowns: konfigurierbar über `endgame.boss.ability.weights.*` und `endgame.boss.ability.cooldowns.*`.
- Visuelles Feedback: Adventure `BossBar`, Name-ArmorStand, HP-Hologram (TextDisplay) implementiert.
- Meteor/Munition-Cleanup: Projektile/FallingBlocks werden aufgeräumt (kein leftover blocks).
- Safe scheduling: alle `runTaskLater`/`runTaskTimer`-Aufrufe werden getrackt und beim Stop gecancelt (`scheduledTasks`, `clearUi()`).
- Debug/Testing: `BossDebugCommand` bietet Spawn/Kill/Test-Abilities.

Anmerkung: Balancing (Schadenswerte, Spawn-Counts) und finale QA sind weiterhin offen; diese Arbeit ist konfigurierbar und wird getrennt als Balancing-Iteration durchgeführt.

### 2. Neue Stats in UI/Progression verfügbar (IN ARBEIT - 2025-11-10)
Beschreibung: LevelUp/Shop/Loot bieten und zeigen neue Stats (Max Health, HP Regen, Shield, Armor, Evasion, Lifesteal, Thorns, Crit Chance, Crit Damage, Projectile Count, Attack Speed, Projectile Bounce, Size, Duration, Damage vs. Elites/Bosses, Knockback, Jump Height, XP Gain, Elite Spawn Increase, Powerup Multiplier).
Status: In Arbeit — Basis-Implementierung vorhanden (LevelUp-Menü zeigt Stat-Picks, `GuiManager` zeigt Stat-Overview, `GameManager` ActionBar-HUD zeigt XP + Level).
Was wurde bereits implementiert (P1.2 - erledigt):
- LevelUp-Menü zeigt Stat-Picks + kurze Beschreibungen (LevelUpMenu).
- Basis-Persistenz für wichtige Basis-Stats (`hpRegenBase`, `damageMult`, `flatDamage`, `extraHearts`, u.a.) implementiert in `PlayerDataManager`.
- UI Rundung/Format: HUD/Stats/LevelUp vereinheitlicht (1 Dezimalstelle).
- LevelUp → automatische Speicherung (saveAsync) nach Auswahl / beim LevelUp (LevelUpMenu + EntityDeathListener).
- Clear Inventory & final save on run end implemented (GameManager.stopGame).

Offene/zu priorisierende Tasks (klein, gezielt):
- QA: Verifizieren, dass Stat-Modifikatoren von Glyphen/Items korrekt wirken und in UI reflektiert werden (Mehrspieler, Disconnect/Reconnect).
- Persistenz & Reset: Sicherstellen, dass temporäre Run-Boni zurückgesetzt werden und nur gewünschte Basis-Stats dauerhaft bleiben; insbesondere: Clear Inventory / Entfernen temporärer Items nach Run-Ende oder Player-Death.
- Glyphen-Logik (Verbesserung): Wenn Ability-Glyph-Slots voll sind, sinnvollere Auswahl/Levelup-Mechanik anzeigen (OFFEN).

### 3. Performance & FX-Throttling
Beschreibung: FX abhängig von Distanz/FX-Setting drosseln.
Akzeptanzkriterien:
- Match new FX effects with player abilites and Glpyhs
- Globaler Regler / Spieler-FX Toggle ergänzt um Rate-Limiter.  # (Basis implementiert: `ParticleUtil` verwendet; Feintuning offen)
- Große Kämpfe verursachen keine Lags. (Verify under load)

### 4. Persistenz & Reset der neuen Stats
Beschreibung: Sicheres Laden/Speichern aller neuen Stats + Reset bei Run-Beginn.
Akzeptanzkriterien:
- PlayerData migriert (Backward-Compat).  # (Grundlegende Migration & Backwards-Compat-Handling implementiert)
- Start eines Runs setzt temporäre Boni korrekt zurück.  # (softReset / softResetPreserveSkills vorhanden und genutzt)
- Bessere Glyphen-Logik: wenn alle Slots belegt, dann Levelup der Fähigkeit; nur Glyphen anzeigen, die noch nicht gesockelt sind. (OFFEN)
- Clear Inventory: After Round End / playerDeath

### 5. (entfernt) Stabilität Wellen/Continuous Mode
- Implementiert: Stop/Cancel-Logik der wichtigsten BukkitRunnables (Lootchest, GlyphPickup, SkillListener, SpawnManager continuous/scaling/aggro) und `MinecraftSurvivors.onDisable()` ruft `gameManager.stopGame()`; verbleibende Aufgaben sind QA/Verifikation, keine weiteren Code-Änderungen nötig.

### 6. Doku (README/Docs)
Beschreibung: Installation, Konfiguration, neue Stats, FX-Commands (/fx …) kurz dokumentiert.
Akzeptanzkriterien:
- README Abschnitt „Getting Started" + „Config cheatsheet". (in Arbeit)

## P2 (Wichtig, nach erstem Release)

### 7. Balancing-Pass
Beschreibung: Schadensskalierung, Crit Caps, Lifesteal Grenzen, Shield-Regen-Delay.
Akzeptanzkriterien:
- Konfigurierbare Caps (ConfigUtil Keys), dokumentiert.

### 8. Elite-/Boss-Damage Multipliers + Anzeige
Beschreibung: Separate Multiplikatoren gegen Elites/Bosse, UI Anzeige.
Akzeptanzkriterien:
- Stat wirkt in CombatEngine; HUD zeigt kurzen Hinweis/Symbol.

### 9. Erweiterte FX (optional abschaltbar)
Beschreibung: Fancy Visuals für Shaman/Holy/Venom/Ranged/Fire ausbauen, inkl. Toggle & Distanzcheck.

### 10. Test-Kommandos bündeln (/msdebug)
Beschreibung: Einheitlicher Debug-Einstieg: FX-Tests, Spawn, Stats-Print.

### 11. Analytics/StatsMeter
Beschreibung: Laufzeit, Kills, Schaden/Heilung, meistgenutzte Abilities.

### 12. Presets (easy/normal/hard)
Beschreibung: Konfig-Presets für schnelle Server-Anpassung.

## P3 (Nice-to-have / später)

### 13. Modulare Boss-Fähigkeiten (Katalog + Weights)
### 14. Seasons/Meta-Progression Ausbau
### 15. Cloud-Speicher für PlayerData (optional)
### 16. Mod-/Resourcenpack für FX (höhere Qualität)

## Qualitätssicherung (QS)
- Unit/Integration Tests: CombatEngine Kernfälle + Persistence.
- Manuelle Tests: Multi-User, Disconnect/Reconnect, FX Performance.
- Profiling: Ticks-per-Second unter Load > 18 TPS.

---
## Umsetzungsvorschlag Reihenfolge
1. Endboss Finalisierung
2. Performance/FX Throttle
3. Persistenz/Reset
4. Doku + Presets
5. Balancing

Release, wenn P1 abgeschlossen.
