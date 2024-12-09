package nl.openminetopia.modules.restapi;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.restapi.base.VerticleManager;
import nl.openminetopia.modules.restapi.verticles.MainVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.BankAccountUsersVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.BankAccountVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.BankAccountsVerticle;
import nl.openminetopia.modules.restapi.verticles.places.PlacesVerticle;
import nl.openminetopia.modules.restapi.verticles.player.ColorsVerticle;
import nl.openminetopia.modules.restapi.verticles.player.CriminalRecordsVerticle;
import nl.openminetopia.modules.restapi.verticles.player.FitnessStatisticsVerticle;
import nl.openminetopia.modules.restapi.verticles.player.PlayerBankAccountsVerticle;
import nl.openminetopia.modules.restapi.verticles.player.PlayerPlotsVerticle;
import nl.openminetopia.modules.restapi.verticles.player.PlayerVerticle;
import nl.openminetopia.modules.restapi.verticles.player.PrefixesVerticle;
import nl.openminetopia.modules.restapi.verticles.plots.PlotsVerticle;

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
                    new PlayerVerticle(),
                    new PrefixesVerticle(),
                    new ColorsVerticle(),
                    new FitnessStatisticsVerticle(),
                    new BankAccountVerticle(),
                    new BankAccountsVerticle(),
                    new BankAccountUsersVerticle(),
                    new PlayerBankAccountsVerticle(),
                    new CriminalRecordsVerticle(),
                    new PlayerPlotsVerticle(),
                    new PlotsVerticle(),
                    new PlacesVerticle()
            );
        }
    }

    @Override
    public void disable() {

    }
}