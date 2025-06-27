package nl.openminetopia.modules.prefix.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.prefix.menus.PrefixMenu;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("prefix")
public class PrefixMenuCommand extends BaseCommand {

    @Subcommand("menu")
    @Description("Open het prefix menu.")
    @CommandCompletion("@players")
    public void prefixMenu(Player player, @Optional OfflinePlayer target) {
        if (target == null) {
            target = player;
        }

        if (target != null && !player.hasPermission("openminetopia.prefix.menu.others")) {
            ChatUtils.sendMessage(player, "<red>Je hebt geen toestemming om het prefix menu van anderen te openen.");
            return;
        }

        // Open het prefix menu
        OfflinePlayer finalTarget = target;
        PlayerManager.getInstance().getMinetopiaPlayer(target).whenComplete((minetopiaPlayer, throwable1) -> {
            if (minetopiaPlayer == null) return;
            new PrefixMenu(player, finalTarget, minetopiaPlayer).open(player);
        });
    }
}
