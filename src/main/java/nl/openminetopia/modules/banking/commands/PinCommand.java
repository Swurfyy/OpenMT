package nl.openminetopia.modules.banking.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.banking.models.PinRequestModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@CommandAlias("pin|pinterminal")
public class PinCommand extends BaseCommand {

    @Subcommand("set")
    @CommandPermission("openminetopia.pin.set")
    @CommandCompletion("@players")
    @Syntax("<speler> <bedrag> [account-id]")
    public void setPinRequest(Player seller, String playerName, double amount, @Optional String accountIdString) {
        if (amount <= 0) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_invalid_amount"));
            return;
        }

        Player buyer = Bukkit.getPlayer(playerName);
        if (buyer == null) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("no_player_provided"));
            return;
        }

        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        List<BankAccountModel> buyerAccounts = bankingModule.getAccountsFromPlayer(buyer.getUniqueId());

        if (buyerAccounts.isEmpty()) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_buyer_no_accounts")
                    .replace("<player>", buyer.getName()));
            return;
        }

        final UUID buyerAccountId;

        // If buyer has multiple accounts, they must specify which one
        if (buyerAccounts.size() > 1) {
            if (accountIdString == null || accountIdString.isEmpty()) {
                ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_multiple_accounts_specify_id")
                        .replace("<player>", buyer.getName()));
                return;
            }

            UUID tempAccountId;
            try {
                tempAccountId = UUID.fromString(accountIdString);
            } catch (IllegalArgumentException e) {
                ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_invalid_account_id"));
                return;
            }

            // Verify the buyer owns this account
            UUID finalTempAccountId = tempAccountId;
            boolean hasAccount = buyerAccounts.stream()
                    .anyMatch(account -> account.getUniqueId().equals(finalTempAccountId));

            if (!hasAccount) {
                ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_buyer_not_own_account")
                        .replace("<player>", buyer.getName()));
                return;
            }
            
            buyerAccountId = tempAccountId;
        } else {
            // Only one account, use it automatically
            buyerAccountId = buyerAccounts.get(0).getUniqueId();
        }

        // Create the pin request
        PinRequestModel request = new PinRequestModel(seller.getUniqueId(), buyer.getUniqueId(), amount, buyerAccountId);
        bankingModule.addPinRequest(seller.getUniqueId(), request);

        ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_request_created")
                .replace("<player>", buyer.getName())
                .replace("<amount>", bankingModule.format(amount)));

        ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_request_received")
                .replace("<player>", seller.getName())
                .replace("<amount>", bankingModule.format(amount)));
    }

    @Subcommand("cancel")
    @CommandPermission("openminetopia.pin.cancel")
    public void cancelPinRequest(Player seller) {
        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);

        if (!bankingModule.hasPinRequest(seller.getUniqueId())) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_no_active_request"));
            return;
        }

        bankingModule.removePinRequest(seller.getUniqueId());
        ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_request_cancelled"));
    }
}

