package nl.openminetopia.modules.restapi;

import nl.openminetopia.utils.modules.ExtendedSpigotModule;
import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.restapi.base.VerticleManager;
import nl.openminetopia.modules.restapi.verticles.MainVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.transactions.BankAccountTransactionVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.BankAccountUsersVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.BankAccountVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.BankAccountsVerticle;
import nl.openminetopia.modules.restapi.verticles.banking.transactions.BankAccountTransactionsVerticle;
import nl.openminetopia.modules.restapi.verticles.places.PlacesVerticle;
import nl.openminetopia.modules.restapi.verticles.player.*;
import nl.openminetopia.modules.restapi.verticles.plots.PlotsVerticle;
import org.jetbrains.annotations.NotNull;

public class RestAPIModule extends ExtendedSpigotModule {

    public RestAPIModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager, DataModule dataModule) {
        super(moduleManager);
    }

    public VerticleManager verticleManager;

    @Override
    public void onEnable() {
        if (OpenMinetopia.getDefaultConfiguration().isRestApiEnabled()) {
            Vertx vertx = OpenMinetopia.getInstance().getOrCreateVertx();

            Context context = vertx.getOrCreateContext();

            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());

            verticleManager = new VerticleManager(vertx, context, router);
            verticleManager.register(
                    /* GET */
                    new MainVerticle(),
                    new PlayerVerticle(),
                    new PrefixesVerticle(),
                    new ColorsVerticle(),
                    new BankAccountVerticle(),
                    new BankAccountsVerticle(),
                    new BankAccountUsersVerticle(),
                    new PlayerBankAccountsVerticle(),
                    new CriminalRecordsVerticle(),
                    new PlayerPlotsVerticle(),
                    new PlotsVerticle(),
                    new PlacesVerticle(),

                    /* POST */
                    new BankAccountTransactionsVerticle(),
                    new BankAccountTransactionVerticle()
            );
        }
    }
}