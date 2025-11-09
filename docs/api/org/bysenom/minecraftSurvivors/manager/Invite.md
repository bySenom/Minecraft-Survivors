# Invite

Package: `org.bysenom.minecraftSurvivors.manager`


## Public Methods

- `createParty(UUID leader)`
- `disband(UUID leader)`
- `leave(UUID member)`
- `invite(java.util.UUID leader, java.util.UUID target, int seconds)`
- `join(UUID target, UUID leader)`
- `transferLeadership(UUID currentLeader, UUID newLeader)`
- `kickMember(UUID leader, UUID member)`
- `getPartyOf(UUID member)`
- `getByLeader(UUID leader)`
- `hasPendingInvite(java.util.UUID target)`
- `cancelInvite(java.util.UUID target)`
