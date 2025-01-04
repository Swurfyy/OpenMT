package nl.openminetopia.modules.plots.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.plots.PlotModule;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.entity.Player;

@CommandAlias("plot|p")
public class PlotDescriptionCommand extends BaseCommand {

    @Subcommand("description")
    @CommandPermission("openminetopia.plot.description")
    @Syntax("<beschrijving>")
    @Description("Zet een beschrijving van een plot.")
    public void plotDescription(Player player, String description) {
        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(player.getLocation(), priority -> priority >= 0);

        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
            if (minetopiaPlayer == null) return;

            if (region == null) {
                player.sendMessage(MessageConfiguration.component("plot_invalid_location"));
                return;
            }

            if (region.getFlag(OpenMinetopia.PLOT_FLAG) == null) {
                player.sendMessage(MessageConfiguration.component("plot_not_valid"));
                return;
            }

            if (description.isBlank() || description.equalsIgnoreCase("remove") ||
                    description.equalsIgnoreCase("delete") || description.equalsIgnoreCase("null")) {
                region.setFlag(OpenMinetopia.PLOT_DESCRIPTION, null);
                ChatUtils.sendMessage(player, MessageConfiguration.message("plot_description_removed")
                        .replace("<plot_id>", region.getId()));
                return;
            }

            region.setFlag(OpenMinetopia.PLOT_DESCRIPTION, description);
            ChatUtils.sendMessage(player, MessageConfiguration.message("plot_description_updated")
                    .replace("<description>", description)
                    .replace("<plot_id>", region.getId()));
        });
    }
}
