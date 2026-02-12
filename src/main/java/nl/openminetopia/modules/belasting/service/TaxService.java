package nl.openminetopia.modules.belasting.service;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.calculator.PlotTaxCalculator;
import nl.openminetopia.modules.belasting.calculator.PlotTaxEntry;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.configuration.BelastingScheduleConfiguration;
import nl.openminetopia.modules.belasting.configuration.CycleWindow;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class TaxService {

    private final BelastingConfiguration config;
    private final BelastingScheduleConfiguration schedule;
    private final BelastingRepository repository;
    private final PlotTaxCalculator calculator;
    private final Set<UUID> excludedCache = ConcurrentHashMap.newKeySet();
    private volatile long lastExclusionRefresh;

    private static final long EXCLUSION_CACHE_MS = 60_000;

    public TaxService(BelastingConfiguration config, BelastingScheduleConfiguration schedule, BelastingRepository repository, PlotTaxCalculator calculator) {
        this.config = config;
        this.schedule = schedule;
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
        return CompletableFuture.runAsync(() -> {
            long now = System.currentTimeMillis();
            CycleWindow current = schedule != null ? schedule.getCurrentCycle(now) : null;
            if (current == null) {
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Geen actieve cycle gevonden, skip belasting run.");
                return;
            }
            long lastRun = config.getLastCycleRunMillis();
            if (lastRun == current.startMs()) {
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Cycle al uitgevoerd voor deze periode, skip.");
                return;
            }

            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start belasting cycle verwerking...");
            long startTime = System.currentTimeMillis();
            
            refreshExclusionCacheIfNeeded();
            
            // Single-pass scan: collect all owner UUIDs and their plot data at once
            Map<UUID, List<PlotTaxEntry>> ownerPlotCache = buildOwnerPlotCache();
            Set<UUID> ownerUuids = ownerPlotCache.keySet();
            
            long cycleStart = current.startMs();
            long nextStart = schedule.getNextCycleStart(now);

            OpenMinetopia.getInstance().getLogger().info("[Belasting] Gevonden " + ownerUuids.size() + " plot eigenaren om te verwerken.");

            if (ownerUuids.isEmpty()) {
                config.setLastAndNextCycleRun(cycleStart, nextStart);
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Geen eigenaren gevonden, cycle voltooid.");
                return;
            }

            // Batch query: get all unpaid invoices at once
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start batch query voor " + ownerUuids.size() + " eigenaren...");
            long queryStartTime = System.currentTimeMillis();
            Map<UUID, TaxInvoiceModel> unpaidInvoices = repository.getUnpaidInvoicesBatch(ownerUuids).join();
            long queryDuration = System.currentTimeMillis() - queryStartTime;
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Batch query voltooid in " + queryDuration + "ms, gevonden " + unpaidInvoices.size() + " unpaid invoices.");
            
            // Process in batches to avoid thread pool exhaustion
            final int[] counters = {0, 0, 0}; // processed, skipped, created
            final int BATCH_SIZE = 50; // Process 50 owners at a time
            
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start verwerking van " + ownerUuids.size() + " eigenaren in batches van " + BATCH_SIZE + "...");
            
            List<UUID> ownerList = new ArrayList<>(ownerUuids);
            for (int i = 0; i < ownerList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, ownerList.size());
                List<UUID> batch = ownerList.subList(i, end);
                
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Verwerk batch " + (i / BATCH_SIZE + 1) + " (" + batch.size() + " eigenaren)...");
                long batchStartTime = System.currentTimeMillis();
                
                List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
                
                for (UUID owner : batch) {
                    CompletableFuture<Void> step = processOwnerForCycle(owner, ownerPlotCache.get(owner), unpaidInvoices.get(owner), counters);
                    batchFutures.add(step);
                }
                
                // Wait for batch to complete before processing next batch (with timeout)
                try {
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                            .get(120, TimeUnit.SECONDS); // 2 minute timeout per batch
                } catch (TimeoutException e) {
                    OpenMinetopia.getInstance().getLogger().severe("[Belasting] Batch " + (i / BATCH_SIZE + 1) + " timeout na 120 seconden! Skip deze batch en ga verder.");
                    // Cancel remaining futures
                    batchFutures.forEach(f -> f.cancel(true));
                } catch (Exception e) {
                    OpenMinetopia.getInstance().getLogger().severe("[Belasting] Batch " + (i / BATCH_SIZE + 1) + " gefaald: " + e.getMessage());
                    e.printStackTrace();
                }
                
                long batchDuration = System.currentTimeMillis() - batchStartTime;
                counters[0] += batch.size();
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Batch " + (i / BATCH_SIZE + 1) + " voltooid in " + batchDuration + "ms. Totaal verwerkt: " + counters[0] + "/" + ownerUuids.size() + " eigenaren (aangemaakt: " + counters[2] + ", overgeslagen: " + counters[1] + ")");
            }

            long duration = System.currentTimeMillis() - startTime;
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Cycle voltooid! " + counters[2] + " nieuwe facturen aangemaakt, " + counters[1] + " overgeslagen. Duur: " + duration + "ms");
            config.setLastAndNextCycleRun(cycleStart, nextStart);
        }).exceptionally(ex -> {
            OpenMinetopia.getInstance().getLogger().severe("[Belasting] Fout tijdens cycle verwerking: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }
    
    /**
     * Process a single owner for the cycle
     */
    private CompletableFuture<Void> processOwnerForCycle(UUID owner, List<PlotTaxEntry> plots, TaxInvoiceModel existingInvoice, int[] counters) {
        return isExcluded(owner).thenCompose(excluded -> {
            if (excluded) {
                synchronized (counters) { counters[1]++; }
                return CompletableFuture.completedFuture(null);
            }
            
            if (existingInvoice != null) {
                synchronized (counters) { counters[1]++; }
                return CompletableFuture.completedFuture(null);
            }
            
            if (plots == null || plots.isEmpty()) {
                synchronized (counters) { counters[1]++; }
                return CompletableFuture.completedFuture(null);
            }
            
            synchronized (counters) { counters[2]++; }
            return createInvoiceFor(owner, plots);
        });
    }

    public CompletableFuture<Integer> runCycleForced() {
        return CompletableFuture.supplyAsync(() -> {
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start geforceerde belasting cycle simulatie...");
            long startTime = System.currentTimeMillis();
            
            long now = System.currentTimeMillis();
            refreshExclusionCacheIfNeeded();
            
            // Single-pass scan: collect all owner UUIDs and their plot data at once
            Map<UUID, List<PlotTaxEntry>> ownerPlotCache = buildOwnerPlotCache();
            Set<UUID> ownerUuids = ownerPlotCache.keySet();
            
            long cs = schedule != null ? schedule.getCurrentCycleStart(now) : 0;
            final long nextStart = schedule != null ? schedule.getNextCycleStart(now) : 0;

            OpenMinetopia.getInstance().getLogger().info("[Belasting] Gevonden " + ownerUuids.size() + " plot eigenaren om te verwerken.");

            if (ownerUuids.isEmpty()) {
                if (cs > 0) config.setLastAndNextCycleRun(now, nextStart);
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Geen eigenaren gevonden, simulatie voltooid.");
                return 0;
            }
            
            final long cycleStart = cs > 0 ? cs : now;
            
            // Batch query: get all unpaid invoices at once
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start batch query voor " + ownerUuids.size() + " eigenaren...");
            long queryStartTime = System.currentTimeMillis();
            Map<UUID, TaxInvoiceModel> unpaidInvoices;
            try {
                unpaidInvoices = repository.getUnpaidInvoicesBatch(ownerUuids)
                        .get(60, TimeUnit.SECONDS); // 60 second timeout to prevent infinite blocking
            } catch (TimeoutException e) {
                OpenMinetopia.getInstance().getLogger().severe("[Belasting] Batch query timeout na 60 seconden! Mogelijk thread pool exhaustion of database probleem.");
                OpenMinetopia.getInstance().getLogger().severe("[Belasting] Gebruik lege map als fallback om verder te gaan.");
                unpaidInvoices = Collections.emptyMap();
            } catch (Exception e) {
                OpenMinetopia.getInstance().getLogger().severe("[Belasting] Batch query gefaald: " + e.getMessage());
                e.printStackTrace();
                unpaidInvoices = Collections.emptyMap();
            }
            long queryDuration = System.currentTimeMillis() - queryStartTime;
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Batch query voltooid in " + queryDuration + "ms, gevonden " + unpaidInvoices.size() + " unpaid invoices.");
            
            // Process in batches to avoid thread pool exhaustion
            final int[] counters = {0, 0, 0}; // processed, skipped, created
            final int BATCH_SIZE = 50; // Process 50 owners at a time
            
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start verwerking van " + ownerUuids.size() + " eigenaren in batches van " + BATCH_SIZE + "...");
            
            List<UUID> ownerList = new ArrayList<>(ownerUuids);
            for (int i = 0; i < ownerList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, ownerList.size());
                List<UUID> batch = ownerList.subList(i, end);
                
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Verwerk batch " + (i / BATCH_SIZE + 1) + " (" + batch.size() + " eigenaren)...");
                long batchStartTime = System.currentTimeMillis();
                
                List<CompletableFuture<Integer>> batchFutures = new ArrayList<>();
                
                for (UUID owner : batch) {
                    CompletableFuture<Integer> step = processOwnerForCycleForced(owner, ownerPlotCache.get(owner), unpaidInvoices.get(owner), counters);
                    batchFutures.add(step);
                }
                
                // Wait for batch to complete before processing next batch (with timeout)
                try {
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                            .get(120, TimeUnit.SECONDS); // 2 minute timeout per batch
                } catch (TimeoutException e) {
                    OpenMinetopia.getInstance().getLogger().severe("[Belasting] Batch " + (i / BATCH_SIZE + 1) + " timeout na 120 seconden! Skip deze batch en ga verder.");
                    // Cancel remaining futures
                    batchFutures.forEach(f -> f.cancel(true));
                } catch (Exception e) {
                    OpenMinetopia.getInstance().getLogger().severe("[Belasting] Batch " + (i / BATCH_SIZE + 1) + " gefaald: " + e.getMessage());
                    e.printStackTrace();
                }
                
                long batchDuration = System.currentTimeMillis() - batchStartTime;
                counters[0] += batch.size();
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Batch " + (i / BATCH_SIZE + 1) + " voltooid in " + batchDuration + "ms. Totaal verwerkt: " + counters[0] + "/" + ownerUuids.size() + " eigenaren (aangemaakt: " + counters[2] + ", overgeslagen: " + counters[1] + ")");
            }
            
            int count = counters[2];
            long duration = System.currentTimeMillis() - startTime;
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Simulatie voltooid! " + counters[2] + " nieuwe facturen aangemaakt, " + counters[1] + " overgeslagen. Duur: " + duration + "ms");
            
            if (schedule != null && schedule.getCycleByStart(cycleStart) != null) {
                config.setLastAndNextCycleRun(cycleStart, nextStart);
            }
            return count;
        }).exceptionally(ex -> {
            OpenMinetopia.getInstance().getLogger().severe("[Belasting] Fout tijdens geforceerde cycle simulatie: " + ex.getMessage());
            ex.printStackTrace();
            return 0;
        });
    }
    
    /**
     * Process a single owner for forced cycle
     */
    private CompletableFuture<Integer> processOwnerForCycleForced(UUID owner, List<PlotTaxEntry> plots, TaxInvoiceModel existingInvoice, int[] counters) {
        return isExcluded(owner).thenCompose(excluded -> {
            if (excluded) {
                synchronized (counters) { counters[1]++; }
                return CompletableFuture.completedFuture(0);
            }
            
            if (existingInvoice != null) {
                synchronized (counters) { counters[1]++; }
                return CompletableFuture.completedFuture(0);
            }
            
            if (plots == null || plots.isEmpty()) {
                synchronized (counters) { counters[1]++; }
                return CompletableFuture.completedFuture(0);
            }
            
            synchronized (counters) { counters[2]++; }
            return createInvoiceFor(owner, plots).thenApply(v -> 1);
        });
    }

    /**
     * Single-pass scan: collect all owner UUIDs and their plot data at once.
     * This eliminates the need to scan regions multiple times (once per owner).
     * Returns a map of owner UUID -> list of their taxable plots.
     */
    private Map<UUID, List<PlotTaxEntry>> buildOwnerPlotCache() {
        Map<UUID, List<PlotTaxEntry>> ownerPlotCache = new ConcurrentHashMap<>();
        com.sk89q.worldguard.protection.regions.RegionContainer container =
                com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) {
            OpenMinetopia.getInstance().getLogger().warning("[Belasting] WorldGuard container niet gevonden!");
            return ownerPlotCache;
        }

        int worldCount = 0;
        int regionCount = 0;
        int plotCount = 0;
        
        for (World world : Bukkit.getWorlds()) {
            worldCount++;
            var manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            if (manager == null) continue;
            
            for (var region : manager.getRegions().values()) {
                regionCount++;
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
                
                // Calculate tax for this plot
                double taxAmount = calculator.computeTaxForWoz(woz);
                PlotTaxEntry entry = new PlotTaxEntry(world.getName(), region.getId(), woz, taxAmount);
                
                // Add this plot to all owners
                for (UUID ownerUuid : region.getOwners().getUniqueIds()) {
                    ownerPlotCache.computeIfAbsent(ownerUuid, k -> new ArrayList<>()).add(entry);
                    plotCount++;
                }
            }
        }
        
        OpenMinetopia.getInstance().getLogger().info("[Belasting] Single-pass scan: " + worldCount + " worlds, " + regionCount + " regions, " + plotCount + " plot entries voor " + ownerPlotCache.size() + " unieke eigenaren.");
        return ownerPlotCache;
    }
    
    /**
     * Legacy method kept for backwards compatibility
     * @deprecated Use buildOwnerPlotCache() instead for better performance
     */
    @Deprecated
    private Set<UUID> collectOwnerUuids() {
        return buildOwnerPlotCache().keySet();
    }

    /**
     * Create invoice for a player using pre-computed plot entries (from cache)
     */
    private CompletableFuture<Void> createInvoiceFor(UUID playerUuid, List<PlotTaxEntry> entries) {
        if (entries == null || entries.isEmpty()) return CompletableFuture.completedFuture(null);

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
    
    /**
     * Legacy method: create invoice by scanning regions again (slower)
     * @deprecated Use createInvoiceFor(UUID, List<PlotTaxEntry>) with cached data instead
     */
    @Deprecated
    private CompletableFuture<Void> createInvoiceFor(UUID playerUuid) {
        List<PlotTaxEntry> entries = calculator.computeTaxablePlots(playerUuid);
        return createInvoiceFor(playerUuid, entries);
    }

    public CompletableFuture<List<TaxInvoiceModel>> getAllInvoices() {
        return repository.getAllInvoices();
    }

    /**
     * Returns only invoices from the last run cycle (the cycle whose start is last-cycle-run).
     * Used by admin commands so they show only the latest cycle.
     */
    public CompletableFuture<List<TaxInvoiceModel>> getInvoicesForLastCycle() {
        long lastRun = config.getLastCycleRunMillis();
        if (lastRun <= 0 || schedule == null) return CompletableFuture.completedFuture(List.of());
        CycleWindow cycle = schedule.getCycleByStart(lastRun);
        if (cycle == null) return CompletableFuture.completedFuture(List.of());
        return repository.getAllInvoices().thenApply(invoices ->
                invoices.stream()
                        .filter(inv -> inv.getCreatedAt() != null
                                && inv.getCreatedAt() >= cycle.startMs()
                                && inv.getCreatedAt() <= cycle.endMs())
                        .toList());
    }

    public CompletableFuture<List<TaxInvoicePlotModel>> getPlotRows(int invoiceId) {
        return repository.getPlotRows(invoiceId);
    }

    public CompletableFuture<Void> markPaid(TaxInvoiceModel invoice) {
        return repository.markPaid(invoice).thenRun(() -> {
            // Invalidate login cache when invoice is paid
            if (invoice.getPlayerUuid() != null) {
                invalidateLoginCache(invoice.getPlayerUuid());
            }
        });
    }
    
    /**
     * Invalidate login cache for a player (called when invoice is paid)
     */
    private void invalidateLoginCache(UUID playerUuid) {
        // Access the listener through BelastingModule
        try {
            nl.openminetopia.modules.belasting.BelastingModule module = 
                    nl.openminetopia.OpenMinetopia.getModuleManager().get(nl.openminetopia.modules.belasting.BelastingModule.class);
            if (module != null) {
                module.invalidateLoginCache(playerUuid);
            }
        } catch (Exception e) {
            // Silently fail if module not found
        }
    }

    public CompletableFuture<Void> addExclusion(UUID playerUuid, long expiresAt) {
        addToExclusionCache(playerUuid);
        return repository.getExclusion(playerUuid)
                .thenCompose(existing -> {
                    if (existing != null) {
                        existing.setExpiresAt(expiresAt);
                        return repository.saveExclusion(existing);
                    }
                    TaxExclusionModel model = new TaxExclusionModel();
                    model.setPlayerUuid(playerUuid);
                    model.setExpiresAt(expiresAt);
                    return repository.saveExclusion(model);
                });
    }

    public CompletableFuture<Void> cleanupExpiredExclusions() {
        return repository.deleteExpiredExclusions();
    }
}
