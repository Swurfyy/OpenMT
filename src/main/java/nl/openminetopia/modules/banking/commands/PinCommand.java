package nl.openminetopia.modules.banking.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.enums.AccountType;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.banking.models.PinRequestModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("pin|pinterminal")
public class PinCommand extends BaseCommand {

    @Subcommand("set")
    @CommandPermission("openminetopia.pin.set")
    @CommandCompletion("@players")
    @Syntax("<speler> <bedrag>")
    public void setPinRequest(Player seller, String playerName, double amount) {
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
        
        // Check if seller has a COMPANY account
        List<BankAccountModel> sellerAccounts = bankingModule.getAccountsFromPlayer(seller.getUniqueId());
        BankAccountModel sellerCompanyAccount = sellerAccounts.stream()
                .filter(account -> account.getType() == AccountType.COMPANY)
                .findFirst()
                .orElse(null);
        
        if (sellerCompanyAccount == null) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_seller_needs_company_account"));
            return;
        }
        
        // Find buyer's PRIVATE account
        List<BankAccountModel> buyerAccounts = bankingModule.getAccountsFromPlayer(buyer.getUniqueId());
        BankAccountModel buyerPrivateAccount = buyerAccounts.stream()
                .filter(account -> account.getType() == AccountType.PRIVATE)
                .findFirst()
                .orElse(null);

        if (buyerPrivateAccount == null) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_buyer_no_private_account")
                    .replace("<player>", buyer.getName()));
            return;
        }

        // Create the pin request
        PinRequestModel request = new PinRequestModel(seller.getUniqueId(), buyer.getUniqueId(), amount, buyerPrivateAccount.getUniqueId());
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

