package nl.openminetopia.modules.currencies.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.currencies.CurrencyModule;
import nl.openminetopia.modules.currencies.models.CurrencyModel;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@CommandAlias("currency|currencies")
public class CurrencyCommand extends BaseCommand {

    private boolean awaitingConfirmation = false;

    @Subcommand("purge-unused")
    @CommandPermission("openminetopia.currency.purge")
    public void purgeOld(CommandSender sender) {
        if (awaitingConfirmation) {
            sender.sendMessage(ChatUtils.color("<red>Purging all unused currencies... This may take a while.</red>"));
            CurrencyModule currencyModule = OpenMinetopia.getModuleManager().get(CurrencyModule.class);

            currencyModule.getAllCurrencies().whenComplete((currencies, throwable) -> {
                if (throwable != null) {
                    Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                        sender.sendMessage(ChatUtils.color("<red>An error occurred while purging currencies: " + throwable.getMessage() + "</red>"));
                    });
                    awaitingConfirmation = false;
                    return;
                }

                List<CurrencyModel> toRemove = new ArrayList<>();
                for (CurrencyModel currency : new ArrayList<>(currencies)) {
                    if (currencyModule.getCurrencies().contains(currency.configModel())) continue;

                    try {
                        StormDatabase.getInstance().getStorm().delete(currency);
                    } catch (SQLException e) {
                        OpenMinetopia.getInstance().getLogger().warning("Error while deleting currency: " + currency.getName() + " for UUID: " + currency.getUniqueId());
                    }

                    currencyModule.getCurrencyModels().values().forEach(list ->
                            list.removeIf(model -> model.getName().equalsIgnoreCase(currency.getName()) &&
                                    model.getUniqueId().equals(currency.getUniqueId()))
                    );

                    toRemove.add(currency);
                }
                currencies.removeAll(toRemove);

                int removedCount = toRemove.size();
                Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () ->
                        sender.sendMessage(ChatUtils.color("<green>Successfully purged " + removedCount + " unused currencies from the database.</green>"))
                );

                awaitingConfirmation = false;
            });
            return;
        }

        sender.sendMessage(ChatUtils.color("<red>Weet je zeker dat je alle ongebruikte currencies uit de database wilt verwijderen? " +
                "<dark_red>Deze actie kan niet ongedaan worden gemaakt.</dark_red> " +
                "Typ het command binnen de 20 seconden opnieuw om te bevestigen.</red>"));
        awaitingConfirmation = true;

        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> awaitingConfirmation = false, 20 * 20L);
    }
}