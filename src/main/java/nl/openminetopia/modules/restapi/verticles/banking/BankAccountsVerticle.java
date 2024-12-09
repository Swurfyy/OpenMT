package nl.openminetopia.modules.restapi.verticles.banking;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.json.simple.JSONObject;

public class BankAccountsVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/bankaccounts/").handler(this::handleGetBankAccounts);
    }

    @SuppressWarnings("unchecked")
    private void handleGetBankAccounts(RoutingContext context) {
        try {
            BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);

            bankingModule.getBankAccounts().whenComplete((accounts, throwable) -> {
                JSONObject jsonObject = new JSONObject();

                if (throwable != null) {
                    throwable.printStackTrace();
                    jsonObject.put("success", false);
                    return;
                }

                if (accounts == null) {
                    jsonObject.put("success", false);
                } else {
                    jsonObject.put("success", true);

                    JSONObject accountsObject = new JSONObject();
                    accounts.forEach(account -> {
                        JSONObject accountObject = new JSONObject();
                        accountObject.put("uuid", account.getUniqueId().toString());
                        accountObject.put("type", account.getType().name());
                        accountObject.put("name", account.getName());
                        accountObject.put("frozen", account.getFrozen());
                        accountObject.put("balance", account.getBalance());
                        accountsObject.put(account.getUniqueId().toString(), accountObject);
                    });
                    jsonObject.put("accounts", accountsObject);
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
