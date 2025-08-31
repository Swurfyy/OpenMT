package nl.openminetopia.modules.currencies.tasks;

import com.jazzkuh.modulemanager.spigot.handlers.tasks.TaskInfo;
import lombok.RequiredArgsConstructor;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.currencies.CurrencyModule;
import nl.openminetopia.modules.currencies.models.CurrencyModel;
import nl.openminetopia.modules.currencies.objects.RegisteredCurrency;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

@RequiredArgsConstructor
@TaskInfo(repeating = true, delay = 20L, period = 20L * 30L) // every 30 seconds
public class CurrencyTask extends BukkitRunnable {

    private final CurrencyModule currencyModule;

    @Override
    public void run() {
        PlayerManager playerManager = PlayerManager.getInstance();

        for (MinetopiaPlayer player : playerManager.getOnlinePlayers().values()) {
            Player bukkitPlayer = player.getBukkit().getPlayer();
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) continue;
            if (!currencyModule.getCurrencyModels().containsKey(player.getUuid())) continue;
            List<CurrencyModel> currencies = currencyModule.getCurrencyModels().get(player.getUuid());
            if (currencies.isEmpty()) continue;

            for (CurrencyModel currency : currencies) {
                RegisteredCurrency configCurrency = currency.configModel();
                if (configCurrency == null) {
                    if (currencyModule.getConfiguration().isIgnoreUnused()) continue;
                    OpenMinetopia.getInstance().getLogger().warning("Currency config not found for currency " + currency.getName());
                    OpenMinetopia.getInstance().getLogger().warning("You should consider removing the currency model from the database (if it is not used) by using /currency purge-unused or enabling ignore-unused in currencies.yml");
                    continue;
                }
                if (!configCurrency.isAutomatic()) continue;

                if (player.getPlaytime() - currency.getLastReward() >= configCurrency.getInterval() * 1000L) {
                    currency.setLastReward(player.getPlaytime());

                    String message = MessageConfiguration.message("currency_automatic_reward")
                            .replaceAll("<amount>", String.valueOf(configCurrency.getAmount()))
                            .replaceAll("<display_name>", configCurrency.getDisplayName());

                    ChatUtils.sendFormattedMessage(player, message);
                    currency.setBalance(currency.getBalance() + configCurrency.getAmount());
                    StormDatabase.getInstance().saveStormModel(currency);
                }
            }
        }
    }
}
