package nl.openminetopia.modules.belasting.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.models.TaxInvoiceModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BelastingLoginListener implements Listener {

    // Cache unpaid invoices for 5 minutes to reduce database queries
    private final Map<UUID, CachedInvoice> invoiceCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(5);

    private static class CachedInvoice {
        final TaxInvoiceModel invoice;
        final long timestamp;

        CachedInvoice(TaxInvoiceModel invoice) {
            this.invoice = invoice;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        if (module == null) return;
        
        BelastingConfiguration config = module.getConfig();
        UUID playerUuid = player.getUniqueId();

        // Check cache first
        CachedInvoice cached = invoiceCache.get(playerUuid);
        if (cached != null && !cached.isExpired()) {
            if (cached.invoice != null) {
                OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                    ChatUtils.sendMessage(player, config.getMessageLoginUnpaid());
                    module.getGuiManager().openPaymentGui(player, cached.invoice);
                });
            }
            return;
        }

        // Query database and cache result
        module.getTaxService().getUnpaidInvoice(playerUuid).thenAccept(invoice -> {
            // Cache the result (even if null, to avoid repeated queries)
            invoiceCache.put(playerUuid, new CachedInvoice(invoice));
            
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                if (invoice == null) return;
                ChatUtils.sendMessage(player, config.getMessageLoginUnpaid());
                module.getGuiManager().openPaymentGui(player, invoice);
            });
        });
    }

    /**
     * Clear cache when invoice is paid (called from TaxService)
     */
    public void invalidateCache(UUID playerUuid) {
        invoiceCache.remove(playerUuid);
    }

    /**
     * Clean up expired cache entries periodically
     */
    public void cleanupExpiredCache() {
        invoiceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
