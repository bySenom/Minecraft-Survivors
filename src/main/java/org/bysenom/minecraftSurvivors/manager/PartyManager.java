package org.bysenom.minecraftSurvivors.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PartyManager {

    public static class Party {
        private UUID leader;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();

        private Party(UUID leader) {
            this.leader = leader;
            this.members.add(leader);
        }
        public UUID getLeader() { return leader; }
        public void setLeader(UUID newLeader) { this.leader = newLeader; }
        public Set<UUID> getMembers() { return members; }
        public boolean isMember(UUID u) { return members.contains(u); }
        public void add(UUID u) { members.add(u); }
        public void remove(UUID u) { members.remove(u); }
    }

    private final Map<UUID, Party> byMember = new ConcurrentHashMap<>(); // member -> party
    private final Map<UUID, Party> byLeader = new ConcurrentHashMap<>(); // leader -> party
    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();  // target -> invite
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>(); // target -> leader

    // invite cooldown per leader->target key
    private final Map<String, Long> inviteCooldowns = new ConcurrentHashMap<>();
    private static final long INVITE_COOLDOWN_MS = 10_000L; // 10 seconds cooldown for repeated invites (assumption)

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
            Player pl = Bukkit.getPlayer(m);
            if (pl != null && pl.isOnline()) {
                pl.sendMessage(Component.text("Die Party wurde aufgelöst.").color(NamedTextColor.YELLOW));
                // visual feedback
                pl.playSound(pl.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.6f, 0.9f);
                pl.getWorld().spawnParticle(Particle.LARGE_SMOKE, pl.getLocation().add(0,1,0), 10, 0.5,0.5,0.5, 0.02);
            }
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
            Player pl = Bukkit.getPlayer(member);
            if (pl != null && pl.isOnline()) {
                pl.sendMessage(Component.text("Du hast die Party verlassen.").color(NamedTextColor.GRAY));
                pl.playSound(pl.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                pl.getWorld().spawnParticle(Particle.CLOUD, pl.getLocation().add(0,1,0), 8, 0.3,0.3,0.3, 0.01);
            }
            // notify remaining members
            for (UUID u : p.getMembers()) {
                Player other = Bukkit.getPlayer(u);
                if (other != null && other.isOnline()) {
                    other.sendMessage(Component.text(nameOf(member) + " hat die Party verlassen.").color(NamedTextColor.YELLOW));
                    other.playSound(other.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                }
            }
            return true;
        }
    }

    public synchronized boolean invite(java.util.UUID leader, java.util.UUID target, int seconds) {
        if (leader == null || target == null) return false;
        // Prevent inviting someone who already has a valid invite
        Invite existing = invites.get(target);
        if (existing != null && existing.valid()) return false;
        // check leader is leader of a party
        Party p = getPartyOf(leader);
        if (p == null || !leader.equals(p.getLeader())) return false;

        // cooldown per leader-target
        String key = leader + "#" + target;
        Long cd = inviteCooldowns.get(key);
        if (cd != null && System.currentTimeMillis() < cd) {
            Player leaderPl = Bukkit.getPlayer(leader);
            if (leaderPl != null && leaderPl.isOnline()) {
                leaderPl.sendMessage(Component.text("Warte kurz bevor du erneut einlädst.").color(NamedTextColor.RED));
            }
            return false;
        }

        long ttl = System.currentTimeMillis() + Math.max(5L, (long) seconds) * 1000L;
        invites.put(target, new Invite(leader, ttl));
        pendingInvites.put(target, leader);
        // set cooldown so leader can't spam invites to same target
        inviteCooldowns.put(key, System.currentTimeMillis() + INVITE_COOLDOWN_MS);

        org.bukkit.Bukkit.getScheduler().runTaskLater(org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance(), () -> {
            Invite inv = invites.get(target);
            if (inv != null && !inv.valid()) {
                invites.remove(target);
                pendingInvites.remove(target);
            }
        }, 20L * Math.max(5, seconds));

        org.bukkit.entity.Player tp = org.bukkit.Bukkit.getPlayer(target);
        if (tp != null) {
            // send clickable accept/decline message
            sendClickableInviteMessage(tp, leader, seconds);
        }
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
        pendingInvites.remove(target);

        // Visual + sound feedback to party
        for (UUID u : p.getMembers()) {
            Player pl = Bukkit.getPlayer(u);
            if (pl != null && pl.isOnline()) {
                if (u.equals(target)) {
                    pl.sendMessage(Component.text("Du bist der Party beigetreten: ").color(NamedTextColor.GREEN)
                            .append(Component.text(nameOf(leader)).color(NamedTextColor.WHITE)));
                    pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    pl.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pl.getLocation().add(0,1,0), 12, 0.4,0.4,0.4, 0.02);
                } else {
                    pl.sendMessage(Component.text(nameOf(target) + " ist der Party beigetreten.").color(NamedTextColor.YELLOW));
                    pl.playSound(pl.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
                    pl.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pl.getLocation().add(0,1,0), 8, 0.3,0.3,0.3, 0.02);
                }
            }
        }

        return true;
    }

    /** Transfer leadership from current leader to another member in the party. */
    public synchronized boolean transferLeadership(UUID currentLeader, UUID newLeader) {
        if (currentLeader == null || newLeader == null) return false;
        Party p = byLeader.get(currentLeader);
        if (p == null) return false;
        if (!p.isMember(newLeader)) return false;
        // remove old mapping, set new leader
        byLeader.remove(currentLeader);
        p.setLeader(newLeader);
        byLeader.put(newLeader, p);
        // notify members
        for (UUID u : p.getMembers()) {
            Player pl = Bukkit.getPlayer(u);
            if (pl != null && pl.isOnline()) {
                pl.sendMessage(Component.text(nameOf(newLeader) + " ist nun Leader der Party.").color(NamedTextColor.GOLD));
                pl.playSound(pl.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.8f, 1.0f);
            }
        }
        return true;
    }

    /** Kick a member from the party; only leader can kick. */
    public synchronized boolean kickMember(UUID leader, UUID member) {
        if (leader == null || member == null) return false;
        Party p = byLeader.get(leader);
        if (p == null) return false;
        if (!p.isMember(member)) return false;
        if (member.equals(leader)) return false; // leader can't kick self (use leave/disband)
        p.remove(member);
        byMember.remove(member);
        Player pl = Bukkit.getPlayer(member);
        if (pl != null && pl.isOnline()) {
            pl.sendMessage(Component.text("Du wurdest aus der Party entfernt.").color(NamedTextColor.RED));
            pl.playSound(pl.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
        }
        // notify remaining
        for (UUID u : p.getMembers()) {
            Player other = Bukkit.getPlayer(u);
            if (other != null && other.isOnline()) {
                other.sendMessage(Component.text(nameOf(member) + " wurde aus der Party entfernt.").color(NamedTextColor.YELLOW));
                other.playSound(other.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
        }
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
        if (!inv.valid()) { invites.remove(target); pendingInvites.remove(target); return null; }
        return inv.leader;
    }

    public synchronized boolean hasPendingInvite(java.util.UUID target) {
        return getPendingInviteLeader(target) != null;
    }

    /** Cancel a pending invite for a target (if any). */
    public synchronized boolean cancelInvite(java.util.UUID target) {
        if (target == null) return false;
        boolean removed = false;
        if (invites.remove(target) != null) removed = true;
        if (pendingInvites.remove(target) != null) removed = true;
        return removed;
    }

    /**
     * Send a clickable accept/decline message to a player. Clicking will run commands:
     * /party accept <leaderUUID>  and /party decline <leaderUUID>
     * The server must have handlers for these commands.
     */
    private void sendClickableInviteMessage(Player targetPlayer, UUID leader, int seconds) {
        String leaderName = nameOf(leader);

        Component header = Component.text("Party-Einladung von ").color(NamedTextColor.GOLD)
                .append(Component.text(leaderName).color(NamedTextColor.YELLOW))
                .append(Component.text("\n"));

        Component accept = Component.text("[Annehmen]").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/party accept " + leader))
                .hoverEvent(HoverEvent.showText(Component.text("Klicke um die Einladung anzunehmen")));

        Component decline = Component.text("[Ablehnen]").color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/party decline " + leader))
                .hoverEvent(HoverEvent.showText(Component.text("Klicke um die Einladung abzulehnen")));

        Component full = header.append(accept).append(Component.text(" ")).append(decline);

        targetPlayer.sendMessage(full);
        targetPlayer.sendMessage(Component.text("Die Einladung verfällt in " + seconds + " Sekunden.").color(NamedTextColor.GRAY));
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.0f);
    }

    private static String nameOf(UUID id) {
        try {
            String n = Bukkit.getOfflinePlayer(id).getName();
            return n == null ? id.toString() : n;
        } catch (Throwable t) { return id.toString(); }
    }
}
