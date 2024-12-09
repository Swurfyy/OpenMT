package nl.openminetopia.modules.restapi.verticles.player;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.color.enums.OwnableColorType;
import nl.openminetopia.modules.color.objects.OwnableColor;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

import java.util.UUID;

public class ColorsVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/player/:uuid/colors").handler(this::handleGetColors);
    }

    @SuppressWarnings("unchecked")
    private void handleGetColors(RoutingContext context) {
        try {
            UUID playerUuid = UUID.fromString(context.pathParam("uuid"));
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);

            JSONObject jsonObject = new JSONObject();

            if (!player.hasPlayedBefore()) {
                jsonObject.put("success", false);
                context.response().end(jsonObject.toJSONString());
                return;
            }

            PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    jsonObject.put("success", false);
                }

                if (minetopiaPlayer == null) {
                    jsonObject.put("success", false);
                } else {
                    jsonObject.put("success", true);
                    JSONObject colorsObject = new JSONObject();


                    minetopiaPlayer.getColors().forEach(color -> {
                        JSONObject colorObject = new JSONObject();
                        colorObject.put("display_name", color.displayName());
                        colorObject.put("color", color.color());
                        colorObject.put("expires_at", color.getExpiresAt());
                        colorObject.put("type", color.getType().name());
                        colorObject.put("hex_value", ChatUtils.componentToHex(ChatUtils.color(color.color())));
                        colorsObject.put(color.getId(), colorObject);
                    });

                    jsonObject.put("colors", colorsObject);
                }
                context.response().end(jsonObject.toJSONString());
            }).join();
        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", false);
            context.response().end(jsonObject.toJSONString());
            OpenMinetopia.getInstance().getLogger().severe("An error occurred while handling a request: " + e.getMessage());
        }
    }
}