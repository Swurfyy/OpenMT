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
import net.kyori.adventure.text.format.TextDecoration;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.events.EventUtils;
import nl.openminetopia.utils.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BelastingPaymentMenu extends Menu {

    private static final int DOUBLE_CHEST_SLOTS = 54;

    private final Player player;
    private final TaxInvoiceModel invoice;
    private final TaxService taxService;
    private final BelastingConfiguration config;
    private volatile boolean paymentInProgress;

    public BelastingPaymentMenu(Player player, TaxInvoiceModel invoice, TaxService taxService, BelastingConfiguration config) {
        super(config.getGuiTitle(), Math.max(6, config.getGuiRows()));
        this.player = player;
        this.invoice = invoice;
        this.taxService = taxService;
        this.config = config;

        gui.disableAllInteractions();

        ItemStack filler = ItemsAdderHelper.getItem(config.getItemsAdderFiller(), config.getFallbackFiller());
        ItemStack fillerNamed = setDisplayNameOnly(filler, " ");
        Set<Integer> actionSlots = Set.of(
                config.getGuiSlotConfirm(),
                config.getGuiSlotDecline()
        );
        Set<Integer> infoSlots = Set.copyOf(config.getGuiSlotsInfo());

        for (int slot = 0; slot < DOUBLE_CHEST_SLOTS; slot++) {
            if (actionSlots.contains(slot) || infoSlots.contains(slot)) continue;
            gui.setItem(slot, new GuiItem(fillerNamed, e -> e.setCancelled(true)));
        }

        String amountStr = OpenMinetopia.getModuleManager().get(BankingModule.class).format(invoice.getTotalAmount());
        String infoText = config.getGuiButtonNameInfo().replace("<amount>", amountStr);
        List<String> infoLore = config.getGuiButtonNameInfoLore().stream()
                .map(line -> line.replace("<amount>", amountStr))
                .toList();
        ItemStack infoItem = ItemsAdderHelper.getItem(config.getItemsAdderInfo(), config.getFallbackInfo());
        ItemStack infoStack = infoLore.isEmpty()
                ? setDisplayNameOnly(infoItem, infoText)
                : setDisplayNameAndLore(infoItem, infoText, infoLore);
        for (int slot : config.getGuiSlotsInfo()) {
            if (slot >= 0 && slot < DOUBLE_CHEST_SLOTS) {
                gui.setItem(slot, new GuiItem(infoStack.clone(), e -> e.setCancelled(true)));
            }
        }

        ItemStack confirmItem = ItemsAdderHelper.getItem(config.getItemsAdderConfirm(), config.getFallbackConfirm());
        ItemStack confirmStack = setDisplayNameOnly(confirmItem, config.getGuiButtonNameConfirm());
        gui.setItem(config.getGuiSlotConfirm(), new GuiItem(confirmStack, e -> {
            e.setCancelled(true);
            handleConfirm();
        }));

        ItemStack declineItem = ItemsAdderHelper.getItem(config.getItemsAdderDecline(), config.getFallbackDecline());
        ItemStack declineStack = setDisplayNameOnly(declineItem, config.getGuiButtonNameDecline());
        gui.setItem(config.getGuiSlotDecline(), new GuiItem(declineStack, e -> {
            e.setCancelled(true);
            player.closeInventory();
        }));

        gui.setDefaultClickAction(e -> e.setCancelled(true));
    }

    public void openWithDelay(Player target) {
        OpenMinetopia.getInstance().getServer().getScheduler().runTaskLater(
                OpenMinetopia.getInstance(),
                () -> {
                    gui.open(target);
                    // Apply ItemsAdder texture overlay on next tick so the inventory is fully open
                    OpenMinetopia.getInstance().getServer().getScheduler().runTaskLater(
                            OpenMinetopia.getInstance(),
                            () -> ItemsAdderGuiHelper.applyTextureToCurrentInventory(target, config.getGuiTextureKey(), config.getGuiTextureTitleOffset(), config.getGuiTextureInventoryOffset()),
                            1L
                    );
                },
                1L
        );
    }

    private void handleConfirm() {
        if (paymentInProgress) return;
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
        paymentInProgress = true;
        taxService.markPaid(invoice)
                .thenRun(() -> {
                    OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                        paymentInProgress = false;
                        account.setBalance(account.getBalance() - total);
                        transactionsModule.createTransactionLog(
                                System.currentTimeMillis(), new UUID(0, 0), "Belasting",
                                TransactionType.WITHDRAW, total, account.getUniqueId(), "Belasting betaling");
                        String msg = config.getMessagePaidSuccess().replace("<amount>", bankingModule.format(total));
                        ChatUtils.sendMessage(player, msg);
                        player.closeInventory();
                    });
                })
                .exceptionally(ex -> {
                    OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                        paymentInProgress = false;
                        ChatUtils.sendMessage(player, config.getMessagePaymentFailed());
                    });
                    return null;
                });
    }

    /**
     * Sets only the display name on a clone of the item, without replacing the full ItemMeta.
     * This preserves ItemsAdder custom model data and other plugin meta (e.g. fivemopia:invisible).
     */
    private static ItemStack setDisplayNameOnly(ItemStack item, String name) {
        ItemStack stack = item.clone();
        stack.editMeta(ItemMeta.class, meta ->
                meta.displayName(ChatUtils.color(name).decoration(TextDecoration.ITALIC, false)));
        return stack;
    }

    /**
     * Sets display name and lore on a clone of the item via editMeta, preserving ItemsAdder meta.
     */
    private static ItemStack setDisplayNameAndLore(ItemStack item, String name, List<String> loreLines) {
        ItemStack stack = item.clone();
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatUtils.color(line).decoration(TextDecoration.ITALIC, false));
        }
        stack.editMeta(ItemMeta.class, meta -> {
            meta.displayName(ChatUtils.color(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });
        return stack;
    }
}
