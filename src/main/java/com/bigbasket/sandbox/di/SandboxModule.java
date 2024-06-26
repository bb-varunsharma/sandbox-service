package com.bigbasket.sandbox.di;

import com.bigbasket.core.common.di.VerticleScope;
import com.bigbasket.core.telemetry.tracing.BBMockTracer;
import com.bigbasket.core.telemetry.tracing.BBTracer;
import com.bigbasket.sandbox.webclient.HulkWebClient;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

@Module
public class SandboxModule {

    @Provides
    @VerticleScope
    static HulkWebClient hulkClient(Vertx vertx, JsonObject config, BBTracer bbTracer) {
        HulkWebClient hulkWebClient = new HulkWebClient(config, vertx);
        hulkWebClient.setBbTracer(bbTracer);
        return hulkWebClient;
    }

    @Provides
    @VerticleScope
    static BBTracer bbTracer(Vertx vertx, JsonObject config) {
        return (BBTracer) (!config.getBoolean("TRACING_ENABLED") ? new BBMockTracer() : BBTracer.getTracer(config, config.getString("SERVICE_NAME", "sandbox")));
    }
}
