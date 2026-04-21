package nl.openminetopia.utils.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.police.utils.BalaclavaUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Short PlaceholderAPI identifier {@code openmt} (e.g. {@code %openmt_balaclava%}).
 */
public class OpenMtExpansion extends PlaceholderExpansion {

    private final OpenMinetopia plugin = OpenMinetopia.getInstance();

    @Override
    public @NotNull String getIdentifier() {
        return "openmt";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (!params.equalsIgnoreCase("balaclava")) {
            return null;
        }
        if (player == null || !player.isOnline()) {
            return "false";
        }
        Player online = player.getPlayer();
        if (online == null) {
            return "false";
        }
        return BalaclavaUtils.isWearingBalaclava(online) ? "true" : "false";
    }
}
