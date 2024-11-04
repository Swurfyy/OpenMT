package nl.openminetopia.modules.staff.chat.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("staffchat|staffc|sc")
public class StaffchatCommand extends BaseCommand {

    @Default
    @CommandPermission("openminetopia.staffchat")
    public void staffchat(Player player, @Optional String message) {
        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (message == null) {
                minetopiaPlayer.setStaffchatEnabled(!minetopiaPlayer.isStaffchatEnabled());
                player.sendMessage(ChatUtils.color("<gold>Je hebt staffchat nu <yellow>" + (minetopiaPlayer.isStaffchatEnabled() ? "aangezet" : "uitgezet")));
                return;
            }

            for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("openminetopia.staffchat")) {
                    onlinePlayer.sendMessage(ChatUtils.format(minetopiaPlayer, "<dark_gray>[<gold><b>Staff</b><dark_gray>] <dark_gray>(<red><b>" + minetopiaPlayer.getWorld().getName() + "</b><dark_gray>) <green>" + player.getName() + "<white>: " + message));
                }
            }
        }, Throwable::printStackTrace);
    }
}
