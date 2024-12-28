package nl.openminetopia.modules.player;

import com.craftmend.storm.api.enums.Where;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.player.commands.PlaytimeCommand;
import nl.openminetopia.modules.player.listeners.PlayerPreLoginListener;
import nl.openminetopia.modules.player.listeners.PlayerQuitListener;
import nl.openminetopia.modules.player.models.PlayerModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class PlayerModule extends Module {

    @Override
    public void enable() {
        registerListener(new PlayerPreLoginListener());
        registerListener(new PlayerQuitListener());

        registerCommand(new PlaytimeCommand());

        Bukkit.getScheduler().runTaskTimerAsynchronously(OpenMinetopia.getInstance(), () -> {
            for (MinetopiaPlayer minetopiaPlayer : PlayerManager.getInstance().getOnlinePlayers().values()) {
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
            PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
                if (minetopiaPlayer == null) return;
                minetopiaPlayer.save().whenComplete((unused, throwable) -> {
                    if (throwable != null) throwable.printStackTrace();
                });
            }, Throwable::printStackTrace);
        }
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

    public CompletableFuture<PlayerModel> playerLoadFuture = new CompletableFuture<>();
    public CompletableFuture<PlayerModel> loadPlayer(UUID uuid) {
        findPlayerModel(uuid).thenAccept(playerModel -> {
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
                createdModel.setFitnessReset(false);
                playerLoadFuture.complete(createdModel);

                StormDatabase.getInstance().saveStormModel(createdModel);
                return;
            }

            playerLoadFuture.complete(playerModel.get());
        });

        return playerLoadFuture;
    }
}