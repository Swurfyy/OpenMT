package nl.openminetopia.modules.transactions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.transactions.TransactionsModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.command.CommandSender;

@CommandAlias("transaction|transactions|transactie|transacties")
@CommandPermission("openminetopia.transactions")
public class TransactionCommand extends BaseCommand {

    private final TransactionsModule transactionsModule = OpenMinetopia.getModuleManager().get(TransactionsModule.class);
    private final BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);

    @Subcommand("account")
    @Syntax("<name>")
    @CommandCompletion("@accountNames")
    public void checkTransactionHistory(CommandSender sender, String accountName) {
        BankAccountModel accountModel = bankingModule.getAccountByName(accountName);

        if (accountModel == null) {
            ChatUtils.sendMessage(sender, MessageConfiguration.message("banking_account_not_found"));
            return;
        }

        ChatUtils.sendMessage(sender, "<gray>Fetching latest transactions");
    }

}
