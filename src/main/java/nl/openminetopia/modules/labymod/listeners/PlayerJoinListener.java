package nl.openminetopia.modules.labymod.listeners;

import net.labymod.serverapi.server.bukkit.LabyModPlayer;
import net.labymod.serverapi.server.bukkit.event.LabyModPlayerJoinEvent;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.labymod.LabymodModule;
import nl.openminetopia.modules.labymod.configuration.LabymodConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public class PlayerJoinListener implements Listener {

	LabymodConfiguration configuration = OpenMinetopia.getModuleManager().get(LabymodModule.class).getConfiguration();

	@EventHandler
	public void onJoin(LabyModPlayerJoinEvent event) {

		LabyModPlayer labyPlayer = event.labyModPlayer();

		boolean EconomyEnabled = configuration.isEconomyDisplayEnabled();
		String EconomyUrl = configuration.getEconomyIconUrl();

		BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
		BankAccountModel accountModel = bankingModule.getAccountById(labyPlayer.getUniqueId());

		/* Labymod Economy */
		if (EconomyEnabled && accountModel != null) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(OpenMinetopia.getInstance(), () -> {

				double balance = accountModel.getBalance();

				labyPlayer.updateBankEconomy(economy -> {
					economy.visible(true);
					economy.balance(balance);

					if (!Objects.equals(EconomyUrl, "")) {
						economy.iconUrl(EconomyUrl);
					}

				});

			}, 0, 20);
		}
	}

}
