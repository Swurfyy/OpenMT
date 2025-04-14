package nl.openminetopia.modules.transactions;

import com.jazzkuh.modulemanager.spigot.SpigotModule;
import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.transactions.enums.TransactionType;
import nl.openminetopia.modules.transactions.objects.TransactionModel;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TransactionsModule extends SpigotModule<@NotNull OpenMinetopia> {

    public TransactionsModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager) {
        super(moduleManager);
    }

    @Override
    public void onEnable() {

    }

    public CompletableFuture<TransactionModel> createTransactionLog(long time, UUID player, TransactionType type, double amount, UUID bankAccount, String description) {
        CompletableFuture<TransactionModel> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            TransactionModel transactionModel = new TransactionModel();
            transactionModel.setTime(time);
            transactionModel.setPlayer(player);
            transactionModel.setType(type);
            transactionModel.setAmount(amount);
            transactionModel.setBankAccount(bankAccount);
            transactionModel.setDescription(description);

            getLogger().warn("Nieuwe transactie: " + transactionModel);

            StormDatabase.getInstance().saveStormModel(transactionModel);
            completableFuture.complete(transactionModel);
        });

        return completableFuture;
    }

}
