package org.bysenom.minecraftSurvivors.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;

public class PartyManager {

    public static class Party {
        private final UUID leader;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();

        private Party(UUID leader) {
            this.leader = leader;
            this.members.add(leader);
        }
        public UUID getLeader() { return leader; }
        public Set<UUID> getMembers() { return members; }
        public boolean isMember(UUID u) { return members.contains(u); }
        public void add(UUID u) { members.add(u); }
        public void remove(UUID u) { members.remove(u); }
    }

    private final Map<UUID, Party> byMember = new ConcurrentHashMap<>(); // member -> party
    private final Map<UUID, Party> byLeader = new ConcurrentHashMap<>(); // leader -> party
    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();  // target -> invite

    private static class Invite {
        final UUID leader;
        final long expiresAtMillis;
        Invite(UUID leader, long expiresAtMillis) { this.leader = leader; this.expiresAtMillis = expiresAtMillis; }
        boolean valid() { return System.currentTimeMillis() <= expiresAtMillis; }
    }

    private final org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin;

    public PartyManager(org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean createParty(UUID leader) {
        if (leader == null) return false;
        if (byMember.containsKey(leader)) return false;
        Party p = new Party(leader);
        byLeader.put(leader, p);
        byMember.put(leader, p);
        return true;
    }

    public synchronized boolean disband(UUID leader) {
        Party p = byLeader.remove(leader);
        if (p == null) return false;
        for (UUID m : p.getMembers()) {
            byMember.remove(m);
        }
        return true;
    }

    public synchronized boolean leave(UUID member) {
        Party p = byMember.get(member);
        if (p == null) return false;
        if (p.getLeader().equals(member)) {
            // leader leaving disbands the party
            return disband(member);
        } else {
            p.remove(member);
            byMember.remove(member);
            return true;
        }
    }

    public synchronized boolean invite(UUID leader, UUID target, int expireSeconds) {
        Party p = byLeader.get(leader);
        if (p == null) return false;
        if (byMember.containsKey(target)) return false; // already in party
        long expires = System.currentTimeMillis() + Math.max(1, expireSeconds) * 1000L;
        invites.put(target, new Invite(leader, expires));
        org.bukkit.entity.Player t = Bukkit.getPlayer(target);
        org.bukkit.entity.Player l = Bukkit.getPlayer(leader);
        if (t != null) t.sendMessage("§eDu wurdest von §a" + (l != null ? l.getName() : "Leader") + "§e in die Party eingeladen. Nutze §a/party join " + (l != null ? l.getName() : "<leader>") + "§e.");
        if (l != null) l.sendMessage("§aInvite gesendet an §e" + (t != null ? t.getName() : target.toString()));
        return true;
    }

    public synchronized boolean join(UUID target, UUID leader) {
        Invite inv = invites.get(target);
        if (inv == null || !inv.valid() || !inv.leader.equals(leader)) return false;
        Party p = byLeader.get(leader);
        if (p == null) return false;
        // remove prior membership if any
        Party old = byMember.get(target);
        if (old != null) {
            if (old.getLeader().equals(target)) disband(target); else leave(target);
        }
        p.add(target);
        byMember.put(target, p);
        invites.remove(target);
        return true;
    }

    public synchronized Party getPartyOf(UUID member) {
        return byMember.get(member);
    }

    public synchronized Party getByLeader(UUID leader) { return byLeader.get(leader); }

    public synchronized java.util.List<java.util.UUID> onlineMembers(Party p) {
        if (p == null) return java.util.Collections.emptyList();
        java.util.List<java.util.UUID> out = new java.util.ArrayList<>();
        for (UUID u : p.getMembers()) {
            org.bukkit.entity.Player pl = Bukkit.getPlayer(u);
            if (pl != null && pl.isOnline()) out.add(u);
        }
        return out;
    }

    public synchronized java.util.UUID getPendingInviteLeader(java.util.UUID target) {
        Invite inv = invites.get(target);
        if (inv == null) return null;
        if (!inv.valid()) { invites.remove(target); return null; }
        return inv.leader;
    }

    public synchronized boolean hasPendingInvite(java.util.UUID target) {
        return getPendingInviteLeader(target) != null;
    }
}
