# LobbySystem TODO

## Prioritätsskala
- P1 = Release-kritisch (vor erstem öffentlichen Release erledigen)
- P2 = Wichtige Qualitäts-/Spielerlebnisverbesserung (bald danach)
- P3 = Komfort / Nice-to-have / spätere Version

## Übersicht
Dies ist die Aufgabenliste NUR für das LobbySystem. Änderungen am Kernspiel (Minecraft Survivors) stehen in einer separaten Datei.

---
## Status-Update (2025-11-09)
- P1.1 Auto-Admit Scheduler: ERLEDIGT (setupAdmissionLoop mit `admission.enabled`, `admission.interval-seconds`, Start optional via `survivors.auto-dispatch-enabled`).
- P1.2 Maximal zugelassene Spieler (Capacity): ERLEDIGT (Limit via `admission.max-admitted` in `admitNext`). Hinweis: /queue status "Voll"-Hinweis noch offen.
- P1.3 Disconnect-Cleanup: ERLEDIGT (Quit-Listener entfernt auch admitted/Interna; BossBar wird entfernt; Survivors-Kontext wird verlassen).
- P1.4 Admission Timeout: ERLEDIGT (enforceAdmissionTimeout; `admission.timeout-seconds`, `admission.timeout-action` = recycle|remove).
- P1.5 Re-Join Cooldown: ERLEDIGT (`queue.rejoin-cooldown-seconds`).
- P1.6 Persistenz: ERLEDIGT (`queue.persist-enabled`; speichert/ladet queue/admitted).
- P1.7 Reset-Hook bei Rundenende: OFFEN (TODO: Survivors-GameStop erkennen, `resetAdmission()` ausführen, BossBar ggf. re-syncen).
- P1.8 Logging & Metriken Basis: TEILWEISE (Debug-Logs + avg wait vorhanden; /queue stats Command noch OFFEN).
- P1.9 Fehlerrobustheit Survivors nicht vorhanden: ERLEDIGT (Reflection try/catch, isSurvivorsLobbyState fallback).
- P1.10 README Abschnitt "LobbySystem": OFFEN.

Nächste Schritte (P1 Rest):
1) Reset-Hook auf Survivors-Rundenende verdrahten (GameStop erkennen + `resetAdmission()`).
2) `/queue stats` Admin-Command (size, admittedCount, avgWait).
3) `/queue status`: Hinweis "Server voll" zeigen, wenn `admission.max-admitted` erreicht wurde.
4) README Abschnitt zu Konfig und Abläufen ergänzen.

---
## P1 (Release-kritisch)

### 1. Auto-Admit Scheduler
Beschreibung: Automatische Zulassung des nächsten Wartenden in festen Intervallen.
Akzeptanzkriterien:
- Konfig-Key `admission.interval-seconds` steuert Intervall.
- Task läuft nur, wenn Survivors-Plugin verfügbar und Spiel NICHT läuft oder freie Slots existieren.
- Kein Deadlock: Task stoppt sauber beim Plugin-Disable.

### 2. Maximal zugelassene Spieler (Capacity)
Beschreibung: Limit der gleichzeitig "admitted" Spieler.
Akzeptanzkriterien:
- Konfig-Key `admission.max-admitted`.
- admitNext() respektiert Limits (wartet bis Platz frei).
- /queue status zeigt "Voll" Hinweis.

### 3. Disconnect-Cleanup
Beschreibung: Spieler soll beim Logout automatisch Queue verlassen.
Akzeptanzkriterien:
- Listener entfernt Spieler aus `queue` und `admitted`.
- BossBar wird entfernt.
- Survivors-Kontext wird verlassen.

### 4. Admission Timeout
Beschreibung: Spieler verliert seinen admitted-Status, wenn er nicht innerhalb X Sekunden Klasse auswählt / Survivors startet.
Akzeptanzkriterien:
- Konfig-Key `admission.timeout-seconds`.
- Timeout setzt Spieler zurück ans Queue-Ende ODER entfernt ihn (konfigurierbar: `admission.timeout-action` = recycle|remove).
- Nachricht an Spieler.

### 5. Re-Join Cooldown
Beschreibung: Schutz vor Spam /queue join.
Akzeptanzkriterien:
- Konfig-Key `queue.rejoin-cooldown-seconds`.
- Spieler erhält klare Fehlermeldung bei zu frühem Re-Join.

### 6. Persistenz (Optional Minimal)
Beschreibung: Option die Queue beim Server-Neustart NICHT zu verlieren.
Akzeptanzkriterien:
- Konfig-Key `queue.persist-enabled`.
- Speichern als einfache JSON beim Shutdown, Laden beim Start.
- Fehlerhandling: Falls Datei korrupt -> Start mit leerer Queue.

