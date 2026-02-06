package nl.openminetopia.modules.belasting.gui;

import dev.triumphteam.gui.guis.GuiItem;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.modules.belasting.service.TaxService;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.transactions.TransactionsModule;
import nl.openminetopia.modules.transactions.enums.TransactionType;
import nl.openminetopia.modules.transactions.events.TransactionUpdateEvent;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.events.EventUtils;
import nl.openminetopia.utils.item.ItemBuilder;
import nl.openminetopia.utils.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BelastingPaymentMenu extends Menu {

    private final Player player;
    private final TaxInvoiceModel invoice;
    private final TaxService taxService;
    private final BelastingConfiguration config;

    public BelastingPaymentMenu(Player player, TaxInvoiceModel invoice, TaxService taxService, BelastingConfiguration config) {
        super(config.getGuiTitle(), config.getGuiRows());
        this.player = player;
        this.invoice = invoice;
        this.taxService = taxService;
        this.config = config;

        gui.disableAllInteractions();

        ItemStack background = ItemsAdderHelper.getItem(config.getItemsAdderBackground(), config.getFallbackBackground());
        for (int slot : config.getGuiSlotsBackground()) {
            if (slot >= 0 && slot < config.getGuiRows() * 9) {
                gui.setItem(slot, new GuiItem(background, e -> e.setCancelled(true)));
            }
        }

        String amountStr = OpenMinetopia.getModuleManager().get(BankingModule.class).format(invoice.getTotalAmount());
        String infoLore = config.getMessageGuiTotal().replace("<amount>", amountStr);
        ItemStack infoItem = ItemsAdderHelper.getItem(config.getItemsAdderInfo(), config.getFallbackInfo());
        ItemBuilder infoBuilder = new ItemBuilder(infoItem.clone()).setName(infoLore);
        gui.setItem(config.getGuiSlotInfo(), new GuiItem(infoBuilder.toItemStack(), e -> e.setCancelled(true)));

        ItemStack confirmItem = ItemsAdderHelper.getItem(config.getItemsAdderConfirm(), config.getFallbackConfirm());
        gui.setItem(config.getGuiSlotConfirm(), new GuiItem(confirmItem, e -> {
            e.setCancelled(true);
            handleConfirm();
        }));

        ItemStack declineItem = ItemsAdderHelper.getItem(config.getItemsAdderDecline(), config.getFallbackDecline());
        gui.setItem(config.getGuiSlotDecline(), new GuiItem(declineItem, e -> {
            e.setCancelled(true);
            player.closeInventory();
        }));

        gui.setDefaultClickAction(e -> e.setCancelled(true));
    }

    private void handleConfirm() {
        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        TransactionsModule transactionsModule = OpenMinetopia.getModuleManager().get(TransactionsModule.class);
        var account = bankingModule.getAccountById(player.getUniqueId());
        if (account == null) {
            ChatUtils.sendMessage(player, config.getMessagePaidInsufficient());
            return;
        }
        double total = invoice.getTotalAmount();
        if (account.getBalance() < total) {
            ChatUtils.sendMessage(player, config.getMessagePaidInsufficient());
            return;
        }
        TransactionUpdateEvent event = new TransactionUpdateEvent(
                new UUID(0, 0), "Belasting", TransactionType.WITHDRAW, total, account,
                "Belasting betaling", System.currentTimeMillis());
        if (EventUtils.callCancellable(event)) {
            return;
        }
        account.setBalance(account.getBalance() - total);
        transactionsModule.createTransactionLog(
                System.currentTimeMillis(), new UUID(0, 0), "Belasting",
                TransactionType.WITHDRAW, total, account.getUniqueId(), "Belasting betaling");
        taxService.markPaid(invoice).thenRun(() -> {
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                String msg = config.getMessagePaidSuccess().replace("<amount>", bankingModule.format(total));
                ChatUtils.sendMessage(player, msg);
                player.closeInventory();
            });
        });
    }
}
