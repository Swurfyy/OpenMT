package nl.openminetopia.api.player;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.player.models.PlayerModel;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class PlayerManager {

    private static PlayerManager instance;

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    private final DataModule dataModule = OpenMinetopia.getModuleManager().getModule(DataModule.class);

    public HashMap<UUID, PlayerModel> playerModels = new HashMap<>();
    public HashMap<UUID, MinetopiaPlayer> minetopiaPlayers = new HashMap<>();

    public @Nullable MinetopiaPlayer getMinetopiaPlayer(@NotNull OfflinePlayer player) {
        UUID playerId = player.getUniqueId();
        MinetopiaPlayer minetopiaPlayer = minetopiaPlayers.get(playerId);

        if (minetopiaPlayer == null) {
            minetopiaPlayer = new MinetopiaPlayer(playerId, playerModels.get(playerId));
            minetopiaPlayer.load().whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    return;
                }
                OpenMinetopia.getInstance().getLogger().info("Loaded player data for " + player.getName());
                if (player.getPlayer() != null && player.isOnline()) {
                    player.getPlayer().sendMessage(MessageConfiguration.component("player_data_loaded"));
                }
            });
            minetopiaPlayers.put(playerId, minetopiaPlayer);
        }

        return minetopiaPlayer;
    }
}