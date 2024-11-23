package nl.openminetopia.modules.plots.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
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
public class PlotMembersCommand extends BaseCommand {

    @Subcommand("addmember")
    @Description("Voegt een speler toe aan een plot.")
    @Syntax("<speler>")
    public void addPlotMember(Player player, OfflinePlayer offlinePlayer) {
        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(player.getLocation(), priority -> priority >= 0);

        if (offlinePlayer == null) {
            player.sendMessage(MessageConfiguration.component("player_not_found"));
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (region == null) {
                player.sendMessage(MessageConfiguration.component("plot_invalid_location"));
                return;
            }

            if (region.getFlag(OpenMinetopia.PLOT_FLAG) == null) {
                player.sendMessage(MessageConfiguration.component("plot_not_valid"));
                return;
            }

            if (!region.getOwners().contains(player.getUniqueId()) && !player.hasPermission("openminetopia.plot.removemember")) {
                player.sendMessage(MessageConfiguration.component("plot_not_owner"));
                return;
            }

            if (region.getMembers().contains(offlinePlayer.getUniqueId())) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_member_already"));
                return;
            }

            region.getMembers().addPlayer(offlinePlayer.getUniqueId());
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_member_added"));
        }, Throwable::printStackTrace);
    }

    @Subcommand("removemember")
    @Description("Verwijderd een speler van een plot.")
    @Syntax("<speler>")
    public void removePlotMember(Player player, OfflinePlayer offlinePlayer) {
        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(player.getLocation(), priority -> priority >= 0);
        PlayerProfile profile = offlinePlayer.getPlayerProfile();

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (region == null) {
                player.sendMessage(MessageConfiguration.component("plot_invalid_location"));
                return;
            }

            if (region.getFlag(OpenMinetopia.PLOT_FLAG) == null) {
                player.sendMessage(MessageConfiguration.component("plot_not_valid"));
                return;
            }

            if (!region.getOwners().contains(player.getUniqueId()) && !player.hasPermission("openminetopia.plot.removemember")) {
                player.sendMessage(MessageConfiguration.component("plot_not_owner"));
                return;
            }

            if (!region.getMembers().contains(profile.getId())) {
                player.sendMessage(MessageConfiguration.component("plot_member_not_added"));
                return;
            }

            region.getMembers().removePlayer(profile.getId());
            player.sendMessage(ChatUtils.format(minetopiaPlayer, "<dark_aqua>Je hebt <aqua>" + profile.getName() + " <dark_aqua>verwijderd van dit plot."));
        }, Throwable::printStackTrace);
    }
}