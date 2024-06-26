package com.bigbasket.sandbox.di.aerospike;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceProxyBuilder;

@VertxGen
@ProxyGen
public interface AerospikeServiceProxy {

    static AerospikeServiceProxy createProxy(Vertx vertx, String address) {
        ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(address);
        return builder.build(AerospikeServiceProxy.class);
    }
}
