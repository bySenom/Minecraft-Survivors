// File: src/main/java/org/bysenom/minecraftSurvivors/model/MetaProfile.java
package org.bysenom.minecraftSurvivors.model;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Persistenter, laufübergreifender Meta-Progress für einen Spieler.
 * Enthält Meta-Währung (Essence) und permanente kleine Boni.
 */
public class MetaProfile {

    private final UUID uuid;

    // Meta-Währung
    private int essence = 0; // dauerhaft, kann im Meta-Shop ausgegeben werden

    // Permanente Boni (klein, stapelbar, gecapped über Config)
    private double permDamageMult = 0.0;   // +% Damage
    private int permHealthHearts = 0;      // zusätzliche halbe Herzen (2 = 1 Herz)
    private double permMoveSpeed = 0.0;    // +% Movement Speed
    private double permAttackSpeed = 0.0;  // +% Attack Speed
    private double permResist = 0.0;       // % Damage-Reduktion (cap via Config, z.B. 30%)
    private double permLuck = 0.0;         // +% Luck
    private int permSkillSlots = 0;        // zusätzliche Skill-Slots über Meta (Basis 1)

    public MetaProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }

    public int getEssence() { return essence; }
    public void setEssence(int essence) { this.essence = Math.max(0, essence); }
    public void addEssence(int delta) { this.essence = Math.max(0, this.essence + Math.max(0, delta)); }

    public double getPermDamageMult() { return permDamageMult; }
    public void setPermDamageMult(double v) { this.permDamageMult = Math.max(0.0, v); }
    public void addPermDamageMult(double d) { this.permDamageMult = Math.max(0.0, this.permDamageMult + Math.max(0.0, d)); }

    public int getPermHealthHearts() { return permHealthHearts; }
    public void setPermHealthHearts(int v) { this.permHealthHearts = Math.max(0, v); }
    public void addPermHealthHearts(int d) { this.permHealthHearts = Math.max(0, this.permHealthHearts + Math.max(0, d)); }

    public double getPermMoveSpeed() { return permMoveSpeed; }
    public void setPermMoveSpeed(double v) { this.permMoveSpeed = Math.max(0.0, v); }
    public void addPermMoveSpeed(double d) { this.permMoveSpeed = Math.max(0.0, this.permMoveSpeed + Math.max(0.0, d)); }

    public double getPermAttackSpeed() { return permAttackSpeed; }
    public void setPermAttackSpeed(double v) { this.permAttackSpeed = Math.max(0.0, v); }
    public void addPermAttackSpeed(double d) { this.permAttackSpeed = Math.max(0.0, this.permAttackSpeed + Math.max(0.0, d)); }

    public double getPermResist() { return permResist; }
    public void setPermResist(double v) { this.permResist = Math.max(0.0, v); }
    public void addPermResist(double d) { this.permResist = Math.max(0.0, this.permResist + Math.max(0.0, d)); }

    public double getPermLuck() { return permLuck; }
    public void setPermLuck(double v) { this.permLuck = Math.max(0.0, v); }
    public void addPermLuck(double d) { this.permLuck = Math.max(0.0, this.permLuck + Math.max(0.0, d)); }

    public int getPermSkillSlots() { return permSkillSlots; }
    public void setPermSkillSlots(int v) { this.permSkillSlots = Math.max(0, v); }
    public void addPermSkillSlots(int d) { this.permSkillSlots = Math.max(0, this.permSkillSlots + Math.max(0, d)); }

    /**
     * Überträgt die permanenten Boni auf den SurvivorPlayer für den nächsten Run.
     * Sollte beim Run-Start aufgerufen werden, nachdem der SurvivorPlayer resettet wurde.
     */
    public void applyTo(SurvivorPlayer sp, Player bukkitPlayer) {
        if (sp == null) return;
        // Multiplikatoren additiv in SurvivorPlayer addieren
        sp.addDamageMult(this.permDamageMult);
        sp.addMoveSpeedMult(this.permMoveSpeed);
        sp.addAttackSpeedMult(this.permAttackSpeed);
        sp.addDamageResist(this.permResist);
        sp.addLuck(this.permLuck);
        // Permanente Herzen: auf Model speichern (Bukkit-Attribut wird andernorts angepasst)
        if (this.permHealthHearts > 0) {
            sp.addExtraHearts(this.permHealthHearts);
        }
        // Skill-Slots: Basis 1 + Meta-Slots, gedeckelt auf 5
        sp.setMaxSkillSlots(Math.min(5, 1 + this.permSkillSlots));
    }
}
