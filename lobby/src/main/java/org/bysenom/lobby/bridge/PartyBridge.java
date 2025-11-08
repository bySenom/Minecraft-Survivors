package org.bysenom.lobby.bridge;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Locker gekoppelte Bridge zum PartyManager des MinecraftSurvivors-Plugins.
 * Nutzt Reflection zur Laufzeit, um keine Compile-Time-Abhängigkeit zu haben.
 */
public class PartyBridge {

    private Object survivorsMain; // org.bysenom.minecraftSurvivors.MinecraftSurvivors
    private Object partyManager;  // org.bysenom.minecraftSurvivors.manager.PartyManager

    private Method mGetPartyManager; // () -> PartyManager
    private Method mGetPartyOf;      // (UUID) -> Party
    private Method mGetLeader;       // Party#getLeader() -> UUID
    private Method mGetMembers;      // Party#getMembers() -> Set<UUID>

    public boolean ensureReady() {
        try {
            if (survivorsMain == null) {
                Plugin p = Bukkit.getPluginManager().getPlugin("MinecraftSurvivors");
                if (p == null || !p.isEnabled()) return false;
                survivorsMain = p; // JavaPlugin-Instanz
                mGetPartyManager = survivorsMain.getClass().getMethod("getPartyManager");
            }
            if (partyManager == null) {
                partyManager = mGetPartyManager.invoke(survivorsMain);
                if (partyManager == null) return false;
                Class<?> pmClass = partyManager.getClass();
                mGetPartyOf = pmClass.getMethod("getPartyOf", UUID.class);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private Object getPartyFor(UUID uuid) {
        try {
            if (!ensureReady()) return null;
            return mGetPartyOf.invoke(partyManager, uuid);
        } catch (Throwable t) { return null; }
    }

    public boolean hasParty(Player p) {
        try {
            return getPartyFor(p.getUniqueId()) != null;
        } catch (Throwable t) { return false; }
    }

    public List<String> listMembers(Player viewer) {
        try {
            Object party = getPartyFor(viewer.getUniqueId());
            if (party == null) return Collections.emptyList();
            Class<?> partyClass = party.getClass();
            if (mGetLeader == null) mGetLeader = partyClass.getMethod("getLeader");
            if (mGetMembers == null) mGetMembers = partyClass.getMethod("getMembers");
            @SuppressWarnings("unchecked")
            java.util.Set<UUID> uuids = (java.util.Set<UUID>) mGetMembers.invoke(party);
            UUID leader = (UUID) mGetLeader.invoke(party);
            List<String> out = new ArrayList<>();
            for (UUID id : uuids) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String name = op.getName() != null ? op.getName() : id.toString();
                boolean isLeader = id.equals(leader);
                out.add((isLeader ? "* " : "  ") + name);
            }
            return out;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public UUID getLeaderUuid(Player p) {
        try {
            Object party = getPartyFor(p.getUniqueId());
            if (party == null) return null;
            if (mGetLeader == null) mGetLeader = party.getClass().getMethod("getLeader");
            return (UUID) mGetLeader.invoke(party);
        } catch (Throwable t) { return null; }
    }

    public Set<UUID> getMemberUuids(Player p) {
        try {
            Object party = getPartyFor(p.getUniqueId());
            if (party == null) return Collections.emptySet();
            if (mGetMembers == null) mGetMembers = party.getClass().getMethod("getMembers");
            @SuppressWarnings("unchecked")
            Set<UUID> uuids = (Set<UUID>) mGetMembers.invoke(party);
            return new HashSet<>(uuids);
        } catch (Throwable t) { return Collections.emptySet(); }
    }

    public boolean isLeader(Player p) {
        UUID leader = getLeaderUuid(p);
        return leader != null && leader.equals(p.getUniqueId());
    }

    public boolean leave(Player p) {
        // Delegiere an bestehenden /party leave Command, um Logik serverseitig einheitlich zu halten
        return p.performCommand("party leave");
    }

    public boolean disbandIfLeader(Player p) {
        // In PartyCommand: leader kann disbanden. Wir nehmen denselben Pfad über Command
        return p.performCommand("party disband");
    }

    public boolean invite(Player leader, Player target, int seconds) {
        // Delegiere an PartyCommand
        return leader.performCommand("party invite " + target.getName() + " " + Math.max(5, seconds));
    }

    public boolean kick(Player leader, Player target) {
        return leader.performCommand("party kick " + target.getName());
    }

    public boolean promote(Player leader, Player target) {
        return leader.performCommand("party promote " + target.getName());
    }
}
