# ConfigEditSessionManager

Package: `org.bysenom.minecraftSurvivors.manager`


## Public Fields

- `UUID player`
- `String path`
- `Object oldValue`
- `Object newValue`
- `boolean awaitingChatInput`
- `long expiresAt`

## Public Methods

- `startChatSession(UUID player, String path)`
- `getSession(UUID player)`
- `setNewValue(UUID player, Object newValue)`
- `clearSession(UUID player)`
- `applySession(UUID player)`
