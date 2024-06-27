package com.bigbasket.sandbox.api.handler;

import javax.inject.Inject;

import com.bigbasket.core.dal.aerospike.ARecord;
import com.bigbasket.core.dal.aerospike.Aerospike;

import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

public class HealthCheckHandler {

    @Inject
    public HealthCheckHandler() {
    }

    @Inject
    Aerospike aerospikeService;

    public Handler<RoutingContext> handle() {
        return routingContext -> {
            HttpServerResponse httpServerResponse = routingContext.response();
            JsonObject response = new JsonObject();
            response.put("finalstatus", "UP");
//            Single<ARecord> value = aerospikeService.get("bigbasket", "new_set", "key1");
//            value.subscribe(t -> {
//                response.put("value", t.bins.get("bin1"));
                httpServerResponse.setStatusCode(200).end(response.encodePrettily());
//            });
        };
    }
}
