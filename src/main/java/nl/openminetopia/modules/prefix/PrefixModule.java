package nl.openminetopia.modules.prefix;

import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.player.models.PlayerModel;
import nl.openminetopia.modules.prefix.models.PrefixModel;
import nl.openminetopia.modules.data.utils.StormUtils;
import nl.openminetopia.modules.prefix.commands.PrefixCommand;
import nl.openminetopia.modules.prefix.commands.subcommands.PrefixAddCommand;
import nl.openminetopia.modules.prefix.commands.subcommands.PrefixMenuCommand;
import nl.openminetopia.modules.prefix.commands.subcommands.PrefixRemoveCommand;
import nl.openminetopia.modules.prefix.objects.Prefix;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PrefixModule extends Module {

    Collection<PrefixModel> prefixModels = new ArrayList<>();

    @Override
    public void enable() {
        registerCommand(new PrefixCommand());
        registerCommand(new PrefixMenuCommand());
        registerCommand(new PrefixAddCommand());
        registerCommand(new PrefixRemoveCommand());

        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            OpenMinetopia.getInstance().getLogger().info("Loading prefixes...");

            this.getPrefixes().whenComplete((prefixModels, throwable) -> {
                if (throwable != null) {
                    OpenMinetopia.getInstance().getLogger().severe("Failed to load prefixes: " + throwable.getMessage());
                    return;
                }

                this.prefixModels = prefixModels;
                OpenMinetopia.getInstance().getLogger().info("Loaded " + prefixModels.size() + " prefixes.");
            });
        }, 20L);

        OpenMinetopia.getCommandManager().getCommandCompletions().registerCompletion("playerPrefixes", context -> {
            var player = PlayerManager.getInstance().getMinetopiaPlayer(context.getPlayer());
            if (player == null) return List.of();
            return player.getPrefixes().stream().map(Prefix::getPrefix).toList();
        });
    }

    @Override
    public void disable() {

    }

    public List<Prefix> getPrefixesFromPlayer(PlayerModel playerModel) {
        return playerModel.getPrefixes().stream().map(prefixModel ->
                new Prefix(prefixModel.getId(), prefixModel.getPrefix(), prefixModel.getExpiresAt())
        ).collect(Collectors.toList());
    }

    public Optional<Prefix> getActivePrefixFromPlayer(PlayerModel playerModel) {
        return playerModel.getPrefixes().stream().filter(prefixModel -> prefixModel.getExpiresAt() == null
                || prefixModel.getExpiresAt() > System.currentTimeMillis()
        ).map(prefixModel -> new Prefix(prefixModel.getId(), prefixModel.getPrefix(), prefixModel.getExpiresAt())
        ).findFirst();
    }

    public CompletableFuture<Collection<PrefixModel>> getPrefixes() {
        CompletableFuture<Collection<PrefixModel>> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<PrefixModel> prefixModels = StormDatabase.getInstance().getStorm().buildQuery(PrefixModel.class)
                        .execute().join();
                completableFuture.complete(prefixModels);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }

    public CompletableFuture<Integer> addPrefix(MinetopiaPlayer player, Prefix prefix) {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            PrefixModel prefixModel = new PrefixModel();
            prefixModel.setPlayerId(player.getPlayerModel().getId());
            prefixModel.setPrefix(prefix.getPrefix());
            prefixModel.setExpiresAt(prefix.getExpiresAt());

            int id = StormDatabase.getInstance().saveStormModel(prefixModel).join();
            completableFuture.complete(id);
        });
        return completableFuture;
    }

    public CompletableFuture<Void> removePrefix(Prefix prefix) {
        return StormUtils.deleteModelData(PrefixModel.class,
                query -> query.where("id", Where.EQUAL, prefix.getId()));
    }
}
