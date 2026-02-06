package nl.openminetopia.modules.belasting.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

@CommandAlias("belasting")
public class BelastingBetaalCommand extends BaseCommand {

    @Subcommand("betaal")
    @Description("Open de belastingbetalings-GUI.")
    public void onBetaal(Player player) {
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        BelastingConfiguration config = module.getConfig();
        module.getTaxService().getUnpaidInvoice(player.getUniqueId()).thenAccept(invoice -> {
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                if (invoice == null) {
                    ChatUtils.sendMessage(player, config.getMessageNoInvoice());
                    return;
                }
                module.getGuiManager().openPaymentGui(player, invoice);
            });
        });
    }
}
