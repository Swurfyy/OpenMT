package nl.openminetopia.modules.restapi.verticles.player;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.color.enums.OwnableColorType;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

import java.util.UUID;

public class PlayerVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/player/:uuid").handler(this::handleGetPlayer);
        startPromise.complete();
    }

    @SuppressWarnings("unchecked")
    private void handleGetPlayer(RoutingContext context) {
        try {
            UUID playerUuid = UUID.fromString(context.pathParam("uuid"));
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);

            JSONObject jsonObject = new JSONObject();

            if (!player.isOnline() && !player.hasPlayedBefore()) {
                jsonObject.put("success", false);
                jsonObject.put("error", "Player has not played before.");
                context.response().end(jsonObject.toJSONString());
                return;
            }

            PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
                JSONObject responseJson = new JSONObject();
                
                if (throwable != null) {
                    throwable.printStackTrace();
                    responseJson.put("success", false);
                    responseJson.put("error", throwable.getMessage());
                } else if (minetopiaPlayer == null) {
                    responseJson.put("success", false);
                    responseJson.put("error", "MinetopiaPlayer has not loaded.");
                } else {
                    responseJson.put("success", true);
                    responseJson.put("uuid", player.getUniqueId().toString());
                    responseJson.put("active_prefix", minetopiaPlayer.getActivePrefix().getPrefix());
                    responseJson.put("active_prefix_color", minetopiaPlayer.getActiveColor(OwnableColorType.PREFIX).displayName());
                    responseJson.put("active_name_color", minetopiaPlayer.getActiveColor(OwnableColorType.NAME).displayName());
                    responseJson.put("active_level_color", minetopiaPlayer.getActiveColor(OwnableColorType.LEVEL).displayName());
                    responseJson.put("active_chat_color", minetopiaPlayer.getActiveColor(OwnableColorType.CHAT).displayName());
                    responseJson.put("playtimeSeconds", minetopiaPlayer.getPlaytime() / 1000);
                    responseJson.put("online", player.isOnline());
                }
                context.response().end(responseJson.toJSONString());
            });
        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", false);
            jsonObject.put("error", e.getMessage());
            context.response().end(jsonObject.toJSONString());
            OpenMinetopia.getInstance().getLogger().severe("An error occurred while handling a request: " + e.getMessage());
        }
    }
}