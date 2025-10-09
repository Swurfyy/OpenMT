package nl.openminetopia.modules.banking.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("accounts|account|rekening")
public class BankingMyAccountsCommand extends BaseCommand {

    @Subcommand("mijn|my")
    @CommandPermission("openminetopia.banking.myaccounts")
    public void myAccounts(Player player) {
        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        List<BankAccountModel> accounts = bankingModule.getAccountsFromPlayer(player.getUniqueId());

        if (accounts.isEmpty()) {
            ChatUtils.sendMessage(player, "<red>Je hebt geen bankrekeningen.");
            return;
        }

        ChatUtils.sendMessage(player, "<gold><bold>Jouw Bankrekeningen:");
        ChatUtils.sendMessage(player, "");
        
        for (BankAccountModel account : accounts) {
            String accountInfo = "<yellow>▪ <gold>" + account.getName() + 
                " <gray>(<yellow>" + account.getType().getName() + "<gray>)";
            ChatUtils.sendMessage(player, accountInfo);
            
            ChatUtils.sendMessage(player, "  <gray>ID: <white>" + account.getUniqueId().toString());
            ChatUtils.sendMessage(player, "  <gray>Saldo: <green>" + bankingModule.format(account.getBalance()));
            ChatUtils.sendMessage(player, "");
        }
        
        if (accounts.size() > 1) {
            ChatUtils.sendMessage(player, "<gray><i>Tip: Kopieer het Account ID voor betalingsverzoeken");
        }
    }
}

