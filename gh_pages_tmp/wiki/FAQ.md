# FAQ

Q: Warum startet das Spiel automatisch, obwohl ich nicht auf "Spiel starten" gedrückt habe?

A: Prüfe Party‑Status (falls du in einer Party bist) und ob alle Spieler in Survivors‑Context sind und als ready markiert sind. Der Leader kann einen Ready‑Vote starten. (Sieh auch `GameManager` für Start‑Logik.)

Q: Wie werden Glyphen eingesetzt?

A: Öffne die Glyphe‑Socket UI über das LevelUp/Inventory UI. Wenn du beim Einsammeln eine Glyphe bekommst, kannst du sie direkt auswählen oder später im Glyph‑Socket Menü einsetzten.

Q: Wie skaliert der Spawn?

A: Continuous Spawns nutzen `spawn.continuous.*` (base-per-player, growth-per-step oder growth-per-minute, warmup). Nutze `spawn.continuous.cap.*` für Dichtebegrenzung.

Weitere Fragen: erstelle ein Issue im Repo.
