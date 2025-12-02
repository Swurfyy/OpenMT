package nl.openminetopia.modules.plots.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
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
import org.bukkit.entity.Player;

@CommandAlias("plot|p")
public class PlotSetWozCommand extends BaseCommand {

    @Subcommand("setwoz")
    @CommandPermission("openminetopia.plot.setwoz")
    @Description("Stel de WoZ waarde van een plot in.")
    @Syntax("<prijs>")
    public void plotSetWoz(Player player, String priceInput) {
        ProtectedRegion region = PlotUtil.getPlot(player.getLocation());
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return;

        if (region == null) {
            player.sendMessage(MessageConfiguration.component("plot_invalid_location"));
            return;
        }

        if (region.getFlag(PlotModule.PLOT_FLAG) == null) {
            player.sendMessage(MessageConfiguration.component("plot_not_valid"));
            return;
        }

        // Validate that the input is a valid integer (no dots, no commas, no letters)
        if (priceInput == null || priceInput.trim().isEmpty()) {
            ChatUtils.sendMessage(player, "<red>Ongeldige prijs. Gebruik alleen cijfers zonder punten of komma's.");
            return;
        }

        // Check for invalid characters (dots, commas, letters, etc.)
        if (!priceInput.matches("^\\d+$")) {
            ChatUtils.sendMessage(player, "<red>Ongeldige prijs. Gebruik alleen cijfers zonder punten of komma's.");
            return;
        }

        // Try to parse as long to ensure it's a valid number
        try {
            long price = Long.parseLong(priceInput);
            if (price < 0) {
                ChatUtils.sendMessage(player, "<red>De prijs moet een positief getal zijn.");
                return;
            }
        } catch (NumberFormatException e) {
            ChatUtils.sendMessage(player, "<red>Ongeldige prijs. Gebruik alleen cijfers zonder punten of komma's.");
            return;
        }

        // Set the WoZ value
        region.setFlag(PlotModule.PLOT_WOZ, priceInput);
        ChatUtils.sendMessage(player, "<gold>WoZ waarde van plot <yellow>" + region.getId() + " <gold>is ingesteld op <yellow>" + priceInput + "<gold>.");
    }
}

