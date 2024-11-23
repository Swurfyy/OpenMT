package nl.openminetopia.modules.staff.chat.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.police.PoliceModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("staffchat|staffc|sc")
public class StaffchatCommand extends BaseCommand {

    private final PoliceModule policeModule = OpenMinetopia.getModuleManager().getModule(PoliceModule.class);

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

            String formattedMessage = MessageConfiguration.message("staff_chat_format")
                    .replace("<player>", player.getName())
                    .replace("<world_name>", player.getWorld().getName())
                    .replace("<message>", message);

            Bukkit.getServer().getOnlinePlayers().forEach(target -> {
                PlayerManager.getInstance().getMinetopiaPlayerAsync(target, targetMinetopiaPlayer -> {
                    if (targetMinetopiaPlayer == null) return;

                    if (targetMinetopiaPlayer.isStaffchatEnabled()) ChatUtils.sendFormattedMessage(targetMinetopiaPlayer, formattedMessage);
                }, Throwable::printStackTrace);
            });
        }, Throwable::printStackTrace);
    }
}
