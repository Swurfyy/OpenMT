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
            Set<UUID> ownerUuids = collectOwnerUuids();
            long cycleStart = current.startMs();
            long nextStart = schedule.getNextCycleStart(now);

            OpenMinetopia.getInstance().getLogger().info("[Belasting] Gevonden " + ownerUuids.size() + " plot eigenaren om te verwerken.");

            if (ownerUuids.isEmpty()) {
                config.setLastAndNextCycleRun(cycleStart, nextStart);
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Geen eigenaren gevonden, cycle voltooid.");
                return;
            }

            List<CompletableFuture<?>> futures = new ArrayList<>();
            final int[] counters = {0, 0, 0}; // processed, skipped, created
            
            for (UUID uuid : ownerUuids) {
                UUID owner = uuid;
                CompletableFuture<Void> step = isExcluded(owner)
                        .thenCompose(excluded -> {
                            if (excluded) {
                                synchronized (counters) { counters[1]++; }
                                return CompletableFuture.completedFuture(null);
                            }
                            return repository.getUnpaidInvoice(owner).thenCompose(existing -> {
                                if (existing != null) {
                                    synchronized (counters) { counters[1]++; }
                                    return CompletableFuture.completedFuture(null);
                                }
                                synchronized (counters) { counters[2]++; }
                                return createInvoiceFor(owner);
                            });
                        });
                futures.add(step);
                synchronized (counters) {
                    counters[0]++;
                    // Log progress every 50 owners
                    if (counters[0] % 50 == 0) {
                        OpenMinetopia.getInstance().getLogger().info("[Belasting] Verwerkt " + counters[0] + "/" + ownerUuids.size() + " eigenaren...");
                    }
                }
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            long duration = System.currentTimeMillis() - startTime;
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Cycle voltooid! " + counters[2] + " nieuwe facturen aangemaakt, " + counters[1] + " overgeslagen. Duur: " + duration + "ms");
            config.setLastAndNextCycleRun(cycleStart, nextStart);
        }).exceptionally(ex -> {
            OpenMinetopia.getInstance().getLogger().severe("[Belasting] Fout tijdens cycle verwerking: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    public CompletableFuture<Integer> runCycleForced() {
        return CompletableFuture.supplyAsync(() -> {
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Start geforceerde belasting cycle simulatie...");
            long startTime = System.currentTimeMillis();
            
            long now = System.currentTimeMillis();
            refreshExclusionCacheIfNeeded();
            Set<UUID> ownerUuids = collectOwnerUuids();
            long cs = schedule != null ? schedule.getCurrentCycleStart(now) : 0;
            final long nextStart = schedule != null ? schedule.getNextCycleStart(now) : 0;

            OpenMinetopia.getInstance().getLogger().info("[Belasting] Gevonden " + ownerUuids.size() + " plot eigenaren om te verwerken.");

            if (ownerUuids.isEmpty()) {
                if (cs > 0) config.setLastAndNextCycleRun(now, nextStart);
                OpenMinetopia.getInstance().getLogger().info("[Belasting] Geen eigenaren gevonden, simulatie voltooid.");
                return 0;
            }
            final long cycleStart = cs > 0 ? cs : now;
            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            final int[] counters = {0, 0, 0}; // processed, skipped, created
            
            for (UUID owner : ownerUuids) {
                CompletableFuture<Integer> step = isExcluded(owner)
                        .thenCompose(excluded -> {
                            if (excluded) {
                                synchronized (counters) { counters[1]++; }
                                return CompletableFuture.completedFuture(0);
                            }
                            return repository.getUnpaidInvoice(owner).thenCompose(existing -> {
                                if (existing != null) {
                                    synchronized (counters) { counters[1]++; }
                                    return CompletableFuture.completedFuture(0);
                                }
                                synchronized (counters) { counters[2]++; }
                                return createInvoiceFor(owner).thenApply(v -> 1);
                            });
                        });
                futures.add(step);
                synchronized (counters) {
                    counters[0]++;
                    // Log progress every 50 owners
                    if (counters[0] % 50 == 0) {
                        OpenMinetopia.getInstance().getLogger().info("[Belasting] Verwerkt " + counters[0] + "/" + ownerUuids.size() + " eigenaren...");
                    }
                }
            }
            
            CompletableFuture<Integer> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .mapToInt(f -> f.join() != null ? f.join() : 0)
                            .sum());
            
            int count = all.join();
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

    private Set<UUID> collectOwnerUuids() {
        Set<UUID> out = new HashSet<>();
        com.sk89q.worldguard.protection.regions.RegionContainer container =
                com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) {
            OpenMinetopia.getInstance().getLogger().warning("[Belasting] WorldGuard container niet gevonden!");
            return out;
        }

        int worldCount = 0;
        int regionCount = 0;
        
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
                out.addAll(region.getOwners().getUniqueIds());
            }
        }
        
        OpenMinetopia.getInstance().getLogger().info("[Belasting] Scanned " + worldCount + " worlds, " + regionCount + " regions, gevonden " + out.size() + " unieke eigenaren.");
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
        return repository.markPaid(invoice);
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
