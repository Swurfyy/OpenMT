package nl.openminetopia.modules.restapi;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.restapi.base.VerticleManager;
import nl.openminetopia.modules.restapi.verticles.MainVerticle;
import nl.openminetopia.modules.restapi.verticles.PlayerVerticle;

public class RestAPIModule extends Module {

    public VerticleManager verticleManager;

    @Override
    public void enable() {
        if (OpenMinetopia.getDefaultConfiguration().isRestApiEnabled()) {
            Vertx vertx = Vertx.vertx();

            Context context = vertx.getOrCreateContext();

            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());

            verticleManager = new VerticleManager(vertx, context, router);
            verticleManager.register(
                    new MainVerticle(),
                    new PlayerVerticle()
            );
        }
    }

    @Override
    public void disable() {

    }

    private void validateApiKey(RoutingContext context) {
        String apiKey = context.request().getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(OpenMinetopia.getDefaultConfiguration().getRestApiKey())) {
            context.response().setStatusCode(401).end("Unauthorized request");
        } else {
            context.next();
        }
    }
}