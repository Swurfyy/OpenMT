package nl.openminetopia.modules.fitness.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("fitness|fitheid")
public class FitnessBoosterCommand extends BaseCommand {

    @Subcommand("booster")
    @CommandPermission("openminetopia.fitness.booster")
    @CommandCompletion("@players")
    public void booster(Player player, OfflinePlayer offlinePlayer, int amount, @Optional Integer expiresAt) {

        if (offlinePlayer.getPlayer() == null) return;

        PlayerManager.getInstance().getMinetopiaPlayerAsync(offlinePlayer.getPlayer(), minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            int expiry = expiresAt == null || expiresAt <= 0 ? -1 : expiresAt;
            long expiresAtMillis = expiry == -1 ? -1 : System.currentTimeMillis() + (expiry * 1000L);

            minetopiaPlayer.getFitness().addBooster(amount, expiresAtMillis);

            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("fitness_booster_added_to")
                    .replace("<playername>", (offlinePlayer.getName() == null ? "null" : offlinePlayer.getName())));

        }, Throwable::printStackTrace);
    }
}
