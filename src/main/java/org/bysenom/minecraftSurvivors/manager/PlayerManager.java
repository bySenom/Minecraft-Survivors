// File: src/main/java/org/bysenom/minecraftSurvivors/manager/PlayerManager.java
package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final Map<UUID, SurvivorPlayer> players = new ConcurrentHashMap<>();

    public SurvivorPlayer get(UUID uuid) {
        return players.computeIfAbsent(uuid, SurvivorPlayer::new);
    }

    public java.util.Collection<SurvivorPlayer> getAll() {
        return players.values();
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public void resetAll() {
        players.values().forEach(SurvivorPlayer::softReset);
    }
}
