package nl.openminetopia.modules.restapi.verticles.places;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.places.PlacesModule;
import nl.openminetopia.modules.places.models.WorldModel;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.json.simple.JSONObject;

import java.util.Optional;

public class PlacesVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/places/worlds").handler(this::handleGetWorlds);
        router.get("/api/places/cities").handler(this::handleGetCities);
        startPromise.complete();
    }

    @SuppressWarnings("unchecked")
    private void handleGetWorlds(RoutingContext context) {
        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("success", true);

            JSONObject worldsObject = new JSONObject();

            PlacesModule placesModule = OpenMinetopia.getModuleManager().getModule(PlacesModule.class);

            placesModule.getWorldModels().forEach(worldModel -> {
                JSONObject worldObject = new JSONObject();
                worldObject.put("name", worldModel.getName());
                worldObject.put("color", worldModel.getColor());
                worldObject.put("title", worldModel.getTitle());
                worldObject.put("loading_name", worldModel.getLoadingName());
                worldObject.put("temperature", worldModel.getTemperature());

                worldsObject.put(worldModel.getId(), worldObject);
            });

            jsonObject.put("worlds", worldsObject);
            context.response().end(jsonObject.toJSONString());

        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", false);
            context.response().end(jsonObject.toJSONString());
            OpenMinetopia.getInstance().getLogger().severe("An error occurred while handling a request: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleGetCities(RoutingContext context) {
        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("success", true);

            JSONObject citiesObject = new JSONObject();

            PlacesModule placesModule = OpenMinetopia.getModuleManager().getModule(PlacesModule.class);

            placesModule.getCityModels().forEach(cityModel -> {
                JSONObject cityObject = new JSONObject();
                cityObject.put("name", cityModel.getName());
                cityObject.put("color", cityModel.getColor());
                cityObject.put("title", cityModel.getTitle());
                cityObject.put("loading_name", cityModel.getLoadingName());
                cityObject.put("temperature", cityModel.getTemperature());

                Optional<WorldModel> worldModel = placesModule.getWorldModels().stream()
                        .filter(world -> world.getId().equals(cityModel.getWorldId()))
                        .findFirst();

                cityObject.put("world", worldModel.isPresent() ? worldModel.get().getName() : "Unknown");

                citiesObject.put(cityModel.getId(), cityObject);
            });

            jsonObject.put("cities", citiesObject);
            context.response().end(jsonObject.toJSONString());

        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", false);
            context.response().end(jsonObject.toJSONString());
            OpenMinetopia.getInstance().getLogger().severe("An error occurred while handling a request: " + e.getMessage());
        }
    }
}