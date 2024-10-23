package nl.openminetopia.modules.player;

import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.player.models.PlayerModel;
import nl.openminetopia.modules.data.utils.StormUtils;
import nl.openminetopia.modules.player.commands.PlaytimeCommand;
import nl.openminetopia.modules.player.listeners.PlayerJoinListener;
import nl.openminetopia.modules.player.listeners.PlayerPreLoginListener;
import nl.openminetopia.modules.player.listeners.PlayerQuitListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerModule extends Module {

    @Override
    public void enable() {
        registerListener(new PlayerPreLoginListener());
        registerListener(new PlayerJoinListener());
        registerListener(new PlayerQuitListener());

        registerCommand(new PlaytimeCommand());

        Bukkit.getScheduler().runTaskTimerAsynchronously(OpenMinetopia.getInstance(), () -> {
            for (MinetopiaPlayer minetopiaPlayer : PlayerManager.getInstance().getMinetopiaPlayers().values()) {
                if (!(minetopiaPlayer instanceof MinetopiaPlayer onlineMinetopiaPlayer)) continue;
                onlineMinetopiaPlayer.save().whenComplete((unused, throwable) -> {
                    if (throwable != null) throwable.printStackTrace();
                });
            }
        }, 0, 20 * 60 * 5); // Save every 5 minutes
    }

    @Override
    public void disable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MinetopiaPlayer minetopiaPlayer = (MinetopiaPlayer) PlayerManager.getInstance().getMinetopiaPlayer(player);
            if (minetopiaPlayer == null) continue;
            minetopiaPlayer.save().whenComplete((unused, throwable) -> {
                if (throwable != null) throwable.printStackTrace();
            });
        }
    }

    private CompletableFuture<Optional<PlayerModel>> findPlayerModel(@NotNull UUID uuid) {
        CompletableFuture<Optional<PlayerModel>> completableFuture = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<PlayerModel> playerModel = StormDatabase.getInstance().getStorm().buildQuery(PlayerModel.class).where("uuid", Where.EQUAL, uuid.toString()).limit(1).execute().join();
                Bukkit.getScheduler().runTaskLaterAsynchronously(OpenMinetopia.getInstance(), () -> completableFuture.complete(playerModel.stream().findFirst()), 1L);
            } catch (Exception exception) {
                exception.printStackTrace();
                completableFuture.completeExceptionally(exception);
            }
        });
        return completableFuture;
    }

    public CompletableFuture<PlayerModel> loadPlayer(UUID uuid) {
        CompletableFuture<PlayerModel> completableFuture = new CompletableFuture<>();
        findPlayerModel(uuid).thenAccept(playerModel -> {
            PlayerManager.getInstance().getPlayerModels().remove(uuid);

            if (playerModel.isEmpty()) {
                PlayerModel createdModel = new PlayerModel();
                createdModel.setUniqueId(uuid);
                createdModel.setPlaytime(0);
                createdModel.setLevel(1);
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

                PlayerManager.getInstance().getPlayerModels().put(uuid, createdModel);
                completableFuture.complete(createdModel);

                StormDatabase.getInstance().saveStormModel(createdModel);
                return;
            }

            PlayerManager.getInstance().getPlayerModels().put(uuid, playerModel.get());
            completableFuture.complete(playerModel.get());
        });

        return completableFuture;
    }

    public CompletableFuture<Void> savePlayer(PlayerModel playerModel) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        StormUtils.updateModelData(PlayerModel.class,
                query -> query.where("uuid", Where.EQUAL, playerModel.getUniqueId()),
                model -> {
                    model.setLevel(playerModel.getLevel());
                    model.setActivePrefixId(playerModel.getActivePrefixId());
                    model.setActivePrefixColorId(playerModel.getActivePrefixColorId());
                    model.setActiveChatColorId(playerModel.getActiveChatColorId());
                    model.setActiveNameColorId(playerModel.getActiveNameColorId());
                    model.setActiveLevelColorId(playerModel.getActiveLevelColorId());
                    model.setPlaytime(playerModel.getPlaytime());
                    model.setStaffchatEnabled(playerModel.getStaffchatEnabled());
                }
        );
        future.complete(null);
        return future;
    }
}