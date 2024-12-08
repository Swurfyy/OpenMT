package nl.openminetopia.modules.restapi.base;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import lombok.Getter;

@Getter
public abstract class BaseVerticle extends AbstractVerticle {

    protected Router router;

    public BaseVerticle() {
    }

    public void init(Vertx vertx, Context context, Router router) {
        this.router = router;
        init(vertx, context);
    }
}
