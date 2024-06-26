package com.bigbasket.sandbox.di;

import com.bigbasket.core.common.di.AerospikeModule;
import com.bigbasket.core.common.di.MySqlModule;
import com.bigbasket.core.common.di.TenantSettingsModule;
import com.bigbasket.core.common.di.VerticleScope;
import com.bigbasket.core.telemetry.tracing.BBTracer;
import com.bigbasket.sandbox.api.SandboxAPI;
import com.bigbasket.sandbox.di.aerospike.AerospikeServiceProxy;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;

@VerticleScope
@Component(modules = { TenantSettingsModule.class, MySqlModule.class, AerospikeModule.class, SandboxModule.class })
public interface ObjectFactory {

    @VerticleScope
    SandboxAPI sandboxAPI();

    @VerticleScope
    BBTracer tracer();

//    @VerticleScope
//    AerospikeServiceProxy aerospikeServiceProxy();

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder injectVertx(Vertx vertx);

        @BindsInstance
        Builder injectConfig(JsonObject config);

        @BindsInstance
        Builder injectRouter(Router router);

        ObjectFactory build();
    }
}
