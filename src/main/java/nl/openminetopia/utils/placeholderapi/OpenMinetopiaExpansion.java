package nl.openminetopia.utils.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class OpenMinetopiaExpansion extends PlaceholderExpansion {

    private final OpenMinetopia plugin = OpenMinetopia.getInstance();

    @Override
    public @NotNull String getIdentifier() {
        return "OpenMinetopia";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            String result = switch (params.toLowerCase()) {
                case "prefix" -> minetopiaPlayer.getActivePrefix().getPrefix();
                case "level" -> String.valueOf(minetopiaPlayer.getLevel());
                case "city" -> minetopiaPlayer.getPlace().getName();
                case "world" -> minetopiaPlayer.getWorld().getName();
                case "temperature" -> String.valueOf(minetopiaPlayer.getPlace().getTemperature());
                case "balance", "balance_formatted" -> {
                    BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);
                    BankAccountModel accountModel = bankingModule.getAccountById(player.getUniqueId());
                    if (accountModel != null) {
                        yield params.equalsIgnoreCase("balance_formatted")
                                ? bankingModule.format(accountModel.getBalance())
                                : String.valueOf(accountModel.getBalance());
                    }
                    yield null;
                }
                case "fitness" -> String.valueOf(minetopiaPlayer.getFitness().getTotalFitness());
                case "max_fitness" -> String.valueOf(OpenMinetopia.getFitnessConfiguration().getMaxFitnessLevel());
                default -> null;
            };

            // Send the result asynchronously to the player if needed
            if (player.isOnline() && player.getPlayer() != null && result != null) {
                player.getPlayer().sendMessage(result);
            }
        }, Throwable::printStackTrace);

        // Return a placeholder or default value immediately
        return "Loading...";
    }
}
