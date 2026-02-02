package nl.openminetopia.modules.police.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.police.chase.ChaseManager;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

@CommandAlias("volg")
public class ChaseCommand extends BaseCommand {

    @Default
    @Syntax("<player>")
    @Description("Start een achtervolging of stop een actieve achtervolging")
    @CommandPermission("openminetopia.chase")
    @CommandCompletion("@players")
    public void chase(Player player, OnlinePlayer onlineTarget) {
        Player target = onlineTarget.getPlayer();
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        
        if (minetopiaPlayer == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
            return;
        }

        if (player == target) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_chase_self"));
            return;
        }

        ChaseManager chaseManager = ChaseManager.getInstance();

        // Check if target is already being chased
        if (chaseManager.isBeingChased(target)) {
            // Stop the chase
            chaseManager.stopChase(target.getUniqueId());
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_chase_stopped_by_agent"));
            return;
        }

        // Check if agent is within 20 blocks of target
        double distance = player.getLocation().distance(target.getLocation());
        if (distance > 20) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_chase_too_far"));
            return;
        }

        // Start the chase
        chaseManager.startChase(player, target);
        
        // Send chase message to target (using sendMessage for direct MiniMessage parsing)
        ChatUtils.sendMessage(target, MessageConfiguration.message("police_chase_started"));
        
        // Send confirmation to agent
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_chase_started_by_agent")
                .replace("<player>", target.getName()));
    }
}
