package nl.openminetopia.modules.plots.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.plots.PlotModule;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("plot|p")
public class PlotOwnersCommand extends BaseCommand {

    @Subcommand("addowner")
    @Description("Voegt een speler toe aan een plot.")
    @CommandPermission("openminetopia.plot.addowner")
    @Syntax("<speler>")
    public void addPlotOwner(Player player, OfflinePlayer offlinePlayer) {
        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(player.getLocation(), priority -> priority >= 0);

        if (offlinePlayer == null) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("player_not_found"));
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (region == null) {
                player.sendMessage(ChatUtils.format(minetopiaPlayer, "<red>Je staat niet op een geldig plot."));
                return;
            }

            if (region.getFlag(OpenMinetopia.PLOT_FLAG) == null) {
                player.sendMessage(ChatUtils.format(minetopiaPlayer, "<red>Dit is geen geldig plot."));
                return;
            }

            if (region.getOwners().contains(offlinePlayer.getUniqueId())) {
                player.sendMessage(ChatUtils.format(minetopiaPlayer, "<red>" + offlinePlayer.getName() + " is al een eigenaar van dit plot."));
                return;
            }

            region.getOwners().addPlayer(offlinePlayer.getUniqueId());
            player.sendMessage(ChatUtils.format(minetopiaPlayer, "<dark_aqua>Je hebt <aqua>" + offlinePlayer.getName() + " <dark_aqua>toegevoegd aan het plot."));
        }, Throwable::printStackTrace);
    }

    @Subcommand("removeowner")
    @Description("Verwijderd een speler van een plot.")
    @CommandPermission("openminetopia.plot.removeowner")
    @Syntax("<speler>")
    public void removePlotOwner(Player player, OfflinePlayer offlinePlayer) {
        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(player.getLocation(), priority -> priority >= 0);
        PlayerProfile profile = offlinePlayer.getPlayerProfile();

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (region == null) {
                player.sendMessage(ChatUtils.format(minetopiaPlayer,"<red>Je staat niet op een geldig plot."));
                return;
            }

            if (region.getFlag(OpenMinetopia.PLOT_FLAG) == null) {
                player.sendMessage(ChatUtils.format(minetopiaPlayer,"<red>Dit is geen geldig plot."));
                return;
            }

            if (!region.getOwners().contains(profile.getId())) {
                player.sendMessage(ChatUtils.format(minetopiaPlayer,"<red>" + profile.getName() + " is geen eigenaar van dit plot."));
                return;
            }

            region.getOwners().removePlayer(profile.getId());
            player.sendMessage(ChatUtils.format(minetopiaPlayer,"<dark_aqua>Je hebt <aqua>" + profile.getName() + " <dark_aqua>verwijderd van dit plot."));
        }, Throwable::printStackTrace);
    }
}
