package nl.openminetopia.modules.restapi.verticles;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

import java.util.concurrent.CompletableFuture;

public class PlayerVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/player/:player").handler(this::handleGetPlayer);
    }

    private void handleGetPlayer(RoutingContext context) {
        String playerName = context.pathParam("player");
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

        if (!player.hasPlayedBefore()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", false);
            context.response().end(jsonObject.toJSONString());
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            JSONObject jsonObject = new JSONObject();

            try {
                MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayerSync(player);

                if (minetopiaPlayer == null) {
                    jsonObject.put("success", false);
                } else {
                    jsonObject.put("success", true);
                    jsonObject.put("uuid", player.getUniqueId().toString());
                    jsonObject.put("level", minetopiaPlayer.getLevel());
                    jsonObject.put("fitness", minetopiaPlayer.getFitness().getTotalFitness());
                    jsonObject.put("prefix", minetopiaPlayer.getActivePrefix().getPrefix());
                    jsonObject.put("timeSeconds", minetopiaPlayer.getPlaytime());
                }
            } catch (Exception e) {
                e.printStackTrace();
                jsonObject.put("success", false);
            }

            return jsonObject.toJSONString();
        }).thenAccept(response -> context.response().end(response));
    }
}