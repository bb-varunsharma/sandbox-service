package com.bigbasket.sandbox.webclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bigbasket.core.BBWebClient;
import com.bigbasket.core.telemetry.tracing.BBTracer;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.ext.web.client.WebClient;

public class HulkWebClient extends BBWebClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HulkWebClient.class);

    private static final Integer POOL_SIZE = 20;

    private static final long TIMEOUT = 50000000000L;

    private static final int INT_TIMEOUT = 10000;

    private static final int RETRIES = 1;

    public static final Integer CONNECT_TIMEOUT = 200;

    public static final Integer IDLE_TIMEOUT = 200;

    public HulkWebClient(JsonObject config, io.vertx.reactivex.core.Vertx vertx) {
        String hulkSvcHost = config.getString("HULK_SVC_HOST", "qa-svc.bigbasket.com");
        int hulkSvcPort = config.getInteger("HULK_SVC_PORT", 80);
        int pool_size = config.getInteger("WEB_CLIENT_POOL", POOL_SIZE);
        LOGGER.debug("Hulk Client: " + hulkSvcHost + " : " + hulkSvcPort);
        WebClientOptions hulkWebClientOptions = new WebClientOptions().setUserAgent("hulk/1.0.0")
                .setMaxPoolSize(pool_size)
                .setDefaultHost(hulkSvcHost)
                .setDefaultPort(hulkSvcPort);

        this.webClient = WebClient.create(vertx, hulkWebClientOptions);

        LOGGER.debug("HulkWebClient created:" + this.webClient);

    }

    @Override
    public void setBbTracer(BBTracer bbTracer) {
        this.bbTracer = bbTracer;
    }

//    public HttpRequest<Buffer> addHeaders(HttpRequest<Buffer> httpRequest, RequestContext requestContext) {
//        // TODO: Set Timestamp, can we set ServiceName too
//        requestContext.setTimestamp(null);
//        return httpRequest.putHeader(BBCommonVerticle.REQUEST_ID_HEADER, requestContext.getTrackingId())
//                .putHeader(BBCommonVerticle.REQUEST_SERVICE_NAME, requestContext.getServiceName())
//                .putHeader(BBCommonVerticle.REQUEST_ENTRY_CONTEXT, requestContext.getEntryContext())
//                .putHeader(BBCommonVerticle.REQUEST_ENTRY_CONTEXT_ID, requestContext.getEntryContextId())
//                .putHeader(BBCommonVerticle.REQUEST_CALLER, requestContext.getServiceName())
//                .putHeader(BBCommonVerticle.REQUEST_TIMESTAMP, requestContext.getTimestamp())
//                .putHeader(RequestContext.REQUEST_PROJECT, requestContext.getProject())
//                .putHeader(RequestContext.REQUEST_TENANT, String.valueOf(requestContext.getTenant()));
//    }
//
//    public Single<JsonObject> post(JsonObject jsonObject, String url, long timeout) {
//        RequestContext requestContext = RequestContext.getConfig();
//
//        HttpRequest<Buffer> bufferHttpRequest = addHeaders(webClient.post(url), requestContext);
//        bufferHttpRequest.putHeader("X-Caller", "sandbox")
//                .putHeader("content-type", "application/json")
//                .putHeader("X-Channel", "BB-Instant-Android");
//        LOGGER.info(jsonObject.toString());
//        return bufferHttpRequest
//                .timeout(timeout).rxSendJsonObject(jsonObject)
//                .flatMap(bufferHttpResponse -> {
//                    if (bufferHttpResponse.statusCode() != 200) {
//                        requestContext.error(LOGGER, "Error for URL {} from Hulk service, response {}, status_code {} ", url, bufferHttpResponse.bodyAsString(), bufferHttpResponse.statusCode());
//                        NewRelic.noticeError(bufferHttpResponse.statusMessage());
//                        return Single.error(HulkErrors.errorFromHulk(bufferHttpResponse.bodyAsString()));
//                    }
//                    return Single.just(bufferHttpResponse);
//                }).map(HttpResponse::bodyAsJsonObject);
//    }
}
