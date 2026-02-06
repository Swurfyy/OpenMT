package nl.openminetopia.modules.belasting.configuration;

import lombok.Getter;
import nl.openminetopia.utils.config.ConfigurateConfig;
import org.bukkit.Material;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BelastingConfiguration extends ConfigurateConfig {

    private final int taxIntervalDays;
    private final String taxCalculationMode;
    private final double taxCalculationValue;
    private volatile long lastCycleRun;

    private final String guiTitle;
    private final int guiRows;
    private final int guiSlotConfirm;
    private final int guiSlotDecline;
    private final int guiSlotInfo;
    private final List<Integer> guiSlotsBackground;

    private final String itemsAdderConfirm;
    private final String itemsAdderDecline;
    private final String itemsAdderInfo;
    private final String itemsAdderBackground;

    private final Material fallbackConfirm;
    private final Material fallbackDecline;
    private final Material fallbackInfo;
    private final Material fallbackBackground;

    private final String messageLoginUnpaid;
    private final String messageGuiTotal;
    private final String messagePaidSuccess;
    private final String messagePaidInsufficient;
    private final String messageNoInvoice;
    private final String messageExcludeAdded;
    private final String messageExcludeRemoved;
    private final String messageAdminNoInvoices;
    private final String messageTimeFormatInvalid;
    private final String messagePlayerNotFound;
    private final String messageSimulateDone;
    private final String messageSimulateNoPlots;
    private final String messageResetCycleDone;
    private final String messageOpenGuiOpened;
    private final String messageOpenGuiNoInvoice;
    private final String messageOpenGuiNotOnline;

    private final String commandBase;
    private final String permissionAdmin;
    private final String permissionExclude;

    public BelastingConfiguration(File dataFolder) {
        super(dataFolder, "belasting.yml", "default/belasting.yml", true);

        var root = rootNode.node("belasting");
        this.taxIntervalDays = root.node("tax-interval-days").getInt(14);
        var calc = root.node("tax-calculation");
        this.taxCalculationMode = calc.node("mode").getString("PERCENTAGE");
        this.taxCalculationValue = calc.node("value").getDouble(0.5);
        this.lastCycleRun = root.node("last-cycle-run").getLong(0);

        var gui = root.node("gui");
        this.guiTitle = gui.node("title").getString("<dark_gray>Belasting betalen");
        this.guiRows = gui.node("rows").getInt(3);
        var slots = gui.node("slots");
        this.guiSlotConfirm = slots.node("confirm").getInt(13);
        this.guiSlotDecline = slots.node("decline").getInt(15);
        this.guiSlotInfo = slots.node("info").getInt(4);
        this.guiSlotsBackground = new ArrayList<>();
        for (var node : slots.node("background").childrenList()) {
            Integer v = node.getInt();
            if (v != null) guiSlotsBackground.add(v);
        }

        var ia = gui.node("itemsadder");
        this.itemsAdderConfirm = ia.node("confirm").getString("openminetopia:belasting_confirm");
        this.itemsAdderDecline = ia.node("decline").getString("openminetopia:belasting_decline");
        this.itemsAdderInfo = ia.node("info").getString("openminetopia:belasting_info");
        this.itemsAdderBackground = ia.node("background").getString("openminetopia:belasting_background");

        var fallback = gui.node("fallback-material");
        this.fallbackConfirm = parseMaterial(fallback.node("confirm").getString("LIME_CONCRETE"));
        this.fallbackDecline = parseMaterial(fallback.node("decline").getString("RED_CONCRETE"));
        this.fallbackInfo = parseMaterial(fallback.node("info").getString("PAPER"));
        this.fallbackBackground = parseMaterial(fallback.node("background").getString("GRAY_STAINED_GLASS_PANE"));

        var msg = root.node("messages");
        this.messageLoginUnpaid = msg.node("login-unpaid").getString("");
        this.messageGuiTotal = msg.node("gui-total").getString("");
        this.messagePaidSuccess = msg.node("paid-success").getString("");
        this.messagePaidInsufficient = msg.node("paid-insufficient").getString("");
        this.messageNoInvoice = msg.node("no-invoice").getString("");
        this.messageExcludeAdded = msg.node("exclude-added").getString("");
        this.messageExcludeRemoved = msg.node("exclude-removed").getString("");
        this.messageAdminNoInvoices = msg.node("admin-no-invoices").getString("");
        this.messageTimeFormatInvalid = msg.node("time-format-invalid").getString("");
        this.messagePlayerNotFound = msg.node("player-not-found").getString("");
        this.messageSimulateDone = msg.node("simulate-done").getString("");
        this.messageSimulateNoPlots = msg.node("simulate-no-plots").getString("");
        this.messageResetCycleDone = msg.node("reset-cycle-done").getString("");
        this.messageOpenGuiOpened = msg.node("open-gui-opened").getString("");
        this.messageOpenGuiNoInvoice = msg.node("open-gui-no-invoice").getString("");
        this.messageOpenGuiNotOnline = msg.node("open-gui-not-online").getString("");

        var cmd = root.node("commands");
        this.commandBase = cmd.node("base").getString("belasting");
        this.permissionAdmin = cmd.node("permission-admin").getString("openminetopia.belasting.admin");
        this.permissionExclude = cmd.node("permission-exclude").getString("openminetopia.belasting.exclude");
    }

    public void setLastCycleRun(long timestamp) {
        this.lastCycleRun = timestamp;
        try {
            rootNode.node("belasting").node("last-cycle-run").set(Long.valueOf(timestamp));
            saveConfiguration();
        } catch (SerializationException e) {
            nl.openminetopia.OpenMinetopia.getInstance().getLogger().warning("Belasting: kon last-cycle-run niet opslaan: " + e.getMessage());
        }
    }

    private static Material parseMaterial(String name) {
        Material m = Material.matchMaterial(name);
        return m != null ? m : Material.PAPER;
    }

    public boolean isTaxPercentage() {
        return "PERCENTAGE".equalsIgnoreCase(taxCalculationMode);
    }
}
