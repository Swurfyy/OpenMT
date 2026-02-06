package nl.openminetopia.modules.belasting.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("belasting")
public class BelastingAdminOpenGuiCommand extends BaseCommand {

    @Subcommand("admin open-gui")
    @CommandPermission("openminetopia.belasting.admin")
    @Description("Open de belastingbetalings-GUI voor een speler (als die een openstaande factuur heeft).")
    @Syntax("<speler>")
    public void onOpenGui(CommandSender sender, String playerName) {
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        BelastingConfiguration config = module.getConfig();

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            ChatUtils.sendMessage(sender, config.getMessageOpenGuiNotOnline());
            return;
        }

        module.getTaxService().getUnpaidInvoice(targetPlayer.getUniqueId()).thenAccept(invoice -> {
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                if (invoice == null) {
                    String msg = config.getMessageOpenGuiNoInvoice().replace("<player>", targetPlayer.getName());
                    ChatUtils.sendMessage(sender, msg);
                    return;
                }
                module.getGuiManager().openPaymentGui(targetPlayer, invoice);
                String msg = config.getMessageOpenGuiOpened().replace("<player>", targetPlayer.getName());
                ChatUtils.sendMessage(sender, msg);
            });
        });
    }
}
