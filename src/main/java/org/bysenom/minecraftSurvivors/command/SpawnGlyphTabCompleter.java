package org.bysenom.minecraftSurvivors.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.bysenom.minecraftSurvivors.glyph.GlyphCatalog;
import org.jetbrains.annotations.NotNull;

public class SpawnGlyphTabCompleter implements TabCompleter {
    private static final List<String> ABILITIES = List.of(
            "ab_lightning","ab_fire","ab_ranged","ab_holy","ab_shockwave","ab_frost_nova","ab_heal_totem","ab_void_nova","ab_time_rift","ab_venom_spire"
    );

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> base = new ArrayList<>(ABILITIES);
            base.add("random");
            return StringUtil.copyPartialMatches(args[0], base, new ArrayList<>());
        }
        if (args.length == 2) {
            String ability = args[0].toLowerCase(Locale.ROOT);
            List<String> glyphs = GlyphCatalog.forAbility(ability).stream().map(g -> g.key.substring(g.key.indexOf(":")+1)).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], glyphs, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