### 7. Reset-Hook bei Rundenende
Beschreibung: LobbySystem reagiert wenn Survivors-Spiel endet.
Akzeptanzkriterien:
- Event/Reflektion erkennt GameStop.
- `resetAdmission()` aufrufen.
- Alle im Survivors-Kontext aber nicht mehr spielenden Spieler zurück in Lobby (BossBar ggf. neu anzeigen).

### 8. Logging & Metriken Basis
Beschreibung: Grundlegende Log-Ausgaben + expose counts.
Akzeptanzkriterien:
- Debug-Flag `queue.debug` aktiviert detail logs (join/leave/admit).
- Command `/queue stats` (Admin) zeigt: size, admittedCount, avg wait time (wenn trackbar).

### 9. Fehlerrobustheit Survivors nicht vorhanden
Beschreibung: Lobby läuft auch wenn das Survivors-Plugin fehlt.
Akzeptanzkriterien:
- Alle Reflection-Aufrufe try-catch.
- Keine Fehler im Log, stattdessen Warnung einmalig.

### 10. README Abschnitt "LobbySystem"
Beschreibung: Kurze Doku wie Queue und Admission funktionieren.
Akzeptanzkriterien:
- Schritte: Installation, Konfig, Start.
- Erläuterung der wichtigsten Konfig-Keys.

---
## P2 (Wichtig, nach erstem Release)

### 11. Verbesserte ETA Berechnung
Beschreibung: Dynamische ETA basierend auf letzten N Zulassungen.
Akzeptanzkriterien:
- Gleitender Durchschnitt in `/queue status`.

### 12. Prioritäts-Tiers (VIP)
Beschreibung: Spieler mit Permission haben Vorrang.
Akzeptanzkriterien:
- Permission `queue.priority.vip` verschiebt Spieler nach vorne / separater Buffer.
- Kein Starve normaler Spieler (Fairness-Regel: nach X VIP muss 1 normaler).

### 13. AFK-Erkennung
Beschreibung: Entfernt AFK-Spieler aus Queue.
Akzeptanzkriterien:
- Bewegung / Interaktion reset Timer.
- Konfig-Key `queue.afk-timeout-seconds`.

### 14. GUI für Queue (/queue gui)
Beschreibung: Inventar mit eigener Position, ETA, Buttons (Leave, Refresh).
Akzeptanzkriterien:
- Sicher gegen Item-Duplizierung (cancel clicks).

### 15. Mehrsprachigkeit (messages.yml)
Beschreibung: Alle hart codierten Nachrichten in externe Datei.
Akzeptanzkriterien:
- Fallback bei fehlender Message.

### 16. Webhook/Externes Logging
Beschreibung: Admission-Ereignis an HTTP-Endpoint senden.
Akzeptanzkriterien:
- Async, Retry bei Fehler.
- Konfig-Key URL + enabled.

### 17. Performance/Profiler Hook
Beschreibung: Periodisch queue/admitted Größen loggen (für Analyse).
Akzeptanzkriterien:
- Konfig-Key `queue.metrics-interval-seconds`.

### 18. Konsolen-Command für Mass-Clear
Beschreibung: `/queue clear [admitted|all]`.
Akzeptanzkriterien:
- Nur Admin, sichere Bestätigung.

---
## P3 (Nice-to-have / später)

### 19. Weighted Fair Queue / Segmentierung
### 20. Historie zuletzt zugelassener Spieler (/queue history)
### 21. Visuelle Effekte beim Admission (Partikelkreis configurable)
### 22. Integration mit Freundesliste: Freund bevorzugt (optional)
### 23. API-Hooks für andere Plugins (Events: QueueJoinEvent, QueueLeaveEvent, QueueAdmitEvent)
### 24. Web-Dashboard (REST Endpoint für Queue Status)
### 25. Schneller Switch bei erneutem Spiel (Priority Re-Entry für zuletzt gespielte)

---
## Tracking / Metriken (technisch)
- Wartezeitstart = Zeitstempel beim `join()`
- Wartezeitende = Zeit bei `admitNext()`
- avgWaitTime = Summe / Anzahl
- Optional Persistenz: totals auf Disk speichern

---
## Qualitätssicherung (QS)
- Testfälle:
  - Join/Leave/Offline Remove
  - Mehrfaches admitNext bis leer
  - Timeout Re-queue
  - Survivors fehlt -> keine Exceptions

- Edge Cases:
  - Spieler offline kurz vor Admission
  - Kapazität 0
  - Re-Join innerhalb Cooldown
  - Persist-Datei korrupt

---
## Umsetzungsvorschlag Reihenfolge
1. Scheduler + Kapazität
2. Disconnect Cleanup
3. Timeout
4. Re-Join Cooldown
5. Reset Hook
6. Logging/Metriken
7. README Abschnitt
8. Persistenz

Fertig für Release wenn P1 abgeschlossen.
