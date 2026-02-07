package nl.openminetopia.modules.belasting.configuration;

import lombok.Getter;
import nl.openminetopia.utils.config.ConfigurateConfig;
import org.bukkit.Material;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
public class BelastingConfiguration extends ConfigurateConfig {

    private final int taxIntervalDays;
    private final String taxCalculationMode;
    private final double taxCalculationValue;
    private volatile String lastCycleRunDisplay;
    private volatile String nextCycleRunDisplay;

    private static final DateTimeFormatter DATE_TIME_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy | HH:mm", Locale.ROOT);
    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final String guiTitle;
    private final String guiTextureKey;
    private final int guiTextureTitleOffset;
    private final int guiTextureInventoryOffset;
    private final int guiRows;
    private final int guiSlotConfirm;
    private final int guiSlotDecline;
    private final List<Integer> guiSlotsInfo;

    private final String guiButtonNameConfirm;
    private final String guiButtonNameDecline;
    private final String guiButtonNameInfo;
    private final List<String> guiButtonNameInfoLore;

    private final String itemsAdderConfirm;
    private final String itemsAdderDecline;
    private final String itemsAdderInfo;
    private final String itemsAdderFiller;

    private final Material fallbackConfirm;
    private final Material fallbackDecline;
    private final Material fallbackInfo;
    private final Material fallbackFiller;

    private final String messageLoginUnpaid;
    private final String messageGuiTotal;
    private final String messagePaidSuccess;
    private final String messagePaidInsufficient;
    private final String messagePaymentFailed;
    private final String messageNoInvoice;
    private final String messageExcludeAdded;
    private final String messageExcludeRemoved;
    private final String messageAdminNoInvoices;
    private final String messageTimeFormatInvalid;
    private final String messagePlayerNotFound;
    private final String messageSimulateDone;
    private final String messageSimulateNoPlots;
    private final String messageSimulateNoNewInvoices;
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
        Object lastRaw = root.node("last-cycle-run").raw();
        if (lastRaw instanceof Number n) {
            long v = n.longValue();
            this.lastCycleRunDisplay = v <= 0 ? "" : formatMillis(v);
        } else {
            this.lastCycleRunDisplay = root.node("last-cycle-run").getString("");
        }
        Object nextRaw = root.node("next-cycle-run").raw();
        if (nextRaw instanceof Number n) {
            long v = n.longValue();
            this.nextCycleRunDisplay = v <= 0 ? "" : formatMillis(v);
        } else {
            this.nextCycleRunDisplay = root.node("next-cycle-run").getString("");
        }

        var gui = root.node("gui");
        this.guiTitle = gui.node("title").getString("");
        this.guiTextureKey = gui.node("texture-key").getString("minecraft:generic_54_tax");
        this.guiTextureTitleOffset = gui.node("texture-title-offset").getInt(8);
        this.guiTextureInventoryOffset = gui.node("texture-inventory-offset").getInt(-8);
        this.guiRows = gui.node("rows").getInt(6);
        var slots = gui.node("slots");
        this.guiSlotConfirm = slots.node("confirm").getInt(38);
        this.guiSlotDecline = slots.node("decline").getInt(42);
        this.guiSlotsInfo = new ArrayList<>();
        for (var node : slots.node("info").childrenList()) {
            Integer v = node.getInt();
            if (v != null) guiSlotsInfo.add(v);
        }
        if (guiSlotsInfo.isEmpty()) {
            guiSlotsInfo.add(12); guiSlotsInfo.add(13); guiSlotsInfo.add(14);
            guiSlotsInfo.add(21); guiSlotsInfo.add(22); guiSlotsInfo.add(23);
        }

