package com.bigbasket.sandbox.api.handler;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.bigbasket.core.dal.aerospike.Aerospike;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

import javax.inject.Inject;

public class AerospikeClientHandler {

    @Inject
    public AerospikeClientHandler() {
        this.aerospikeClient = new AerospikeClient("localhost", 3000);
    }
    @Inject
    Aerospike aerospikeService;
    private final AerospikeClient aerospikeClient;
    public Handler<RoutingContext> handleUpdates(){
        return routingContext -> {
            HttpServerResponse httpServerResponse = routingContext.response();
            String key = routingContext.getBodyAsJson().getString("key");
            String value = routingContext.getBodyAsJson().getString("value");
            Key aerospikeKey = new Key("test", "testSet", key);
            Bin bin = new Bin("bins", value);
            WritePolicy writePolicy = new WritePolicy();
            aerospikeClient.put(writePolicy, aerospikeKey, bin);
            httpServerResponse.setStatusCode(200).end(new JsonObject().put("key", key).put("value", value).encode());
        };
    }
    public Handler<RoutingContext> handleReads() {
        return routingContext -> {
            HttpServerResponse httpServerResponse = routingContext.response();
            String key = routingContext.queryParams().get("key");
            Key aerospikeKey = new Key("test", "testSet", key);
            // Read the value from Aerospike
            Record record = null;
            record = aerospikeClient.get(null, aerospikeKey);
            if (record != null) {
                // Value exists in Aerospike
                String value = record.getString("bins");
                httpServerResponse.setStatusCode(200).end(new JsonObject().put("key", key).put("value", value).encode());
            }
        };
    }
}
