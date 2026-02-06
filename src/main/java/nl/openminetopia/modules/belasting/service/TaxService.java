package nl.openminetopia.modules.belasting.service;

import nl.openminetopia.modules.belasting.calculator.PlotTaxCalculator;
import nl.openminetopia.modules.belasting.calculator.PlotTaxEntry;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.enums.InvoiceStatus;
import nl.openminetopia.modules.belasting.models.TaxExclusionModel;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.modules.belasting.models.TaxInvoicePlotModel;
import nl.openminetopia.modules.belasting.storage.BelastingRepository;
import nl.openminetopia.modules.plots.PlotModule;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TaxService {

    private final BelastingConfiguration config;
    private final BelastingRepository repository;
    private final PlotTaxCalculator calculator;
    private final Set<UUID> excludedCache = ConcurrentHashMap.newKeySet();
    private volatile long lastExclusionRefresh;

    private static final long EXCLUSION_CACHE_MS = 60_000;

    public TaxService(BelastingConfiguration config, BelastingRepository repository, PlotTaxCalculator calculator) {
        this.config = config;
        this.repository = repository;
        this.calculator = calculator;
    }

    public CompletableFuture<Boolean> isExcluded(UUID playerUuid) {
        refreshExclusionCacheIfNeeded();
        if (excludedCache.contains(playerUuid)) return CompletableFuture.completedFuture(true);
        return repository.getExclusion(playerUuid).thenApply(Objects::nonNull);
    }

    public void addToExclusionCache(UUID playerUuid) {
        excludedCache.add(playerUuid);
    }

    public void removeFromExclusionCache(UUID playerUuid) {
        excludedCache.remove(playerUuid);
    }

    private void refreshExclusionCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastExclusionRefresh < EXCLUSION_CACHE_MS) return;
        lastExclusionRefresh = now;
        repository.getActiveExclusions().thenAccept(list -> {
            excludedCache.clear();
            list.forEach(e -> excludedCache.add(e.getPlayerUuid()));
        });
    }

    public CompletableFuture<TaxInvoiceModel> getUnpaidInvoice(UUID playerUuid) {
        return repository.getUnpaidInvoice(playerUuid);
    }

    public CompletableFuture<Boolean> hasUnpaidInvoice(UUID playerUuid) {
        return repository.hasUnpaidInvoice(playerUuid);
    }

    public CompletableFuture<Void> runCycle() {
        long now = System.currentTimeMillis();
        long lastRun = config.getLastCycleRun();
        long intervalMs = (long) config.getTaxIntervalDays() * 24 * 60 * 60 * 1000;
        if (lastRun > 0 && (now - lastRun) < intervalMs) {
            return CompletableFuture.completedFuture(null);
        }

        refreshExclusionCacheIfNeeded();
        Set<UUID> ownerUuids = collectOwnerUuids();
        if (ownerUuids.isEmpty()) {
            config.setLastCycleRun(now);
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (UUID uuid : ownerUuids) {
            UUID owner = uuid;
            CompletableFuture<Void> step = isExcluded(owner)
                    .thenCompose(excluded -> {
                        if (excluded) return CompletableFuture.completedFuture(null);
                        return repository.getUnpaidInvoice(owner).thenCompose(existing -> {
                            if (existing != null) return CompletableFuture.completedFuture(null);
                            return createInvoiceFor(owner);
                        });
                    });
            futures.add(step);
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return all.thenRun(() -> config.setLastCycleRun(now));
    }

    public CompletableFuture<Integer> runCycleForced() {
        long now = System.currentTimeMillis();
        refreshExclusionCacheIfNeeded();
        Set<UUID> ownerUuids = collectOwnerUuids();
        if (ownerUuids.isEmpty()) {
            config.setLastCycleRun(now);
            return CompletableFuture.completedFuture(0);
        }
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (UUID owner : ownerUuids) {
            CompletableFuture<Integer> step = isExcluded(owner)
                    .thenCompose(excluded -> {
                        if (excluded) return CompletableFuture.completedFuture(0);
                        return repository.getUnpaidInvoice(owner).thenCompose(existing -> {
                            if (existing != null) return CompletableFuture.completedFuture(0);
                            return createInvoiceFor(owner).thenApply(v -> 1);
                        });
                    });
            futures.add(step);
        }
        CompletableFuture<Integer> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .mapToInt(f -> f.join() != null ? f.join() : 0)
                        .sum());
        return all.thenApply(count -> {
            config.setLastCycleRun(now);
            return count;
        });
    }

    private Set<UUID> collectOwnerUuids() {
        Set<UUID> out = new HashSet<>();
        com.sk89q.worldguard.protection.regions.RegionContainer container =
                com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return out;

        for (World world : Bukkit.getWorlds()) {
            var manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            if (manager == null) continue;
            for (var region : manager.getRegions().values()) {
                if (region.getFlag(PlotModule.PLOT_FLAG) == null) continue;
                String wozRaw = region.getFlag(PlotModule.PLOT_WOZ);
                if (wozRaw == null || wozRaw.trim().isEmpty()) continue;
                long woz;
                try {
                    woz = Long.parseLong(wozRaw.trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (woz <= 0) continue;
                out.addAll(region.getOwners().getUniqueIds());
            }
        }
        return out;
    }

    private CompletableFuture<Void> createInvoiceFor(UUID playerUuid) {
        List<PlotTaxEntry> entries = calculator.computeTaxablePlots(playerUuid);
        if (entries.isEmpty()) return CompletableFuture.completedFuture(null);

        double total = calculator.totalTax(entries);
        String playerName = Optional.ofNullable(Bukkit.getOfflinePlayer(playerUuid).getName()).orElse(playerUuid.toString());

        TaxInvoiceModel invoice = new TaxInvoiceModel();
        invoice.setPlayerUuid(playerUuid);
        invoice.setPlayerName(playerName);
        invoice.setTotalAmount(total);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setCreatedAt(System.currentTimeMillis());
        invoice.setPaidAt(null);

        List<TaxInvoicePlotModel> rows = entries.stream().map(e -> {
            TaxInvoicePlotModel row = new TaxInvoicePlotModel();
            row.setWorldName(e.getWorldName());
            row.setPlotId(e.getPlotId());
            row.setWozValue(e.getWozValue());
            row.setTaxAmount(e.getTaxAmount());
            return row;
        }).toList();

        return repository.saveInvoice(invoice, rows).thenAccept(v -> {});
    }

    public CompletableFuture<List<TaxInvoiceModel>> getAllInvoices() {
        return repository.getAllInvoices();
    }

    public CompletableFuture<List<TaxInvoicePlotModel>> getPlotRows(int invoiceId) {
        return repository.getPlotRows(invoiceId);
    }

    public CompletableFuture<Void> markPaid(TaxInvoiceModel invoice) {
        return repository.markPaid(invoice);
    }

    public CompletableFuture<Void> addExclusion(UUID playerUuid, long expiresAt) {
        TaxExclusionModel model = new TaxExclusionModel();
        model.setPlayerUuid(playerUuid);
        model.setExpiresAt(expiresAt);
        addToExclusionCache(playerUuid);
        return repository.saveExclusion(model);
    }

    public CompletableFuture<Void> cleanupExpiredExclusions() {
        return repository.deleteExpiredExclusions();
    }
}
