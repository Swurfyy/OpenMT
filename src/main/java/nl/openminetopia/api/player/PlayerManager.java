package nl.openminetopia.api.player;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.player.PlayerModule;
import nl.openminetopia.modules.player.models.PlayerModel;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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


    public MinetopiaPlayer getOnlineMinetopiaPlayer(Player player) {
        return onlinePlayers.get(player.getUniqueId());
    }

    public CompletableFuture<MinetopiaPlayer> getMinetopiaPlayer(OfflinePlayer player) {
        CompletableFuture<MinetopiaPlayer> future = new CompletableFuture<>();

        if (onlinePlayers.containsKey(player.getUniqueId())) {
            future.complete(onlinePlayers.get(player.getUniqueId()));
            return future;
        }

        this.playerModule.getPlayerModel(player.getUniqueId()).whenComplete((playerModel, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }

            MinetopiaPlayer minetopiaPlayer = new MinetopiaPlayer(player.getUniqueId(), playerModel);

            minetopiaPlayer.load().thenAccept(unused -> {
                if (player.isOnline()) {
                    onlinePlayers.put(player.getUniqueId(), minetopiaPlayer);
                }

                future.complete(minetopiaPlayer);
            }).exceptionally(throwable2 -> {
                future.completeExceptionally(throwable2);
                return null;
            });
        });

        return future;
    }
}