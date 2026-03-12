package nl.openminetopia.modules.staff.mod.commands.subcommands;

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
public class ModCheckCommand extends BaseCommand {

    @Subcommand("check")
    @Syntax("<user>")
    @Description("Toon welke staff-toggles voor een speler aan staan")
    @CommandPermission("openmt.mod.admin")
    @CommandCompletion("@players")
    public void onCheck(CommandSender sender, String targetName) {
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

            boolean staffchat = minetopiaPlayer.isStaffchatEnabled();
            boolean chatSpy = minetopiaPlayer.isChatSpyEnabled();
            boolean commandSpy = minetopiaPlayer.isCommandSpyEnabled();

            String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;

            sender.sendMessage(ChatUtils.color("<gold>Overzicht voor <yellow>" + resolvedName + "<gold>:"));
            sender.sendMessage(ChatUtils.color(" <gray>- <yellow>Staffchat<gold>: " + (staffchat ? "<green>AAN" : "<red>UIT")));
            sender.sendMessage(ChatUtils.color(" <gray>- <yellow>ChatSpy<gold>: " + (chatSpy ? "<green>AAN" : "<red>UIT")));
            sender.sendMessage(ChatUtils.color(" <gray>- <yellow>CommandSpy<gold>: " + (commandSpy ? "<green>AAN" : "<red>UIT")));
        });
    }
}

