package nl.openminetopia.modules.banking.menus;

import com.jazzkuh.inventorylib.objects.PaginatedMenu;
import com.jazzkuh.inventorylib.objects.icon.Icon;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.transactions.TransactionsModule;
import nl.openminetopia.modules.transactions.enums.TransactionType;
import nl.openminetopia.modules.transactions.objects.TransactionModel;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.PersistentDataUtil;
import nl.openminetopia.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class BankTransactionsMenu extends PaginatedMenu {

    private final BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
    private final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private final BankAccountModel accountModel;

    public BankTransactionsMenu(Player player, BankAccountModel accountModel) {
        super(ChatUtils.color(accountModel.getType().getColor() + accountModel.getName() + " <reset>| <red>Transactions"), 4);
        this.accountModel = accountModel;
        this.registerPageSlotsBetween(0, 27);

        this.addSpecialIcon(new Icon(13, new ItemBuilder(Material.IRON_BLOCK)
                .setName("<red>Transacties inladen.")
                .toItemStack()
        ));
        createBackButton();

        TransactionsModule transactionsModule = OpenMinetopia.getModuleManager().get(TransactionsModule.class);
        transactionsModule.getAccountTransactions(TransactionsModule.LookupType.BANK_ACCOUNT, accountModel.getUniqueId()).whenComplete(((transactionModels, throwable) -> {
            if (throwable != null) {
                player.closeInventory();
                ChatUtils.sendMessage(player, "<red>Er is iets misgegaan.");
                return;
            }

            player.sendMessage(ChatUtils.color("<gold>Er zijn in totaal <red>" + transactionModels.size() + " <gold>transacties ingeladen."));
            clearItems();

            List<TransactionModel> sortedTransactions = transactionModels.stream()
                    .sorted(Comparator.comparing(TransactionModel::getTime).reversed())
                    .toList();

            for (TransactionModel sortedTransaction : sortedTransactions) {
                TransactionType type = sortedTransaction.getType();
                ItemBuilder transactionBuilder = new ItemBuilder(type.getMaterial());
                transactionBuilder.setName("<yellow>" + type.getName());
                transactionBuilder.addLoreLine("");
                transactionBuilder.addLoreLine("<gold>Datum: <red>" + format.format(new Date(sortedTransaction.getTime())));
                transactionBuilder.addLoreLine("<gold>Bedrag: <red>" + bankingModule.format(sortedTransaction.getAmount()));
                transactionBuilder.addLoreLine("<gold>Door: <red>" + sortedTransaction.getUsername());
                addItem(new Icon(PersistentDataUtil.set(transactionBuilder.toItemStack(), sortedTransaction.getTime(), "time")));
            }

            createBackButton();
            update();
        }));
    }

    @Override
    public Icon getPreviousPageItem() {
        ItemStack previousStack = new ItemBuilder(Material.ARROW)
                .setName(MessageConfiguration.message("previous_page"))
                .toItemStack();
        return new Icon(29, previousStack, e -> e.setCancelled(true));
    }

    @Override
    public Icon getNextPageItem() {
        ItemStack previousStack = new ItemBuilder(Material.ARROW)
                .setName(MessageConfiguration.message("next_page"))
                .toItemStack();
        return new Icon(33, previousStack, e -> e.setCancelled(true));
    }

    private void createBackButton() {
        this.addSpecialIcon(new Icon(31, new ItemBuilder(Material.OAK_SIGN)
                .setName(MessageConfiguration.message("go_back"))
                .toItemStack(),
                event -> {
                    event.setCancelled(true);
                    new BankAccountSelectionMenu(player, accountModel.getType()).open(player);
                }
        ));
    }
}
