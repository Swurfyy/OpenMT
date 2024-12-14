package nl.openminetopia.modules.restapi.verticles.banking;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.json.simple.JSONObject;

import java.util.UUID;

public class BankAccountUsersVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/bankaccount/:uuid/users").handler(this::handleGetUsers);
        startPromise.complete();
    }

    @SuppressWarnings("unchecked")
    private void handleGetUsers(RoutingContext context) {
        try {
            UUID accountUuid = UUID.fromString(context.pathParam("uuid"));

            BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);

            bankingModule.getAccountByIdAsync(accountUuid).whenComplete((account, throwable) -> {
                JSONObject jsonObject = new JSONObject();

                if (throwable != null) {
                    throwable.printStackTrace();
                    jsonObject.put("success", false);
                    return;
                }

                if (account == null) {
                    jsonObject.put("success", false);
                } else {
                    JSONObject usersObject = new JSONObject();

                    account.getUsers().forEach((userUuid, permission) -> {
                        JSONObject userObject = new JSONObject();
                        userObject.put("permission", permission.name());
                        usersObject.put(userUuid, userObject);
                    });

                    jsonObject.put("success", true);
                    jsonObject.put("users", usersObject);
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