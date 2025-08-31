package nl.openminetopia.modules.banking.tasks;

import com.jazzkuh.modulemanager.spigot.handlers.tasks.TaskInfo;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.player.PlayerModule;
import nl.openminetopia.modules.player.configuration.LevelCheckConfiguration;
import nl.openminetopia.modules.transactions.enums.TransactionType;
import nl.openminetopia.modules.transactions.events.TransactionUpdateEvent;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.events.EventUtils;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

@TaskInfo(repeating = true, period = 10L * 20L)
public class WagePaymentTask implements Runnable {

    private final Supplier<LevelCheckConfiguration> levelCheckConfigurationSupplier;
    private final PlayerModule playerModule = OpenMinetopia.getModuleManager().get(PlayerModule.class);
    private final BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);

    public WagePaymentTask(Supplier<LevelCheckConfiguration> levelCheckConfigurationSupplier) {
        this.levelCheckConfigurationSupplier = levelCheckConfigurationSupplier;
    }

    @Override
    public void run() {
        long wageTime = this.levelCheckConfigurationSupplier.get().getWageInterval() * 1000L;
        for (MinetopiaPlayer minetopiaPlayer : PlayerManager.getInstance().getOnlinePlayers().values()) {
            Player bukkitPlayer = minetopiaPlayer.getBukkit().getPlayer();
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) continue;
            minetopiaPlayer.updatePlaytime();
            if (!levelCheckConfigurationSupplier.get().isWageEnabled()) continue;
            long playerWageTime = minetopiaPlayer.getWageTime();
            long totalPlayTime = minetopiaPlayer.getPlaytime();

            if (playerWageTime + wageTime <= totalPlayTime) {
                giveWage(minetopiaPlayer);
                minetopiaPlayer.setWageTime(totalPlayTime);
            }
        }
    }

    private void giveWage(MinetopiaPlayer minetopiaPlayer) {
        BankAccountModel bankAccountModel = bankingModule.getAccountById(minetopiaPlayer.getUuid());
        LevelCheckConfiguration configuration = levelCheckConfigurationSupplier.get();
        if (bankAccountModel == null) return;

        double wage;
        if (configuration.getWageOverrides().containsKey(minetopiaPlayer.getLevel())) {
            wage = configuration.getWageOverrides().get(minetopiaPlayer.getLevel());
        } else {
            String wageFormula = configuration.getWageFormula().replace("<level>", "l");
            Expression expression = new ExpressionBuilder(wageFormula).variables("l").build().setVariable("l", minetopiaPlayer.getLevel());
            wage = expression.evaluate();
        }

        TransactionUpdateEvent transactionUpdateEvent = new TransactionUpdateEvent(minetopiaPlayer.getBukkit().getUniqueId(), playerModule.getName(), TransactionType.DEPOSIT, wage, bankAccountModel, "Wage payment for playtime", System.currentTimeMillis());
        if (EventUtils.callCancellable(transactionUpdateEvent)) return;

        bankAccountModel.setBalance(bankAccountModel.getBalance() + wage);
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("levelcheck_wage")
                .replace("<amount>", String.valueOf(wage)));
    }
}
