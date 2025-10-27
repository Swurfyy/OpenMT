package nl.openminetopia.modules.police.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.police.handcuff.HandcuffManager;
import nl.openminetopia.modules.police.handcuff.objects.HandcuffedPlayer;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

@CommandAlias("handboei|handcuff")
public class HandcuffCommand extends BaseCommand {

    @Default
    @Syntax("<player>")
    @Description("Boei een speler of bevrijd een geboeide speler")
    @CommandPermission("openminetopia.handcuff")
    @CommandCompletion("@players")
    public void handcuff(Player player, OnlinePlayer onlineTarget) {
        Player target = onlineTarget.getPlayer();
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        MinetopiaPlayer targetMinetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(target);
        
        if (minetopiaPlayer == null || targetMinetopiaPlayer == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
            return;
        }

        if (player == target) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_handcuff_self"));
            return;
        }

        HandcuffManager handcuffManager = HandcuffManager.getInstance();

        // Check if target is already handcuffed
        if (handcuffManager.isHandcuffed(target)) {
            HandcuffedPlayer handcuffedPlayer = handcuffManager.getHandcuffedPlayer(target);
            if (handcuffedPlayer != null) {
                handcuffManager.release(handcuffedPlayer);
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_handcuff_released")
                        .replace("<player>", target.getName()));
                return;
            }
        }

        // Check if source is already handcuffing someone
        if (handcuffManager.isHandcuffing(player)) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_handcuff_already_handcuffing"));
            return;
        }

        // Handcuff the target
        handcuffManager.handcuff(targetMinetopiaPlayer, minetopiaPlayer);
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_handcuff_applied")
                .replace("<player>", target.getName()));
    }
}
