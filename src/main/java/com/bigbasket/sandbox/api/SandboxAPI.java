package com.bigbasket.sandbox.api;

import java.util.HashMap;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bigbasket.core.common.RequestContext;
import com.bigbasket.core.common.di.VerticleScope;
import com.bigbasket.core.controller.BBCommonVerticle;
import com.bigbasket.sandbox.model.SandboxError;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

@VerticleScope
public class SandboxAPI {

    private static final Set<String> LOGGER_HEADERS = Set.of("X-Caller", "X-Entry-Context",
            "X-Entry-Context-Id", "X-Tracker", "X-Forwarded-For", "Cookie", "BB-Decoded-VID",
            "BB-Decoded-AID", "BB-Decoded-MID", "BB-Decoded-UID", "X-Project", "X-Tenant-Id", "X-Timestamp");

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "X-Timestamp", "X-Caller", "X-Entry-Context",
            "X-Entry-Context-Id", "X-Tracker"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(SandboxAPI.class);

    public static final String INTERNAL_V1_PREFIX = "/sandbox/internal/v1";

    public static final String HEALTH_CHECK = INTERNAL_V1_PREFIX + "/health";

    public static final String CREATE_AEROSPIKE_DATA = INTERNAL_V1_PREFIX + "/test/aerospike";

    @Inject
    protected Router router;

    @Inject
    protected JsonObject config;

    @Inject
    public SandboxAPI() {
    }

    public void setUpAPI() {
        this.router.route("/*").handler(routingContext -> {
            RequestContext requestContext = RequestContext.getConfig();
            HttpServerResponse serverResponse = routingContext.response();
            serverResponse.putHeader("content-type", "application/json");
            serverResponse.putHeader(BBCommonVerticle.REQUEST_ID_HEADER, requestContext.getTrackingId());

            logRequest(routingContext, requestContext);

            routingContext.put("serverResponse", serverResponse);
            routingContext.next();
        });

        this.router.route("/*").handler(routingContext -> {
            checkHeaders(routingContext);
            routingContext.next();
        });

    }

    private void checkHeaders(RoutingContext routingContext) {
        RequestContext requestContext = RequestContext.getConfig();
        HttpServerRequest serverRequest = routingContext.request();
        Set<String> headerNames = serverRequest.headers().names();
        boolean checkHeaders = config.getBoolean("CHECK_HEADERS", Boolean.TRUE);
        String url = serverRequest.path().trim();
        if (checkHeaders && !HEALTH_CHECK.equals(url)) {
            if (headerNames.containsAll(REQUIRED_HEADERS)) {
                HttpServerResponse serverResponse = routingContext.response();
                serverResponse.putHeader("content-type", "application/json");
                serverResponse.putHeader(BBCommonVerticle.REQUEST_ID_HEADER, requestContext.getTrackingId());
                routingContext.put("serverResponse", serverResponse);
                routingContext.next();
            } else {
                HttpServerResponse serverResponse = routingContext.response();
                serverResponse.putHeader("content-type", "application/json");
                serverResponse.putHeader(BBCommonVerticle.REQUEST_ID_HEADER, requestContext.getTrackingId());
                SandboxError missingHeaders = SandboxError.missingHeaders(REQUIRED_HEADERS);
                serverResponse.setStatusCode(missingHeaders.getHttpCode()).end(Json.encodePrettily(missingHeaders.toJson()));
            }
        }
    }

    private void logRequest(RoutingContext routingContext, RequestContext requestContext) {
        HttpServerRequest request = routingContext.request();
        HashMap<String, String> currentHeaders = new HashMap<>();
        for (String header : LOGGER_HEADERS) {
            String headerValue = request.getHeader(header);
            if (StringUtils.isNotEmpty(headerValue)) {
                currentHeaders.put(header, headerValue);
            }
        }

        requestContext.info(LOGGER, "Got request for URL {}, Body {} , Get Params {}, method {}, headers {}",
                request.path(), routingContext.getBodyAsString(), request.params(), request.method().name(), currentHeaders);
    }
}
