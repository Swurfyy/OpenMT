package nl.openminetopia.modules.restapi.verticles.banking;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.json.simple.JSONObject;

import java.util.UUID;

public class BankAccountTransactionVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.post("/api/bankaccount/:uuid/withdraw").handler(this::handleWithdraw);
        router.post("/api/bankaccount/:uuid/deposit").handler(this::handleDeposit);
        startPromise.complete();
    }

    @SuppressWarnings("unchecked")
    private void handleWithdraw(RoutingContext context) {
        JSONObject responseJson = new JSONObject();

        UUID accountUuid;
        try {
            accountUuid = UUID.fromString(context.pathParam("uuid"));
        } catch (IllegalArgumentException e) {
            responseJson.put("success", false);
            responseJson.put("error", "Invalid UUID format.");
            context.response().setStatusCode(400).end(responseJson.toJSONString());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(context.body().asJsonObject().getString("amount"));
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive.");
            }
        } catch (Exception e) {
            responseJson.put("success", false);
            responseJson.put("error", "Invalid amount.");
            context.response().setStatusCode(400).end(responseJson.toJSONString());
            return;
        }

        BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);

        bankingModule.getAccountByIdAsync(accountUuid).whenComplete((account, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                responseJson.put("success", false);
                responseJson.put("error", "Internal server error.");
                context.response().setStatusCode(500).end(responseJson.toJSONString());
                return;
            }

            if (account == null) {
                responseJson.put("success", false);
                responseJson.put("error", "Account not found.");
                context.response().setStatusCode(404).end(responseJson.toJSONString());
                return;
            }

            double currentBalance = account.getBalance();
            if (currentBalance < amount) {
                responseJson.put("success", false);
                responseJson.put("error", "Insufficient balance.");
                context.response().setStatusCode(400).end(responseJson.toJSONString());
            } else {
                account.setBalance(currentBalance - amount);
                responseJson.put("success", true);
                responseJson.put("newBalance", account.getBalance());
                StormDatabase.getInstance().saveStormModel(account);
                context.response().setStatusCode(200).end(responseJson.toJSONString());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void handleDeposit(RoutingContext context) {
        JSONObject responseJson = new JSONObject();

        UUID accountUuid;
        try {
            accountUuid = UUID.fromString(context.pathParam("uuid"));
        } catch (IllegalArgumentException e) {
            responseJson.put("success", false);
            responseJson.put("error", "Invalid UUID format.");
            context.response().setStatusCode(400).end(responseJson.toJSONString());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(context.body().asJsonObject().getString("amount"));
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive.");
            }
        } catch (Exception e) {
            responseJson.put("success", false);
            responseJson.put("error", "Invalid amount.");
            context.response().setStatusCode(400).end(responseJson.toJSONString());
            return;
        }

        BankingModule bankingModule = OpenMinetopia.getModuleManager().getModule(BankingModule.class);

        bankingModule.getAccountByIdAsync(accountUuid).whenComplete((account, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                responseJson.put("success", false);
                responseJson.put("error", "Internal server error.");
                context.response().setStatusCode(500).end(responseJson.toJSONString());
                return;
            }

            if (account == null) {
                responseJson.put("success", false);
                responseJson.put("error", "Account not found.");
                context.response().setStatusCode(404).end(responseJson.toJSONString());
                return;
            }

            double currentBalance = account.getBalance();
            account.setBalance(currentBalance + amount); // Add the amount to the account balance
            responseJson.put("success", true);
            responseJson.put("newBalance", account.getBalance());
            StormDatabase.getInstance().saveStormModel(account); // Save the updated account
            context.response().setStatusCode(200).end(responseJson.toJSONString());
        });
    }
}