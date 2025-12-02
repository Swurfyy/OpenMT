package nl.openminetopia.modules.plots.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.plots.PlotModule;
import nl.openminetopia.modules.plots.utils.PlotUtil;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("plot|p")
public class PlotTransferCommand extends BaseCommand {

    @Subcommand("transfer")
    @CommandPermission("openminetopia.plot.transfer")
    @Description("Draag een plot over naar een andere speler.")
    @CommandCompletion("@players")
    @Syntax("<speler>")
    public void plotTransfer(Player player, OfflinePlayer targetPlayer) {
        ProtectedRegion region = PlotUtil.getPlot(player.getLocation());
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return;

        // Check if player is standing on a plot
        if (region == null) {
            player.sendMessage(MessageConfiguration.component("plot_invalid_location"));
            return;
        }

        // Check if it's a valid plot
        if (region.getFlag(PlotModule.PLOT_FLAG) == null) {
            player.sendMessage(MessageConfiguration.component("plot_not_valid"));
            return;
        }

        // Check if player is owner of the plot
        if (!WorldGuardUtils.isRegionOwner(region, player)) {
            ChatUtils.sendMessage(player, "<red>Sukkel, denk je nou echt dat dit werkt?!");
            return;
        }

        // Check if target player exists
        if (targetPlayer == null) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("player_not_found"));
            return;
        }

        // Check if trying to transfer to themselves
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            ChatUtils.sendMessage(player, "<red>Je kunt een plot niet aan jezelf overdragen!");
            return;
        }

        // Get all current owners and members
        var currentOwners = region.getOwners().getUniqueIds();
        var currentMembers = region.getMembers().getUniqueIds();

        // Remove all current owners
        for (UUID ownerUuid : currentOwners) {
            region.getOwners().removePlayer(ownerUuid);
        }

        // Remove all current members
        for (UUID memberUuid : currentMembers) {
            region.getMembers().removePlayer(memberUuid);
        }

        // Add target player as the new owner
        region.getOwners().addPlayer(targetPlayer.getUniqueId());

        // Get player names for messages
        String targetPlayerName = targetPlayer.getName() != null ? targetPlayer.getName() : "Onbekend";
        String playerName = player.getName();

        // Send message to the player who transferred
        ChatUtils.sendMessage(player, "Je hebt je plot <aqua>" + region.getId() + " <reset>overgedragen naar <aqua>" + targetPlayerName + "<reset>.");

        // Send message to the target player if they are online
        Player targetOnlinePlayer = targetPlayer.getPlayer();
        if (targetOnlinePlayer != null && targetOnlinePlayer.isOnline()) {
            MinetopiaPlayer targetMinetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(targetOnlinePlayer);
            if (targetMinetopiaPlayer != null) {
                targetOnlinePlayer.sendMessage(ChatUtils.format(targetMinetopiaPlayer, 
                    "Je hebt plot <aqua>" + region.getId() + " <reset>ontvangen van <aqua>" + playerName + "<reset>."));
            }
        }
    }
}
