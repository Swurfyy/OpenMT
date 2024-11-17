package nl.openminetopia.modules.prefix.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.prefix.menu.PrefixMenu;
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

        // Open het prefix menu
        OfflinePlayer finalTarget = target;
        PlayerManager.getInstance().getMinetopiaPlayerAsync(target, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;
            new PrefixMenu(player, finalTarget, minetopiaPlayer).open(player);
        }, Throwable::printStackTrace);
    }
}
