# LobbySystem TODO

## Prioritätsskala
- P1 = Release-kritisch (vor erstem öffentlichen Release erledigen)
- P2 = Wichtige Qualitäts-/Spielerlebnisverbesserung (bald danach)
- P3 = Komfort / Nice-to-have / spätere Version

## Übersicht
Dies ist die Aufgabenliste NUR für das LobbySystem. Änderungen am Kernspiel (Minecraft Survivors) stehen in einer separaten Datei.

---
## Status-Update (2025-11-09)
- Erledigt: Auto-Admit Scheduler, Capacity Limit, Disconnect-Cleanup, Admission Timeout, Re-Join Cooldown, Persistenz, Reset-Hook, Logging (/queue stats), Voll-Hinweis, README Abschnitt, Fehlerrobustheit, Verbesserte ETA, AFK-Erkennung (Basis: Activity-Listener + periodisches Pruning über `queue.afk-timeout-seconds`).
- Offen (P2/P3): VIP-Prio, /queue gui, messages.yml, Webhook, Profiler Hook, Clear Command, weiterführende Nice-to-have Features.

---
## P2 (Wichtig, nach erstem Release)

### 12. Prioritäts-Tiers (VIP)
Beschreibung: Spieler mit Permission haben Vorrang.

### 14. GUI für Queue (/queue gui)
Beschreibung: Inventar mit Position/ETA/Buttons.

### 15. Mehrsprachigkeit (messages.yml)
Beschreibung: Externe Messages, Fallback bei fehlenden Keys.

### 16. Webhook/Externes Logging
Beschreibung: Admission-Ereignis an HTTP Endpoint.

### 17. Performance/Profiler Hook
Beschreibung: Periodisches Logging/Metrik-Intervall.

### 18. Konsolen-Command für Mass-Clear
Beschreibung: `/queue clear [admitted|all]` mit Bestätigung.

---
## P3 (Nice-to-have / später)

19. Weighted Fair Queue / Segmentierung
20. Historie zuletzt zugelassener Spieler (/queue history)
21. Visuelle Admission-Effekte (konfigurierbar)
22. Freundeslisten-Bevorzugung (optional)
23. API Events (QueueJoinEvent, QueueLeaveEvent, QueueAdmitEvent)
24. Web-Dashboard / REST Status
25. Priority Re-Entry für zuletzt gespielte

---
## Tracking / Metriken (technisch)
- Wartezeitstart = Zeitstempel beim `join()`
- Wartezeitende = Zeit bei `admitNext()`
- avgWaitTime = Summe / Anzahl

---
## Qualitätssicherung (QS)
- Test: Offline während admission, Persistenz-Datei korrupt, Rejoin-Cooldown.

---
## Nächste Entscheidung
Wähle 1–2 P2 Features für den nächsten Sprint (Empfehlung: VIP + messages.yml oder /queue gui + messages.yml).
