# RoundStatsManager

Package: `org.bysenom.minecraftSurvivors.manager`

Collects per-round statistics: damage by source (ability/glyph/physical), kills, coins and lootchests picked.
Exposes a snapshot after round end and writes an admin JSON report to disk.

## Public Fields

- `long startMs`
- `long endMs`

## Public Methods

- `startRound()`
- `recordDamage(UUID player, String sourceKey, double amount)`
- `recordKill(UUID player, String entityType)`
- `recordCoins(UUID player, int coins)`
- `recordLootChestPicked(UUID player)`
- `recordSourceObserved(String sourceKey)`
- `finishRoundAndSnapshot()`
- `getLastSnapshot()`
- `clearAutoReports()`
- `toJson()`
