# GameManager

Package: `org.bysenom.minecraftSurvivors.manager`


## Public Methods

- `enqueueLevelUp(java.util.UUID uuid, int level)`
- `enqueueLoot(java.util.UUID uuid)`
- `tryOpenNextQueued(java.util.UUID uuid)`
- `tryOpenNextQueuedDelayed(java.util.UUID uuid)`
- `getBossManager()`
- `getAbilityManager()`
- `enterSurvivorsContext(java.util.UUID uuid)`
- `exitSurvivorsContext(java.util.UUID uuid)`
- `isInSurvivorsContext(java.util.UUID uuid)`
- `leaveSurvivorsContext(java.util.UUID uuid)`
- `ensureClassAbility(java.util.UUID uuid)`
- `giveInitialKit(org.bukkit.entity.Player p)`
- `startGame()`
- `stopGame()`
- `protectPlayer(java.util.UUID playerUuid, int ticks)`
- `isPlayerTemporarilyProtected(java.util.UUID playerUuid)`
- `isPlayerPaused(java.util.UUID playerUuid)`
- `getState()`
- `nextWave(int waveNumber)`
- `getSpawnManager()`
- `getPlugin()`
- `reloadConfigAndApply()`
- `incrementWaveNumber()`
- `getCurrentWaveNumber()`
- `setCurrentWaveNumber(int n)`
- `abortStartCountdown(String reason)`
- `startGameWithCountdown(int seconds)`
- `beginPartyStartVote(org.bysenom.minecraftSurvivors.manager.PartyManager.Party party, int seconds)`
- `run()`
- `handlePartyVote(java.util.UUID leader, java.util.UUID member, boolean accept)`
- `handlePlayerQuit(java.util.UUID quitting)`
- `trySoloAutoStart(org.bukkit.entity.Player starter)`
