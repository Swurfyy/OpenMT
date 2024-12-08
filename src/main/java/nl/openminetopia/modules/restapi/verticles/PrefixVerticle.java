package nl.openminetopia.modules.restapi.verticles;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.prefix.objects.Prefix;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

public class PrefixVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/player/:player/prefixes").handler(this::handleGetPrefixes);
    }

    @SuppressWarnings("unchecked")
    private void handleGetPrefixes(RoutingContext context) {
        String playerName = context.pathParam("player");
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

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
                jsonObject.put("prefixes", minetopiaPlayer.getPrefixes().stream().map(Prefix::getPrefix).toList());
            }
            context.response().end(jsonObject.toJSONString());
        }).join();
    }
}