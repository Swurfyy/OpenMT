package nl.openminetopia.modules.staff.mod.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

@CommandAlias("mod")
@CommandPermission("openmt.mod.admin")
public class AdminChatSpyCommand extends BaseCommand {

    @Subcommand("enablechatspy")
    @Syntax("<username>")
    @Description("Zet ChatSpy aan voor een andere speler")
    @CommandCompletion("@players")
    public void onEnableChatSpy(CommandSender sender, String targetName) {
        handleChatSpyToggle(sender, targetName, true);
    }

    @Subcommand("disablechatspy")
    @Syntax("<username>")
    @Description("Zet ChatSpy uit voor een andere speler")
    @CommandCompletion("@players")
    public void onDisableChatSpy(CommandSender sender, String targetName) {
        handleChatSpyToggle(sender, targetName, false);
    }

    private void handleChatSpyToggle(CommandSender sender, String targetName, boolean enable) {
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

            minetopiaPlayer.setChatSpyEnabled(enable);

            String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            sender.sendMessage(ChatUtils.color("<gold>Je hebt <yellow>ChatSpy <gold>voor <yellow>"
                    + resolvedName
                    + " <gold>" + (enable ? "aangezet" : "uitgezet") + "."));        });
    }
}

