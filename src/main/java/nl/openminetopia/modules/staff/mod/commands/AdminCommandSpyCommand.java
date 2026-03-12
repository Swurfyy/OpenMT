package nl.openminetopia.modules.staff.mod.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

@CommandAlias("commandspy")
@CommandPermission("openmt.mod.admin")
public class AdminCommandSpyCommand extends BaseCommand {

    @Syntax("<enable/disable> <username>")
    @Description("Zet CommandSpy aan of uit voor een andere speler")
    @CommandCompletion("enable|disable @players")
    public void onAdminCommandSpy(CommandSender sender, String action, String targetName) {
        boolean enable;
        if (action.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (action.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            sender.sendMessage(ChatUtils.color("<red>Gebruik: /commandspy <enable/disable> <username>"));
            return;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget == null || (offlineTarget.getName() == null && !offlineTarget.hasPlayedBefore())) {
            sender.sendMessage(ChatUtils.color("<red>Speler <dark_red>" + targetName + " <red>is niet gevonden."));
            return;
        }

        CompletableFuture<MinetopiaPlayer> future = PlayerManager.getInstance().getMinetopiaPlayer(offlineTarget);
        future.whenComplete((minetopiaPlayer, throwable) -> {
            if (throwable != null || minetopiaPlayer == null) {
                sender.sendMessage(ChatUtils.color("<red>De data van speler <dark_red>" + targetName + " <red>kon niet worden geladen."));
                return;
            }

            minetopiaPlayer.setCommandSpyEnabled(enable);

            String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            sender.sendMessage(ChatUtils.color("<gold>Je hebt <yellow>CommandSpy <gold>voor <yellow>"
                    + resolvedName
                    + " <gold>" + (enable ? "aangezet" : "uitgezet") + "."));
        });
    }
}

