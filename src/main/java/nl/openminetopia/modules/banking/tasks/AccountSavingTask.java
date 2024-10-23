package nl.openminetopia.modules.banking.tasks;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.data.storm.models.BankAccountModel;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class AccountSavingTask extends BukkitRunnable {

    private final BankAccountModel account;

    public AccountSavingTask(BankAccountModel account) {
        this.account = account;
    }

    @Override
    public void run() {
        BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);
        bankingModule.saveBankAccount(account);
    }

    public void saveAndCancel() {
        run();
        cancel();
    }
}
