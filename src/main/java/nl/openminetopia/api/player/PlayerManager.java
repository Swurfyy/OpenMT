package nl.openminetopia.api.player;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.player.PlayerModule;
import nl.openminetopia.modules.player.models.PlayerModel;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private final HashMap<UUID, PlayerModel> playerModels = new HashMap<>();
    private final HashMap<UUID, MinetopiaPlayer> minetopiaPlayers = new HashMap<>();

    public @Nullable MinetopiaPlayer getMinetopiaPlayer(@NotNull OfflinePlayer player) {
        UUID playerId = player.getUniqueId();

        // Check if the MinetopiaPlayer is already loaded
        MinetopiaPlayer minetopiaPlayer = minetopiaPlayers.get(playerId);
        if (minetopiaPlayer != null) {
            return minetopiaPlayer;
        }

        // If the PlayerModel is not loaded, load it asynchronously
        if (!playerModels.containsKey(playerId)) {
            loadPlayerModel(player, playerId);
            return null; // Return null while loading
        }

        // Create a new MinetopiaPlayer instance
        minetopiaPlayer = new MinetopiaPlayer(playerId, playerModels.get(playerId));
        loadMinetopiaPlayer(minetopiaPlayer, player);
        return minetopiaPlayer;
    }

    private void loadPlayerModel(OfflinePlayer player, UUID playerId) {
        this.playerModule.loadPlayer(playerId).whenComplete((playerModel, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }
            if (playerModel == null) {
                OpenMinetopia.getInstance().getLogger().warning("Failed to load player data for " + player.getName());
                return;
            }

            playerModels.put(playerId, playerModel);
            // Attempt to retrieve MinetopiaPlayer again after loading
            getMinetopiaPlayer(player);
        });
    }

    private void loadMinetopiaPlayer(MinetopiaPlayer minetopiaPlayer, OfflinePlayer player) {
        minetopiaPlayer.load().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }
            OpenMinetopia.getInstance().getLogger().info("Loaded player data for " + player.getName());
            if (player.isOnline() && player.getPlayer() != null) {
                player.getPlayer().sendMessage(MessageConfiguration.component("player_data_loaded"));
            }
        });
        minetopiaPlayers.put(minetopiaPlayer.getUuid(), minetopiaPlayer);
    }
}