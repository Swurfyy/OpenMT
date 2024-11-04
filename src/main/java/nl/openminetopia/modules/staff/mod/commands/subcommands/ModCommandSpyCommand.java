package nl.openminetopia.modules.staff.mod.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.chat.utils.SpyUtils;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.Optional;

@CommandAlias("mod")
public class ModCommandSpyCommand extends BaseCommand {

    @Subcommand("commandspy")
    @CommandPermission("openminetopia.mod.commandspy")
    @Description("Enables or disables CommandSpy")
    public void commandSpy(Player player) {

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            minetopiaPlayer.setCommandSpyEnabled(!minetopiaPlayer.isCommandSpyEnabled());
            player.sendMessage(ChatUtils.color("<gold>Je hebt <yellow>CommandSpy <gold>" + (minetopiaPlayer.isCommandSpyEnabled() ? "aangezet" : "uitgezet") + "!"));
        }, Throwable::printStackTrace);
    }
}