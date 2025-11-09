# GlyphPickupListener

Package: `org.bysenom.minecraftSurvivors.listener`

Glyphen-Pickup wie Lootchests: schwebende Items mit Hologramm, bei Berührung öffnet sich ein GlyphSocket-UI für eine Ability.

## Public Methods

- `setPendingGlyph(java.util.UUID playerUuid, String glyphKey)`
- `getPendingGlyph(java.util.UUID playerUuid)`
- `consumePendingGlyph(java.util.UUID playerUuid)`
- `clearPendingFor(java.util.UUID playerUuid)`
- `setPendingGlyphWithLog(java.util.UUID playerUuid, String glyphKey)`
- `spawnGlyph(Location loc, String abilityKey)`
- `run()`
- `onMove(PlayerMoveEvent e)`
- `spawnRandomGlyphNear(Location around, String abilityKey)`
- `setSelectionOpen(java.util.UUID playerUuid, boolean open)`
- `isSelectionOpen(java.util.UUID playerUuid)`
- `setSelectionContext(java.util.UUID playerUuid, String abilityKey, int slot)`
- `clearSelectionContext(java.util.UUID playerUuid)`
- `markSelectionHandled(java.util.UUID playerUuid)`
- `consumeSelectionHandled(java.util.UUID playerUuid)`
- `setNoReopenFor(java.util.UUID playerUuid, int seconds)`
- `isNoReopen(java.util.UUID playerUuid)`
- `clearNoReopen(java.util.UUID playerUuid)`
