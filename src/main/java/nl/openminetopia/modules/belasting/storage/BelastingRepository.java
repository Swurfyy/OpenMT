package nl.openminetopia.modules.belasting.storage;

import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.modules.belasting.enums.InvoiceStatus;
import nl.openminetopia.modules.belasting.models.TaxExclusionModel;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.modules.belasting.models.TaxInvoicePlotModel;
import nl.openminetopia.modules.data.storm.StormDatabase;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BelastingRepository {

    public CompletableFuture<TaxInvoiceModel> saveInvoice(TaxInvoiceModel invoice, List<TaxInvoicePlotModel> plotRows) {
        CompletableFuture<TaxInvoiceModel> future = new CompletableFuture<>();
        
        // Save invoice first, then save plot rows
        StormDatabase.getInstance().saveStormModel(invoice)
                .thenCompose(id -> {
                    if (id == null || id <= 0) {
                        return CompletableFuture.completedFuture(invoice);
                    }
                    
                    invoice.setId(id);
                    
                    // Save all plot rows sequentially to avoid overwhelming the database
                    CompletableFuture<Void> saveRowsFuture = CompletableFuture.completedFuture(null);
                    for (TaxInvoicePlotModel row : plotRows) {
                        row.setInvoiceId(id);
                        saveRowsFuture = saveRowsFuture.thenCompose(v -> 
                            StormDatabase.getInstance().saveStormModel(row).thenApply(savedId -> null)
                        );
                    }
                    
                    return saveRowsFuture.thenApply(v -> invoice);
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(result);
                    }
                });
        
        return future;
    }

    public CompletableFuture<TaxInvoiceModel> getUnpaidInvoice(UUID playerUuid) {
        CompletableFuture<TaxInvoiceModel> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<TaxInvoiceModel> list = StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxInvoiceModel.class)
                        .where("player_uuid", Where.EQUAL, playerUuid.toString())
                        .where("status", Where.EQUAL, InvoiceStatus.UNPAID.toString())
                        .limit(1)
                        .execute().join();
                future.complete(list.isEmpty() ? null : list.iterator().next());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<List<TaxInvoicePlotModel>> getPlotRows(int invoiceId) {
        CompletableFuture<List<TaxInvoicePlotModel>> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<TaxInvoicePlotModel> list = StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxInvoicePlotModel.class)
                        .where("invoice_id", Where.EQUAL, invoiceId)
                        .execute().join();
                future.complete(List.copyOf(list));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<List<TaxInvoiceModel>> getAllInvoices() {
        CompletableFuture<List<TaxInvoiceModel>> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<TaxInvoiceModel> list = StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxInvoiceModel.class)
                        .execute().join();
                future.complete(List.copyOf(list));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> markPaid(TaxInvoiceModel invoice) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidAt(System.currentTimeMillis());
                StormDatabase.getInstance().saveStormModel(invoice);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> hasUnpaidInvoice(UUID playerUuid) {
        return getUnpaidInvoice(playerUuid).thenApply(inv -> inv != null);
    }

    /**
     * Batch query to get all unpaid invoices for multiple players at once.
     * This significantly reduces database queries from N queries to 1 query.
     * Uses CompletableFuture chain to avoid nested .join() calls that can cause deadlocks.
     */
    public CompletableFuture<Map<UUID, TaxInvoiceModel>> getUnpaidInvoicesBatch(Set<UUID> playerUuids) {
        if (playerUuids.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        CompletableFuture<Map<UUID, TaxInvoiceModel>> future = new CompletableFuture<>();
        
        // Execute query in executor service to avoid blocking
        StormDatabase.getExecutorService().submit(() -> {
            try {
                // Use CompletableFuture chain instead of nested .join() to avoid deadlocks
                StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxInvoiceModel.class)
                        .where("status", Where.EQUAL, InvoiceStatus.UNPAID.toString())
                        .execute()
                        .thenApply(allUnpaid -> {
                            // Filter to only requested UUIDs and create map
                            Map<UUID, TaxInvoiceModel> result = new java.util.HashMap<>();
                            for (TaxInvoiceModel invoice : allUnpaid) {
                                if (playerUuids.contains(invoice.getPlayerUuid())) {
                                    // Only keep the first unpaid invoice per player (if multiple exist)
                                    result.putIfAbsent(invoice.getPlayerUuid(), invoice);
                                }
                            }
                            return result;
                        })
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                // Log error but return empty map to allow processing to continue
                                nl.openminetopia.OpenMinetopia.getInstance().getLogger().severe("[Belasting] Fout tijdens batch query voor unpaid invoices: " + ex.getMessage());
                                ex.printStackTrace();
                                future.complete(Collections.emptyMap());
                            } else {
                                future.complete(result);
                            }
                        });
            } catch (Exception e) {
                nl.openminetopia.OpenMinetopia.getInstance().getLogger().severe("[Belasting] Fout tijdens batch query setup: " + e.getMessage());
                e.printStackTrace();
                future.complete(Collections.emptyMap());
            }
        });
        
        return future;
    }

    public CompletableFuture<Void> saveExclusion(TaxExclusionModel exclusion) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                StormDatabase.getInstance().saveStormModel(exclusion);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<List<TaxExclusionModel>> getActiveExclusions() {
        CompletableFuture<List<TaxExclusionModel>> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<TaxExclusionModel> list = StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxExclusionModel.class)
                        .execute().join();
                long now = System.currentTimeMillis();
                List<TaxExclusionModel> active = list.stream()
                        .filter(e -> e.getExpiresAt() != null && e.getExpiresAt() > now)
                        .toList();
                future.complete(active);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<TaxExclusionModel> getExclusion(UUID playerUuid) {
        CompletableFuture<TaxExclusionModel> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<TaxExclusionModel> list = StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxExclusionModel.class)
                        .where("player_uuid", Where.EQUAL, playerUuid.toString())
                        .execute().join();
                long now = System.currentTimeMillis();
                TaxExclusionModel found = list.stream()
                        .filter(e -> e.getExpiresAt() != null && e.getExpiresAt() > now)
                        .findFirst()
                        .orElse(null);
                future.complete(found);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteExpiredExclusions() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<TaxExclusionModel> list = StormDatabase.getInstance().getStorm()
                        .buildQuery(TaxExclusionModel.class)
                        .execute().join();
                long now = System.currentTimeMillis();
                for (TaxExclusionModel e : list) {
                    if (e.getExpiresAt() != null && e.getExpiresAt() <= now) {
                        StormDatabase.getInstance().getStorm().delete(e);
                    }
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
