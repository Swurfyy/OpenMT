package nl.openminetopia.modules.staff.mod.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.modules.plots.PlotModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CommandAlias("mod")
public class ModCheckPlotsCommand extends BaseCommand {

    @Subcommand("checkplots")
    @Syntax("<user>")
    @Description("Toon alle plots (plotId | world) waarvan de speler eigenaar is, over alle werelden.")
    @CommandPermission("openmt.mod.admin")
    @CommandCompletion("@players")
    public void onCheckPlots(CommandSender sender, String targetName) {
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget == null || (offlineTarget.getName() == null && !offlineTarget.hasPlayedBefore())) {
            sender.sendMessage(ChatUtils.color("<red>Speler <dark_red>" + targetName + " <red>is niet gevonden."));
            return;
        }
        UUID targetUuid = offlineTarget.getUniqueId();
        String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;

        List<String> results = new ArrayList<>();

        var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) {
            sender.sendMessage(ChatUtils.color("<red>WorldGuard container niet gevonden."));
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) continue;

            for (ProtectedRegion region : manager.getRegions().values()) {
                // Alleen plots met de PLOT_FLAG en waar target owner is
                if (region.getFlag(PlotModule.PLOT_FLAG) == null) continue;
                if (!region.getOwners().getUniqueIds().contains(targetUuid)) continue;
                results.add(region.getId() + " | " + world.getName());
            }
        }

        if (results.isEmpty()) {
            sender.sendMessage(ChatUtils.color("<gold>Geen plots gevonden voor <yellow>" + resolvedName + "<gold>."));
            return;
        }

        sender.sendMessage(ChatUtils.color("<gold>Plots van <yellow>" + resolvedName + "<gold>:"));
        for (String line : results) {
            sender.sendMessage(ChatUtils.color(" <gray>- <white>" + line));
        }
    }
}
