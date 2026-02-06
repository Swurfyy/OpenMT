package nl.openminetopia.modules.belasting.calculator;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.plots.PlotModule;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlotTaxCalculator {

    private final BelastingConfiguration config;

    public PlotTaxCalculator(BelastingConfiguration config) {
        this.config = config;
    }

    public List<PlotTaxEntry> computeTaxablePlots(UUID playerUuid) {
        List<PlotTaxEntry> entries = new ArrayList<>();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return entries;

        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) continue;

            for (ProtectedRegion region : manager.getRegions().values()) {
                if (region.getFlag(PlotModule.PLOT_FLAG) == null) continue;
                if (!region.getOwners().getUniqueIds().contains(playerUuid)) continue;

                String wozRaw = region.getFlag(PlotModule.PLOT_WOZ);
                if (wozRaw == null || wozRaw.trim().isEmpty()) continue;
                long woz;
                try {
                    woz = Long.parseLong(wozRaw.trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (woz <= 0) continue;

                double taxAmount = computeTaxForWoz(woz);
                entries.add(new PlotTaxEntry(world.getName(), region.getId(), woz, taxAmount));
            }
        }
        return entries;
    }

    public double computeTaxForWoz(long woz) {
        if (config.isTaxPercentage()) {
            return woz * (config.getTaxCalculationValue() / 100.0);
        }
        return woz * config.getTaxCalculationValue();
    }

    public double totalTax(List<PlotTaxEntry> entries) {
        return entries.stream().mapToDouble(PlotTaxEntry::getTaxAmount).sum();
    }
}
