package nl.openminetopia.modules.restapi.verticles.player;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

import java.util.UUID;

public class PlayerBankAccountsVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/player/:uuid/bankaccounts").handler(this::handleGetBankAccounts);
    }

    @SuppressWarnings("unchecked")
    private void handleGetBankAccounts(RoutingContext context) {
        try {
            UUID playerUuid = UUID.fromString(context.pathParam("uuid"));
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);

            JSONObject jsonObject = new JSONObject();

            if (!player.hasPlayedBefore()) {
                jsonObject.put("success", false);
                context.response().end(jsonObject.toJSONString());
                return;
            }

            BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);

            JSONObject accountsObject = new JSONObject();

            bankingModule.getAccountsFromPlayer(playerUuid).forEach(account -> {
                jsonObject.put("success", true);

                JSONObject accountObject = new JSONObject();
                accountObject.put("type", account.getType().name());
                accountObject.put("name", account.getName());
                accountObject.put("frozen", account.getFrozen());
                accountObject.put("balance", account.getBalance());
                accountsObject.put(account.getUniqueId().toString(), accountObject);
            });

            jsonObject.put("accounts", accountsObject);
            context.response().end(jsonObject.toJSONString());
        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", false);
            context.response().end(jsonObject.toJSONString());
            OpenMinetopia.getInstance().getLogger().severe("An error occurred while handling a request: " + e.getMessage());
        }
    }
}
