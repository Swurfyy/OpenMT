package nl.openminetopia.modules.plots.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.plots.PlotModule;
import nl.openminetopia.modules.plots.utils.PlotUtil;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandAlias("plotinfo|pi")
public class PlotInfoCommand extends BaseCommand {

    private static final DecimalFormat WOZ_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        WOZ_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private String formatWozAmount(long amount) {
        return "€" + WOZ_FORMAT.format(amount);
    }

    @Default
    @Description("Bekijk informatie van een plot.")
    public void plotInfo(Player player) {
        ProtectedRegion region = PlotUtil.getPlot(player.getLocation());

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer == null) {
            OpenMinetopia.getInstance().getLogger().warning("[PlotInfoCommand] MinetopiaPlayer not loaded for " + player.getName());
            player.sendMessage("§cJe speler data is nog niet geladen. Probeer het over een paar seconden opnieuw.");
            return;
        }

        if (region == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_invalid_location"));
            return;
        }

        if (region.getFlag(PlotModule.PLOT_FLAG) == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_not_valid"));
            return;
        }

        String owners = region.getOwners().getUniqueIds().stream()
                .map(ownerId -> Bukkit.getOfflinePlayer(ownerId).getName())
                .collect(Collectors.joining(", "));

        String members = region.getMembers().getUniqueIds().stream()
                .map(memberId -> Bukkit.getOfflinePlayer(memberId).getName())
                .collect(Collectors.joining(", "));

        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_header"));
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_title")
                .replace("<plot>", region.getId()));
        player.sendMessage(Component.empty());
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_owners")
                .replace("<owners>", (region.getOwners().size() > 0 ? owners : "Geen.")));
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_members")
                .replace("<members>", (region.getMembers().size() > 0 ? members : "Geen.")));

        // Add WoZ value
        String wozValue = region.getFlag(PlotModule.PLOT_WOZ);
        if (wozValue != null && !wozValue.isEmpty()) {
            try {
                long wozAmount = Long.parseLong(wozValue);
                String formattedWoz = formatWozAmount(wozAmount);
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_woz")
                        .replace("<price>", formattedWoz));
            } catch (NumberFormatException e) {
                // If parsing fails, show raw value
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_woz")
                        .replace("<price>", wozValue));
            }
        } else {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_woz")
                    .replace("<price>", "Niet ingesteld"));
        }

        if (region.getFlag(PlotModule.PLOT_DESCRIPTION) != null) {
            String description = region.getFlag(PlotModule.PLOT_DESCRIPTION);
            if (description != null && !description.isEmpty())
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_description")
                        .replace("<description>", description));
        }

        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("plot_info_footer"));
    }
}
