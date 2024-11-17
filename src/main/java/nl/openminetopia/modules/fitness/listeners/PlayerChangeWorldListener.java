package nl.openminetopia.modules.fitness.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.places.PlacesModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangeWorldListener implements Listener {

    @EventHandler
    public void worldChange(final PlayerChangedWorldEvent event) {
        PlacesModule placesModule = OpenMinetopia.getModuleManager().getModule(PlacesModule.class);
        if (placesModule.getWorld(event.getFrom().getSpawnLocation()) != null) return;
        PlayerManager.getInstance().getMinetopiaPlayerAsync(event.getPlayer(), minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;
            minetopiaPlayer.getFitness().getRunnable().run();
        }, Throwable::printStackTrace);
    }
}
