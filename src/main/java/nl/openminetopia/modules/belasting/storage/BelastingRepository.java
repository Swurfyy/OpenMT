package nl.openminetopia.modules.belasting.storage;

import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.modules.belasting.enums.InvoiceStatus;
import nl.openminetopia.modules.belasting.models.TaxExclusionModel;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.modules.belasting.models.TaxInvoicePlotModel;
import nl.openminetopia.modules.data.storm.StormDatabase;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BelastingRepository {

    public CompletableFuture<TaxInvoiceModel> saveInvoice(TaxInvoiceModel invoice, List<TaxInvoicePlotModel> plotRows) {
        CompletableFuture<TaxInvoiceModel> future = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Integer id = StormDatabase.getInstance().saveStormModel(invoice).join();
                if (id != null && id > 0) {
                    invoice.setId(id);
                    for (TaxInvoicePlotModel row : plotRows) {
                        row.setInvoiceId(id);
                        StormDatabase.getInstance().saveStormModel(row).join();
                    }
                }
                future.complete(invoice);
            } catch (Exception e) {
                future.completeExceptionally(e);
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