        var btnNames = gui.node("button-names");
        this.guiButtonNameConfirm = btnNames.node("confirm").getString("<green>Betaal je belasting");
        this.guiButtonNameDecline = btnNames.node("decline").getString("<red>Betaal nog niet.");
        var infoNode = btnNames.node("info");
        if (infoNode.isList() && !infoNode.childrenList().isEmpty()) {
            var list = infoNode.childrenList();
            this.guiButtonNameInfo = list.get(0).getString("<gray>Totaal te betalen: <yellow><amount>");
            this.guiButtonNameInfoLore = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                String line = list.get(i).getString("");
                if (line != null && !line.isBlank()) this.guiButtonNameInfoLore.add(line);
            }
        } else {
            this.guiButtonNameInfo = infoNode.getString("<gray>Totaal te betalen: <yellow><amount>");
            this.guiButtonNameInfoLore = List.of();
        }

        var ia = gui.node("itemsadder");
        this.itemsAdderConfirm = ia.node("confirm").getString("fivemopia:invisible");
        this.itemsAdderDecline = ia.node("decline").getString("fivemopia:invisible");
        this.itemsAdderInfo = ia.node("info").getString("fivemopia:invisible");
        this.itemsAdderFiller = ia.node("filler").getString("fivemopia:invisible");

        var fallback = gui.node("fallback-material");
        this.fallbackConfirm = parseMaterial(fallback.node("confirm").getString("LIME_CONCRETE"));
        this.fallbackDecline = parseMaterial(fallback.node("decline").getString("RED_CONCRETE"));
        this.fallbackInfo = parseMaterial(fallback.node("info").getString("PAPER"));
        this.fallbackFiller = parseMaterial(fallback.node("filler").getString("STRUCTURE_VOID"));

        var msg = root.node("messages");
        this.messageLoginUnpaid = msg.node("login-unpaid").getString("");
        this.messageGuiTotal = msg.node("gui-total").getString("");
        this.messagePaidSuccess = msg.node("paid-success").getString("");
        this.messagePaidInsufficient = msg.node("paid-insufficient").getString("");
        this.messagePaymentFailed = msg.node("payment-failed").getString("<red>Betaling mislukt. Probeer het later opnieuw.</red>");
        this.messageNoInvoice = msg.node("no-invoice").getString("");
        this.messageExcludeAdded = msg.node("exclude-added").getString("");
        this.messageExcludeRemoved = msg.node("exclude-removed").getString("");
        this.messageAdminNoInvoices = msg.node("admin-no-invoices").getString("");
        this.messageTimeFormatInvalid = msg.node("time-format-invalid").getString("");
        this.messagePlayerNotFound = msg.node("player-not-found").getString("");
        this.messageSimulateDone = msg.node("simulate-done").getString("");
        this.messageSimulateNoPlots = msg.node("simulate-no-plots").getString("");
        this.messageSimulateNoNewInvoices = msg.node("simulate-no-new-invoices").getString("<yellow>Geen nieuwe facturen aangemaakt (geen plot-eigenaren, alle uitgesloten of hebben al een factuur).</yellow>");
        this.messageResetCycleDone = msg.node("reset-cycle-done").getString("");
        this.messageOpenGuiOpened = msg.node("open-gui-opened").getString("");
        this.messageOpenGuiNoInvoice = msg.node("open-gui-no-invoice").getString("");
        this.messageOpenGuiNotOnline = msg.node("open-gui-not-online").getString("");

        var cmd = root.node("commands");
        this.commandBase = cmd.node("base").getString("belasting");
        this.permissionAdmin = cmd.node("permission-admin").getString("openminetopia.belasting.admin");
        this.permissionExclude = cmd.node("permission-exclude").getString("openminetopia.belasting.exclude");
    }

    /** Parses "dd/MM/yyyy | HH:mm" to epoch ms; returns 0 if empty or invalid. */
    public long getLastCycleRunMillis() {
        return parseDisplayToMillis(lastCycleRunDisplay);
    }

    public long getNextCycleRunMillis() {
        return parseDisplayToMillis(nextCycleRunDisplay);
    }

    public String getLastCycleRunDisplay() {
        return lastCycleRunDisplay != null ? lastCycleRunDisplay : "";
    }

    public String getNextCycleRunDisplay() {
        return nextCycleRunDisplay != null ? nextCycleRunDisplay : "";
    }

    public String getMessagePaymentFailed() {
        return messagePaymentFailed != null ? messagePaymentFailed : "";
    }

    private static long parseDisplayToMillis(String display) {
        if (display == null || display.isBlank()) return 0;
        String s = display.trim();
        int pipe = s.indexOf(" | ");
        if (pipe <= 0 || pipe >= s.length() - 3) return 0;
        try {
            LocalDate date = LocalDate.parse(s.substring(0, pipe).trim(), DATE_PARSER);
            LocalTime time = LocalTime.parse(s.substring(pipe + 3).trim(), TIME_PARSER);
            return LocalDateTime.of(date, time).atZone(ZONE).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    private static String formatMillis(long timestamp) {
        if (timestamp <= 0) return "";
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZONE).format(DATE_TIME_DISPLAY);
    }

    public void setLastCycleRun(long timestamp) {
        this.lastCycleRunDisplay = formatMillis(timestamp);
        try {
            rootNode.node("belasting").node("last-cycle-run").set(lastCycleRunDisplay);
            saveConfiguration();
        } catch (SerializationException e) {
            nl.openminetopia.OpenMinetopia.getInstance().getLogger().warning("Belasting: kon last-cycle-run niet opslaan: " + e.getMessage());
        }
    }

    public void setNextCycleRun(long timestamp) {
        this.nextCycleRunDisplay = formatMillis(timestamp);
        try {
            rootNode.node("belasting").node("next-cycle-run").set(nextCycleRunDisplay);
            saveConfiguration();
        } catch (SerializationException e) {
            nl.openminetopia.OpenMinetopia.getInstance().getLogger().warning("Belasting: kon next-cycle-run niet opslaan: " + e.getMessage());
        }
    }

    public void setLastAndNextCycleRun(long lastMs, long nextMs) {
        this.lastCycleRunDisplay = formatMillis(lastMs);
        this.nextCycleRunDisplay = formatMillis(nextMs);
        try {
            var belasting = rootNode.node("belasting");
            belasting.node("last-cycle-run").set(lastCycleRunDisplay);
            belasting.node("next-cycle-run").set(nextCycleRunDisplay);
            saveConfiguration();
        } catch (SerializationException e) {
            nl.openminetopia.OpenMinetopia.getInstance().getLogger().warning("Belasting: kon cycle-run niet opslaan: " + e.getMessage());
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
