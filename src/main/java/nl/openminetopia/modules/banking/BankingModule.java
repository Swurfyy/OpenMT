package nl.openminetopia.modules.banking;

import com.craftmend.storm.api.enums.Where;
import lombok.Getter;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.Economy;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.banking.commands.BankingCommand;
import nl.openminetopia.modules.banking.commands.subcommands.*;
import nl.openminetopia.modules.banking.enums.AccountPermission;
import nl.openminetopia.modules.banking.enums.AccountType;
import nl.openminetopia.modules.banking.listeners.BankingInteractionListener;
import nl.openminetopia.modules.banking.vault.VaultEconomyHandler;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.banking.models.BankPermissionModel;
import nl.openminetopia.modules.data.utils.StormUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Todo:
 * - Menu's (banking inventory)
 * - Debitcards
 */

@Getter
public class BankingModule extends Module {

    private DecimalFormat decimalFormat;
    private Collection<BankAccountModel> bankAccountModels = new ArrayList<>();

    @Override
    public void enable() {
        decimalFormat = new DecimalFormat(OpenMinetopia.getBankingConfiguration().getEconomyFormat());

        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            OpenMinetopia.getInstance().getLogger().info("Loading bank accounts..");

            this.getBankAccounts().whenComplete((accounts, accountThrowable) -> {
                if (accountThrowable != null) {
                    OpenMinetopia.getInstance().getLogger().severe("Something went wrong while trying to load all bank accounts: " + accountThrowable.getMessage());
                    return;
                }

                bankAccountModels = accounts;
                bankAccountModels.forEach(BankAccountModel::initSavingTask);

                OpenMinetopia.getInstance().getLogger().info("Loaded a total of " + bankAccountModels.size() + " accounts.");

                this.getBankPermissions().whenComplete((permissions, throwable) -> {
                    if (throwable != null) {
                        OpenMinetopia.getInstance().getLogger().severe("Something went wrong while trying to load all bank permissions: " + throwable.getMessage());
                        return;
                    }

                    permissions.forEach(permission -> {
                        BankAccountModel accountModel = getAccountById(permission.getAccount());
                        if (accountModel == null) {
                            /*
                            todo: remove permission from db?
                             dataModule.getAdapter().deleteBankPermission(permission.getAccount(), permission.getUuid());
                             */
                            return;
                        }
                        accountModel.getUsers().put(permission.getUuid(), permission.getPermission());
                    });
                    OpenMinetopia.getInstance().getLogger().info("Found and applied " + permissions.size() + " bank permissions.");
                });
            });
        }, 3L);

        OpenMinetopia.getCommandManager().getCommandCompletions().registerCompletion("accountNames", context -> bankAccountModels.stream().map(BankAccountModel::getName).collect(Collectors.toList()));

        registerCommand(new BankingCommand());
        registerCommand(new BankingCreateCommand());
        registerCommand(new BankingDeleteCommand());
        registerCommand(new BankingUsersCommand());
        registerCommand(new BankingOpenCommand());
        registerCommand(new BankingFreezeCommand());
        registerCommand(new BankingInfoCommand());
        registerCommand(new BankingBalanceCommand());
        registerCommand(new BankingListCommand());

        registerListener(new BankingInteractionListener());

        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Bukkit.getServicesManager().register(Economy.class, new VaultEconomyHandler(), OpenMinetopia.getInstance(), ServicePriority.Normal);
            OpenMinetopia.getInstance().getLogger().info("Registered Vault economy handler.");
        }
    }

    @Override
    public void disable() {
        bankAccountModels.forEach(accountModel -> {
            if (accountModel.getSavingTask() != null) {
                accountModel.getSavingTask().saveAndCancel();
            }
        });
    }

    public List<BankAccountModel> getAccountsFromPlayer(UUID uuid) {
        return bankAccountModels.stream().filter(account -> account.getUsers().containsKey(uuid)).collect(Collectors.toList());
    }

    public BankAccountModel getAccountByName(String name) {
        return bankAccountModels.stream().filter(account -> account.getName().equals(name)).findAny().orElse(null);
    }

    public BankAccountModel getAccountById(UUID uuid) {
        return bankAccountModels.stream().filter(account -> account.getUniqueId().equals(uuid)).findAny().orElse(null);
    }

    public CompletableFuture<Collection<BankAccountModel>> getBankAccounts() {
        CompletableFuture<Collection<BankAccountModel>> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<BankAccountModel> accountModels = StormDatabase.getInstance().getStorm().buildQuery(BankAccountModel.class)
                        .where("type", Where.NOT_EQUAL, AccountType.PRIVATE.toString())
                        .execute().join();
                completableFuture.complete(accountModels);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }

    public CompletableFuture<Collection<BankPermissionModel>> getBankPermissions() {
        CompletableFuture<Collection<BankPermissionModel>> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<BankPermissionModel> permissionModels = StormDatabase.getInstance().getStorm().buildQuery(BankPermissionModel.class)
                        .execute().join();
                completableFuture.complete(permissionModels);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }

    public CompletableFuture<BankAccountModel> createBankAccount(UUID uuid, AccountType type, double balance, String name, boolean frozen) {
        CompletableFuture<BankAccountModel> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            BankAccountModel accountModel = new BankAccountModel();
            accountModel.setUniqueId(uuid);
            accountModel.setType(type);
            accountModel.setBalance(balance);
            accountModel.setName(name);
            accountModel.setFrozen(frozen);

            StormDatabase.getInstance().saveStormModel(accountModel);
            completableFuture.complete(accountModel);
        });

        return completableFuture;
    }

    public CompletableFuture<Void> saveBankAccount(BankAccountModel accountModel) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        StormUtils.updateModelData(BankAccountModel.class,
                query -> query.where("uuid", Where.EQUAL, accountModel.getUniqueId().toString()),
                model -> {
                    model.setBalance(accountModel.getBalance());
                    model.setFrozen(accountModel.getFrozen());
                    model.setName(accountModel.getName());
                    model.setType(accountModel.getType());
                }
        );

        return completableFuture;
    }

    public CompletableFuture<Void> deleteBankAccount(UUID accountUuid) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            StormUtils.deleteModelData(BankAccountModel.class,
                    query -> query.where("uuid", Where.EQUAL, accountUuid.toString())
            ).join();

            StormUtils.deleteModelData(BankPermissionModel.class,
                    query -> query.where("account", Where.EQUAL, accountUuid.toString())
            ).join();

            completableFuture.complete(null);
        });

        return completableFuture;
    }

    public CompletableFuture<BankPermissionModel> createBankPermission(UUID player, UUID accountId, AccountPermission permission) {
        CompletableFuture<BankPermissionModel> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            BankPermissionModel permissionModel = new BankPermissionModel();
            permissionModel.setUuid(player);
            permissionModel.setAccount(accountId);
            permissionModel.setPermission(permission);

            StormDatabase.getInstance().saveStormModel(permissionModel);
            completableFuture.complete(permissionModel);
        });

        return completableFuture;
    }

    public CompletableFuture<Void> deleteBankPermission(UUID accountUuid, UUID playerUuid) {
        return StormUtils.deleteModelData(BankPermissionModel.class, query -> {
            query.where("uuid", Where.EQUAL, playerUuid.toString());
            query.where("account", Where.EQUAL, accountUuid.toString());
        });
    }

    @SneakyThrows
    public CompletableFuture<BankAccountModel> getAccountModel(UUID accountId) {
        CompletableFuture<BankAccountModel> accountModelFuture = new CompletableFuture<>();

        Collection<BankAccountModel> collectionFuture = StormDatabase.getInstance().getStorm()
                .buildQuery(BankAccountModel.class)
                .where("uuid", Where.EQUAL, accountId.toString())
                .execute().join();

        accountModelFuture.complete(collectionFuture.stream().findFirst().orElse(null));

        return accountModelFuture;
    }

    public void createPermissions(BankPermissionModel permissionModel) throws SQLException {
        BankAccountModel accountModel = getAccountById(permissionModel.getAccount());
        accountModel.getUsers().put(permissionModel.getUuid(), permissionModel.getPermission());
        StormDatabase.getInstance().getStorm().save(permissionModel);
    }

    public String format(double amount) {
        return "€" + decimalFormat.format(amount);
    }

}
