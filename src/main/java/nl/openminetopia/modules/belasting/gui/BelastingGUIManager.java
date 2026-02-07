package nl.openminetopia.modules.belasting.gui;

import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.modules.belasting.service.TaxService;
import org.bukkit.entity.Player;

public class BelastingGUIManager {

    private final TaxService taxService;
    private final BelastingConfiguration config;

    public BelastingGUIManager(TaxService taxService, BelastingConfiguration config) {
        this.taxService = taxService;
        this.config = config;
    }

    public void openPaymentGui(Player player, TaxInvoiceModel invoice) {
        if (invoice == null) return;
        BelastingPaymentMenu menu = new BelastingPaymentMenu(player, invoice, taxService, config);
        menu.openWithDelay(player);
    }
}
