package nl.openminetopia.modules.player;

import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.utils.modules.ExtendedSpigotModule;
import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import lombok.Getter;
import lombok.Setter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.player.commands.PlaytimeCommand;
import nl.openminetopia.modules.player.listeners.PlayerJoinListener;
import nl.openminetopia.modules.player.listeners.PlayerPreLoginListener;
import nl.openminetopia.modules.player.listeners.PlayerQuitListener;
import nl.openminetopia.modules.player.models.PlayerModel;

import nl.openminetopia.modules.player.runnables.MinetopiaPlayerSaveRunnable;
import nl.openminetopia.modules.player.runnables.PlayerPlaytimeRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Setter @Getter
public class PlayerModule extends ExtendedSpigotModule {

    public PlayerModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager, DataModule dataModule) {
        super(moduleManager);
    }


    private MinetopiaPlayerSaveRunnable minetopiaPlayerSaveRunnable;
    private PlayerPlaytimeRunnable playerPlaytimeRunnable;

    @Override
    public void onEnable() {
        registerComponent(new PlayerPreLoginListener());
        registerComponent(new PlayerJoinListener());
        registerComponent(new PlayerQuitListener());

        registerComponent(new PlaytimeCommand());

        minetopiaPlayerSaveRunnable = new MinetopiaPlayerSaveRunnable(PlayerManager.getInstance(), 5 * 60 * 1000L, 50, 30 * 60 * 1000L, () -> new ArrayList<>(PlayerManager.getInstance().getOnlinePlayers().keySet()), true);
        OpenMinetopia.getInstance().registerDirtyPlayerRunnable(minetopiaPlayerSaveRunnable, 20L * 5);

        playerPlaytimeRunnable = new PlayerPlaytimeRunnable(PlayerManager.getInstance(), 1000L * 5, 50, 20 * 1000L, () -> new ArrayList<>(PlayerManager.getInstance().getOnlinePlayers().keySet()), true);
        OpenMinetopia.getInstance().registerDirtyPlayerRunnable(playerPlaytimeRunnable, 20L);

    }

    @Override
    public void onDisable() {
        // First, try to get already loaded players (non-blocking)
        List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Try to get already loaded player first (non-blocking)
            MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
            
            if (minetopiaPlayer == null) {
                // If not loaded, try to load with timeout (non-blocking)
                try {
                    minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player)
                            .get(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    getLogger().warn("Could not load player data for " + player.getName() + " during shutdown: " + e.getMessage());
                    continue;
                }
            }
            
            if (minetopiaPlayer == null) continue;
            
            // Update playtime
            minetopiaPlayer.updatePlaytime();
            
            // Save asynchronously with timeout
            CompletableFuture<Void> saveFuture = minetopiaPlayer.save()
                    .orTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        getLogger().warn("Could not save player data for " + player.getName() + " during shutdown: " + throwable.getMessage());
                        return null;
                    });
            saveFutures.add(saveFuture);
        }
        
        // Wait for all saves to complete (with timeout)
        try {
            CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            getLogger().warn("Some player data could not be saved during shutdown: " + e.getMessage());
        }
        
        OpenMinetopia.getInstance().unregisterDirtyPlayerRunnable(minetopiaPlayerSaveRunnable);
        OpenMinetopia.getInstance().unregisterDirtyPlayerRunnable(playerPlaytimeRunnable);
    }

    private CompletableFuture<Optional<PlayerModel>> findPlayerModel(@NotNull UUID uuid) {
        CompletableFuture<Optional<PlayerModel>> completableFuture = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<PlayerModel> playerModel = StormDatabase.getInstance().getStorm().buildQuery(PlayerModel.class)
                        .where("uuid", Where.EQUAL, uuid.toString())
                        .limit(1).execute().join();

                Bukkit.getScheduler().runTaskLaterAsynchronously(OpenMinetopia.getInstance(), () -> completableFuture.complete(playerModel.stream().findFirst()), 1L);
            } catch (Exception exception) {
                exception.printStackTrace();
                completableFuture.completeExceptionally(exception);
            }
        });
        return completableFuture;
    }

    public CompletableFuture<PlayerModel> getPlayerModel(UUID uuid) {
        CompletableFuture<PlayerModel> future = new CompletableFuture<>();

        findPlayerModel(uuid).thenAccept(playerModel -> {
            if (playerModel.isEmpty()) {
                PlayerModel createdModel = new PlayerModel();
                createdModel.setUniqueId(uuid);
                createdModel.setPlaytime(0L);
                createdModel.setActivePrefixId(-1);
                createdModel.setActivePrefixColorId(-1);
                createdModel.setActiveChatColorId(-1);
                createdModel.setActiveNameColorId(-1);
                createdModel.setActiveLevelColorId(-1);
                createdModel.setStaffchatEnabled(false);
                createdModel.setCommandSpyEnabled(false);
                createdModel.setChatSpyEnabled(false);
                createdModel.setPrefixes(new ArrayList<>());
                createdModel.setColors(new ArrayList<>());
                future.complete(createdModel);

                StormDatabase.getInstance().saveStormModel(createdModel);
                return;
            }

            future.complete(playerModel.get());
        });

        return future;
    }
}
