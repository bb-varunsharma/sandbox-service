package com.bigbasket.sandbox.api.handler;

import javax.inject.Inject;

import com.bigbasket.core.dal.aerospike.ARecord;
import com.bigbasket.core.dal.aerospike.Aerospike;
import com.bigbasket.sandbox.model.SandboxError;

import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

public class AerospikeClientHandler {

    @Inject
    public AerospikeClientHandler() {
    }

    @Inject
    Aerospike aerospikeService;

    public Handler<RoutingContext> handleUpdates() {
        return routingContext -> {
            HttpServerResponse httpServerResponse = routingContext.response();
            JsonObject bodyJson = routingContext.getBodyAsJson();
            String key = "";
            String value = "";
            if (bodyJson != null) {
                key = bodyJson.getString("key");
                value = bodyJson.getString("value");
            } else {
                httpServerResponse.setStatusCode(400)
                        .end(Json.encodePrettily(SandboxError.badRequest("body json not found").toJson()));
                return;
            }

            String namespace = "bigbasket";

            String set = "new_set";
            int expirationInSecs = 0; // 1-hour

            Single<Boolean> result = aerospikeService.put(namespace, set, key, value, expirationInSecs);

            result.subscribe(res -> {
                if (res) {
                    httpServerResponse.setStatusCode(200).end(bodyJson.put("status", "success").encode());
                } else {
                    httpServerResponse.setStatusCode(500)
                            .end(Json.encodePrettily(SandboxError.unknownException("Error while fetching data from cache").toJson()));
                }
            });
        };
    }

    public Handler<RoutingContext> handleReads() {
        return routingContext -> {
            HttpServerResponse httpServerResponse = routingContext.response();
            String key = routingContext.queryParams().get("key");
            String namespace = "bigbasket";
            String set = "new_set";
            String binName = "value";

            // Read the value from Aerospike
            Single<ARecord> record = aerospikeService.get(namespace, set, key);
            JsonObject responseJson = new JsonObject().put("key", key);
            record.subscribe(res -> {
                if (res.bins != null && res.bins.get(binName) != null) {
                    String value = (String) res.bins.get(binName);
                    responseJson.put("value", value)
                            .put("status", "success");
                    httpServerResponse.setStatusCode(200).end(responseJson.encodePrettily());
                } else {
                    httpServerResponse.setStatusCode(404)
                            .end(Json.encodePrettily(SandboxError.notFoundError("data not in cache").toJson()));
                }
            }, err -> {
                httpServerResponse.setStatusCode(500)
                        .end(Json.encodePrettily(SandboxError.unknownException("Error while fetching data from cache").toJson()));
            });
        };
    }
}
