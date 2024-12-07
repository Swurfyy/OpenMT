package nl.openminetopia.modules.portal.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.portal.PortalModule;
import nl.openminetopia.utils.ChatUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

@CommandAlias("koppel")
public class VerifyCommand extends BaseCommand {

    @Default
    public void verify(Player player, String token) {
        verifyPlayer(player, token);
    }

    @SuppressWarnings("unchecked")
    private boolean verifyPlayer(Player player, String token) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("token", token);
        requestBody.put("minecraft_username", player.getName());
        requestBody.put("minecraft_uuid", player.getUniqueId().toString());

        PortalModule portalModule = OpenMinetopia.getModuleManager().getModule(PortalModule.class);

        try (AsyncHttpClient client = new DefaultAsyncHttpClient()) {
            client.preparePost(portalModule.getPortalApiUrl() + "/minecraft/verify")
                    .setHeader("Content-Type", "application/json")
                    .setHeader("X-API-Key", portalModule.getApiKey())
                    .setBody(requestBody.toString())
                    .execute()
                    .toCompletableFuture()
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            ChatUtils.sendMessage(player, "<red>Er is iets fout gegaan bij het verifiëren van je account.");
                            throwable.printStackTrace();
                            return;
                        }

                        if (response.getStatusCode() != 200) {
                            ChatUtils.sendMessage(player, "<red>Er is iets fout gegaan bij het verifiëren van je account.");
                            return;
                        }

                        ChatUtils.sendMessage(player, "<green>Je account is succesvol gekoppeld!");
                    })
                    .join();

            return true;
        } catch (Exception e) {
            ChatUtils.sendMessage(player, "<red>Er is iets fout gegaan bij het verifiëren van je account.");
            e.printStackTrace();
        }

        return false;
    }
}
