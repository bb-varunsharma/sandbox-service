package com.bigbasket.sandbox.controller;

import static com.bigbasket.core.dal.aerospike.AerospikeService.AEROSPIKE_SERVICE_ADDRESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bigbasket.sandbox.di.aerospike.AerospikeServiceProxy;
import com.bigbasket.sandbox.di.aerospike.AerospikeServiceProxyImpl;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class MainVerticle extends com.bigbasket.core.controller.MainVerticle { //NOSONAR

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    protected void rxDeployVertical(JsonObject config) {
        AerospikeServiceProxy aerospikeServiceProxy = AerospikeServiceProxyImpl.createAerospikeService(vertx, config);
        new ServiceBinder(vertx)
                .setAddress(AEROSPIKE_SERVICE_ADDRESS)
                .register(AerospikeServiceProxy.class, aerospikeServiceProxy);

        super.rxDeployVertical(config);
        DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(config.getInteger("NUMBER_OF_VERTICLES", 4));
        vertx.deployVerticle(SandboxVerticle.class, options);
    }
}
