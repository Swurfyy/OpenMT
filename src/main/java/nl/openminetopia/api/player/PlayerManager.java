package nl.openminetopia.api.player;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.player.PlayerModule;
import nl.openminetopia.modules.player.models.PlayerModel;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class PlayerManager {

    private static PlayerManager instance;

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    private final PlayerModule playerModule = OpenMinetopia.getModuleManager().getModule(PlayerModule.class);
    private final Map<UUID, MinetopiaPlayer> onlinePlayers = new ConcurrentHashMap<>();


    public CompletableFuture<MinetopiaPlayer> getMinetopiaPlayer(OfflinePlayer player) {
        CompletableFuture<MinetopiaPlayer> future = new CompletableFuture<>();
        getMinetopiaPlayerAsync(player, future::complete, future::completeExceptionally);
        return future;
    }

    /**
     * Fetches a MinetopiaPlayer asynchronously. If the player is online, retrieve it from the map.
     * Otherwise, query the database.
     *
     * @param player The OfflinePlayer to retrieve the MinetopiaPlayer for.
     * @param callback The action to perform if the MinetopiaPlayer is retrieved successfully.
     * @param errorCallback The action to perform if there is an error during retrieval.
     */
    public void getMinetopiaPlayerAsync(OfflinePlayer player,
                                        Consumer<MinetopiaPlayer> callback,
                                        Consumer<Throwable> errorCallback) {
        UUID playerId = player.getUniqueId();

        // Check if player is online and already loaded
        if (onlinePlayers.containsKey(playerId)) {
            callback.accept(onlinePlayers.get(playerId));
            return;
        }

        // Load player if offline or not in the map
        loadPlayerModel(player).whenComplete((playerModel, throwable) -> {
            if (throwable != null) {
                errorCallback.accept(throwable);
                return;
            }
            if (playerModel == null) {
                callback.accept(null);
                return;
            }

            // Create a new MinetopiaPlayer
            MinetopiaPlayer newMinetopiaPlayer = new MinetopiaPlayer(playerId, playerModel);
            loadMinetopiaPlayer(newMinetopiaPlayer).whenComplete((loadedMinetopiaPlayer, loadThrowable) -> {
                if (loadThrowable != null) {
                    errorCallback.accept(loadThrowable);
                    return;
                }
                if (player.isOnline()) {
                    onlinePlayers.put(playerId, loadedMinetopiaPlayer);
                } else {
                    loadedMinetopiaPlayer.load();
                }
                callback.accept(loadedMinetopiaPlayer);
            });
        });
    }

    /**
     * Synchronously gets a MinetopiaPlayer if there's an ongoing async operation or if the player is online.
     *
     * @param player The player to retrieve.
     * @return The MinetopiaPlayer object.
     */
    public MinetopiaPlayer getMinetopiaPlayerSync(OfflinePlayer player) {
        UUID playerId = player.getUniqueId();
        if (onlinePlayers.containsKey(playerId)) {
            return onlinePlayers.get(playerId);
        }

        // Perform synchronous retrieval if needed (not recommended if complex)
        CompletableFuture<MinetopiaPlayer> future = new CompletableFuture<>();
        getMinetopiaPlayerAsync(player, future::complete, future::completeExceptionally);
        return future.join();
    }

    private CompletableFuture<PlayerModel> loadPlayerModel(OfflinePlayer player) {
        UUID playerId = player.getUniqueId();
        CompletableFuture<PlayerModel> future = new CompletableFuture<>();
        this.playerModule.loadPlayer(playerId).whenComplete((playerModel, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }
            future.complete(playerModel);
        });
        return future;
    }

    private CompletableFuture<MinetopiaPlayer> loadMinetopiaPlayer(MinetopiaPlayer minetopiaPlayer) {
        CompletableFuture<MinetopiaPlayer> future = new CompletableFuture<>();
        minetopiaPlayer.load().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }
            future.complete(minetopiaPlayer);
        });
        return future;
    }
}