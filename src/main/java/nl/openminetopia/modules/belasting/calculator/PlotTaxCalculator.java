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

                entries.add(new PlotTaxEntry(world.getName(), region.getId(), woz, 0.0));
            }
        }
        int ownedPlots = entries.size();
        List<PlotTaxEntry> out = new ArrayList<>(entries.size());
        for (PlotTaxEntry entry : entries) {
            out.add(new PlotTaxEntry(
                    entry.getWorldName(),
                    entry.getPlotId(),
                    entry.getWozValue(),
                    computeTaxForWoz(entry.getWozValue(), ownedPlots)
            ));
        }
        return out;
    }

    public double computeTaxForWoz(long woz) {
        return computeTaxForWoz(woz, 1);
    }

    public double computeTaxForWoz(long woz, int ownedPlots) {
        if (woz <= 0) return 0.0;
        double taxValue = config.getTaxValueForPlotCount(Math.max(ownedPlots, 1));
        if (config.isTaxPercentage()) {
            return woz * (taxValue / 100.0);
        }
        return woz * taxValue;
    }

    public double totalTax(List<PlotTaxEntry> entries) {
        return totalTax(entries, entries != null ? entries.size() : 0);
    }

    public double totalTax(List<PlotTaxEntry> entries, int ownedPlots) {
        if (entries == null || entries.isEmpty()) return 0.0;
        final int count = Math.max(ownedPlots, 1);
        return entries.stream().mapToDouble(e -> computeTaxForWoz(e.getWozValue(), count)).sum();
    }

    /**
     * Counts only taxable plots (WOZ > 0). Empty/invalid WOZ plots should never influence bracket selection.
     */
    public int countTaxablePlots(List<PlotTaxEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        return (int) entries.stream().filter(e -> e.getWozValue() > 0).count();
    }
}
