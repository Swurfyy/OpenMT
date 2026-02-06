package nl.openminetopia.modules.belasting.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.modules.belasting.models.TaxInvoicePlotModel;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@CommandAlias("belasting")
public class BelastingAdminUnpaidCommand extends BaseCommand {

    @Subcommand("admin unpaid")
    @CommandPermission("openminetopia.belasting.admin")
    @Description("Toon alle onbetaalde belastingfacturen.")
    public void onUnpaid(CommandSender sender) {
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        BelastingConfiguration config = module.getConfig();
        module.getTaxService().getAllInvoices().thenCompose(invoices -> {
            List<TaxInvoiceModel> unpaid = invoices.stream().filter(i -> !i.isPaid()).toList();
            if (unpaid.isEmpty()) {
                runSync(() -> ChatUtils.sendMessage(sender, config.getMessageAdminNoInvoices()));
                return CompletableFuture.<Void>completedFuture(null);
            }
            CompletableFuture<Void> all = CompletableFuture.completedFuture(null);
            for (TaxInvoiceModel inv : unpaid) {
                Integer invId = inv.getId();
                if (invId == null) continue;
                all = all.thenCompose(v -> module.getTaxService().getPlotRows(invId).thenAccept(rows -> {
                    for (TaxInvoicePlotModel row : rows) {
                        String line = "<red>" + inv.getPlayerName() + " | " + row.getPlotId() + " | Unpaid";
                        runSync(() -> ChatUtils.sendMessage(sender, line));
                    }
                }));
            }
            return all;
        });
    }

    private static void runSync(Runnable r) {
        OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), r);
    }
}
