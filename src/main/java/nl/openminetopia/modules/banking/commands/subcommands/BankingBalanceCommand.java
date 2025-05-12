package nl.openminetopia.modules.banking.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.transactions.TransactionsModule;
import nl.openminetopia.modules.transactions.enums.TransactionType;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("accounts|account|rekening")
public class BankingBalanceCommand extends BaseCommand {

    @Subcommand("setbalance")
    @CommandCompletion("@accountNames")
    @CommandPermission("openminetopia.banking.setbalance")
    public void setBalance(CommandSender sender, String accountName, double balance) {
        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        bankingModule.getAccountByNameAsync(accountName).whenComplete((accountModel, throwable) -> {
            if (accountModel == null) {
                sender.sendMessage(MessageConfiguration.component("banking_account_not_found"));
                return;
            }

            accountModel.setBalance(balance);
            accountModel.save();

            UUID executorUuid = ((sender instanceof Player executor) ? executor.getUniqueId() : new UUID(0, 0));
            String username = ((sender instanceof Player executor) ? executor.getName() : "Console");

            TransactionsModule transactionsModule = OpenMinetopia.getModuleManager().get(TransactionsModule.class);
            transactionsModule.createTransactionLog(System.currentTimeMillis(), executorUuid, username, TransactionType.SET, balance, accountModel.getUniqueId(), "Set via '/account setbalance'");

            sender.sendMessage(ChatUtils.color("<gold>De balans van <red>" + accountModel.getName() + " <gold>is nu ingesteld op <red>" + bankingModule.format(balance) + "<gold>."));
        });
    }
}