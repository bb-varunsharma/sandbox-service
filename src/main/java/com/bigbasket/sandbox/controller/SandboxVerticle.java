package com.bigbasket.sandbox.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bigbasket.core.common.RequestContext;
import com.bigbasket.core.controller.BBCommonVerticle;
import com.bigbasket.sandbox.api.SandboxAPI;
import com.bigbasket.sandbox.di.DaggerObjectFactory;
import com.bigbasket.sandbox.di.ObjectFactory;
import com.bigbasket.sandbox.model.SandboxError;
import com.newrelic.api.agent.NewRelic;

import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class SandboxVerticle extends BBCommonVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(SandboxVerticle.class);

    public static final String HEALTH_BASIC_URL = "/sandbox/v1/health";

    public static final String DEBUG_URL = "/sandbox/v1/debug";

    private ObjectFactory factory;

    public SandboxVerticle() {
        super(HEALTH_BASIC_URL, DEBUG_URL);
    }

    @Override
    public void start(Promise<Void> future) {
        router = Router.router(vertx);
        factory = DaggerObjectFactory.builder()
                .injectConfig(config())
                .injectVertx(vertx)
                .injectRouter(router)
                .build();
        tracer = factory.tracer();
        super.start(future);
        try {
            SandboxAPI sandboxAPI = factory.sandboxAPI();
            sandboxAPI.setUpAPI();
        } catch (Exception e) {
            LOGGER.error("Failed to setup API's  at start up", e);
            future.fail(e);
        }
        this.router.route("/*").failureHandler(this::failureHandler);
    }

    @Override
    public void failureHandler(RoutingContext routingContext) {
        // For handling errors globally
        Throwable err = routingContext.failure();
        NewRelic.noticeError(err);
        HttpServerRequest request = routingContext.request();
        String path = request.path();
        MultiMap headers = request.headers();
        String body = routingContext.getBodyAsString();
        RequestContext requestContext = RequestContext.getConfig();
        SandboxError sandboxError = SandboxError.unknownException("Something went wrong. Reason: " + err.getMessage());
        HttpServerResponse serverResponse = routingContext.response();
        serverResponse.putHeader("content-type", "application/json");
        if (requestContext == null) {
            LOGGER.error("Unable to initialise requestContext, raising bad request for path {}, request body {}, request headers: {}",
                    path, body, headers, err);
            sandboxError = SandboxError.unknownException("Unable to initialise requestContext");
        } else {
            requestContext.error(LOGGER, "Error processing request with for path {} , request body {}", path, body, err);
            serverResponse.putHeader(BBCommonVerticle.REQUEST_ID_HEADER, requestContext.getTrackingId());
        }
        serverResponse.setStatusCode(SandboxError.INTERNAL_SERVER_ERROR).end(Json.encodePrettily(sandboxError.toJson()));
    }
}
