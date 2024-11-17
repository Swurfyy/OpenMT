package nl.openminetopia.modules.staff.admintool.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import com.destroystokyo.paper.profile.PlayerProfile;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.staff.admintool.menus.AdminToolMenu;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("admintool")
public class AdminToolOpenCommand extends BaseCommand {

    @Subcommand("open")
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.admintool.open")
    public void open(Player player, OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            player.sendMessage(ChatUtils.color("<red>Deze speler heeft nog nooit gespeeld."));
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayerAsync(offlinePlayer, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;
            if (!offlinePlayer.isOnline()) minetopiaPlayer.getFitness().getRunnable().forceRun();
            Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () -> new AdminToolMenu(player, offlinePlayer, minetopiaPlayer).open(player));
        }, Throwable::printStackTrace);
    }
}
