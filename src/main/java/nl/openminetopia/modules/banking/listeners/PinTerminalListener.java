package nl.openminetopia.modules.banking.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.configuration.BankingConfiguration;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.banking.models.PinRequestModel;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.transactions.TransactionsModule;
import nl.openminetopia.modules.transactions.enums.TransactionType;
import nl.openminetopia.modules.transactions.events.TransactionUpdateEvent;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.events.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class PinTerminalListener implements Listener {

    @EventHandler
    public void onPinTerminalInteraction(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Material material = block.getType();

        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        BankingConfiguration bankingConfiguration = bankingModule.getConfiguration();

        if (!bankingConfiguration.getPinTerminalMaterials().contains(material)) return;

        event.setCancelled(true);

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if seller is activating the terminal
        if (bankingModule.hasPinRequest(player.getUniqueId())) {
            PinRequestModel request = bankingModule.getPinRequest(player.getUniqueId());

            if (request.hasTerminalLocation()) {
                ChatUtils.sendMessage(player, MessageConfiguration.message("pin_already_activated"));
                return;
            }

            // Activate the terminal
            request.setTerminalLocation(block.getLocation());
            ChatUtils.sendMessage(player, MessageConfiguration.message("pin_terminal_activated")
                    .replace("<player>", Bukkit.getOfflinePlayer(request.getBuyerUuid()).getName())
                    .replace("<amount>", bankingModule.format(request.getAmount())));

            // Notify buyer
            Player buyer = Bukkit.getPlayer(request.getBuyerUuid());
            if (buyer != null) {
                ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_terminal_ready")
                        .replace("<player>", player.getName())
                        .replace("<amount>", bankingModule.format(request.getAmount())));
            }
            return;
        }

        // Check if buyer is paying
        if (!isBankCard(itemInHand, bankingConfiguration)) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("pin_not_holding_card"));
            return;
        }

        // Find active request for this buyer at this location
        PinRequestModel activeRequest = bankingModule.findPinRequestForBuyerAtLocation(
                player.getUniqueId(), block.getLocation());

        if (activeRequest == null) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("pin_no_request_at_terminal"));
            return;
        }

        if (activeRequest.isExpired()) {
            UUID sellerUuid = activeRequest.getSellerUuid();
            bankingModule.removePinRequest(sellerUuid);
            ChatUtils.sendMessage(player, MessageConfiguration.message("pin_request_expired"));
            return;
        }

        // Process the payment
        processPayment(player, activeRequest, bankingModule);
    }

    @EventHandler
    public void onPinTerminalBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();

        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        BankingConfiguration bankingConfiguration = bankingModule.getConfiguration();

        if (!bankingConfiguration.getPinTerminalMaterials().contains(material)) return;

        // Check if this terminal has an active request
        Location location = block.getLocation();
        PinRequestModel request = bankingModule.findPinRequestAtLocation(location);

        if (request != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            ChatUtils.sendMessage(player, MessageConfiguration.message("pin_terminal_in_use"));
        }
    }

    private boolean isBankCard(ItemStack item, BankingConfiguration config) {
        if (item == null || item.getType().isAir()) return false;

        List<ItemStack> bankCards = config.getBankCardItems();
        if (bankCards == null || bankCards.isEmpty()) {
            // Fallback: any PAPER item is considered a bank card
            return item.getType() == Material.PAPER;
        }

        // Check if the item matches any configured bank card
        for (ItemStack bankCard : bankCards) {
            if (bankCard == null) continue;
            if (item.getType() != bankCard.getType()) continue;

            if (!item.hasItemMeta() && !bankCard.hasItemMeta()) return true;
            if (item.hasItemMeta() != bankCard.hasItemMeta()) continue;

            var itemMeta = item.getItemMeta();
            var cardMeta = bankCard.getItemMeta();

            if (itemMeta == null || cardMeta == null) continue;

            // Check custom model data
            @SuppressWarnings("deprecation")
            boolean cardHasModel = cardMeta.hasCustomModelData();
            if (cardHasModel) {
                @SuppressWarnings("deprecation")
                boolean itemHasModel = itemMeta.hasCustomModelData();
                if (!itemHasModel) continue;
                
                @SuppressWarnings("deprecation")
                int itemModelData = itemMeta.getCustomModelData();
                @SuppressWarnings("deprecation")
                int cardModelData = cardMeta.getCustomModelData();
                
                if (itemModelData != cardModelData) continue;
            }

            // Check item model (1.21.4+)
            if (cardMeta.hasItemModel() && itemMeta.hasItemModel()) {
                if (!itemMeta.getItemModel().equals(cardMeta.getItemModel())) continue;
            }

            return true;
        }

        return false;
    }

    private void processPayment(Player buyer, PinRequestModel request, BankingModule bankingModule) {
        BankAccountModel buyerAccount = bankingModule.getAccountById(request.getBuyerAccountId());
        if (buyerAccount == null) {
            ChatUtils.sendMessage(buyer, MessageConfiguration.message("banking_account_not_found"));
            bankingModule.removePinRequest(request.getSellerUuid());
            return;
        }

        if (buyerAccount.getFrozen()) {
            ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_account_frozen"));
            return;
        }

        if (buyerAccount.getBalance() < request.getAmount()) {
            ChatUtils.sendMessage(buyer, MessageConfiguration.message("banking_not_enough_money"));
            return;
        }

        // Get seller's primary account
        Player seller = Bukkit.getPlayer(request.getSellerUuid());
        BankAccountModel sellerAccount = bankingModule.getAccountsFromPlayer(request.getSellerUuid())
                .stream()
                .findFirst()
                .orElse(null);

        if (sellerAccount == null) {
            ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_seller_no_account"));
            return;
        }

        // Fire transaction events
        TransactionUpdateEvent buyerEvent = new TransactionUpdateEvent(
                buyer.getUniqueId(),
                buyer.getName(),
                TransactionType.WITHDRAW,
                request.getAmount(),
                buyerAccount,
                "PIN Terminal Payment to " + (seller != null ? seller.getName() : "Unknown"),
                System.currentTimeMillis()
        );

        if (EventUtils.callCancellable(buyerEvent)) {
            ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_transaction_cancelled"));
            return;
        }

        TransactionUpdateEvent sellerEvent = new TransactionUpdateEvent(
                request.getSellerUuid(),
                seller != null ? seller.getName() : "Unknown",
                TransactionType.DEPOSIT,
                request.getAmount(),
                sellerAccount,
                "PIN Terminal Payment from " + buyer.getName(),
                System.currentTimeMillis()
        );

        if (EventUtils.callCancellable(sellerEvent)) {
            ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_transaction_cancelled"));
            return;
        }

        // Process the transaction
        buyerAccount.setBalance(buyerAccount.getBalance() - request.getAmount());
        sellerAccount.setBalance(sellerAccount.getBalance() + request.getAmount());

        StormDatabase.getInstance().saveStormModel(buyerAccount);
        StormDatabase.getInstance().saveStormModel(sellerAccount);

        // Log transactions
        TransactionsModule transactionsModule = OpenMinetopia.getModuleManager().get(TransactionsModule.class);
        long timestamp = System.currentTimeMillis();

        transactionsModule.createTransactionLog(
                timestamp,
                buyer.getUniqueId(),
                buyer.getName(),
                TransactionType.WITHDRAW,
                request.getAmount(),
                buyerAccount.getUniqueId(),
                "PIN Terminal Payment to " + (seller != null ? seller.getName() : "Unknown")
        );

        transactionsModule.createTransactionLog(
                timestamp,
                request.getSellerUuid(),
                seller != null ? seller.getName() : "Unknown",
                TransactionType.DEPOSIT,
                request.getAmount(),
                sellerAccount.getUniqueId(),
                "PIN Terminal Payment from " + buyer.getName()
        );

        // Notify both parties
        ChatUtils.sendMessage(buyer, MessageConfiguration.message("pin_payment_successful")
                .replace("<amount>", bankingModule.format(request.getAmount()))
                .replace("<player>", seller != null ? seller.getName() : "Unknown"));

        if (seller != null) {
            ChatUtils.sendMessage(seller, MessageConfiguration.message("pin_payment_received")
                    .replace("<amount>", bankingModule.format(request.getAmount()))
                    .replace("<player>", buyer.getName()));
        }

        // Remove the request
        bankingModule.removePinRequest(request.getSellerUuid());
    }
}

