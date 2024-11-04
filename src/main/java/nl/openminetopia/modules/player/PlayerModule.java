package nl.openminetopia.modules.player;

import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.player.models.PlayerModel;
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

//        // fetch all players from the database and load them into the cache
//        StormDatabase.getExecutorService().submit(() -> {
//            Collection<PlayerModel> playerModels = new ArrayList<>();
//            try {
//                playerModels = StormDatabase.getInstance().getStorm().buildQuery(PlayerModel.class).execute().join();
//            } catch (Exception e) {
//                OpenMinetopia.getInstance().getLogger().severe("Failed to load player models from the database: " + e.getMessage());
//            }
//
//            for (PlayerModel playerModel : playerModels) {
//                PlayerManager.getInstance().getPlayerModels().put(playerModel.getUniqueId(), playerModel);
//            }
//        });

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
            //PlayerManager.getInstance().getPlayerModels().remove(uuid);

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

                //PlayerManager.getInstance().getPlayerModels().put(uuid, createdModel);
                completableFuture.complete(createdModel);

                StormDatabase.getInstance().saveStormModel(createdModel);
                return;
            }

            //PlayerManager.getInstance().getPlayerModels().put(uuid, playerModel.get());
            completableFuture.complete(playerModel.get());
        });

        return completableFuture;
    }
}