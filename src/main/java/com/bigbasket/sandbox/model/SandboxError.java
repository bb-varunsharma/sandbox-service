package com.bigbasket.sandbox.model;

import java.util.Set;

import com.bigbasket.core.common.JsonSerializable;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SandboxError extends Exception implements JsonSerializable {

    public static final int BAD_REQUEST = 400;

    public static final int UNAUTHORIZED = 401;

    public static final int NOT_FOUND = 404;

    public static final int INTERNAL_SERVER_ERROR = 500;

    @JsonProperty("code")
    private int errorCode;

    @JsonProperty("code_str")
    private String codeStr;

    @JsonProperty("type")
    private String type;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("display_msg")
    private String displayMessage;

    @JsonProperty("http_code")
    private int httpCode;

    public SandboxError(int errorCode, String type, String message, String displayMessage, int httpCode) {
        this.errorCode = errorCode;
        this.codeStr = "SB-" + errorCode;
        this.type = type;
        this.message = message;
        this.displayMessage = displayMessage;
        this.httpCode = httpCode;
    }

    public static SandboxError missingHeaders(Set<String> requiredHeaders) {
        return new SandboxError(SandboxError.BAD_REQUEST,
                "MISSING_HEADERS",
                "You are missing one of the Required Headers " + requiredHeaders,
                "You are missing one of the Required Headers " + requiredHeaders,
                SandboxError.BAD_REQUEST);
    }

    public static SandboxError badRequest(String message) {
        return new SandboxError(SandboxError.BAD_REQUEST,
                "Bad Request",
                "Bad Request: " + message,
                "Bad Request: " + message,
                SandboxError.BAD_REQUEST);
    }

    public static SandboxError notFoundError(String message) {
        return new SandboxError(SandboxError.BAD_REQUEST,
                "Not found",
                "Not found: " + message,
                "Not found: " + message,
                SandboxError.NOT_FOUND);
    }

    public static SandboxError unknownException(String error) {
        return new SandboxError(SandboxError.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Internal Server Error " + error,
                "Internal Server Error " + error,
                SandboxError.INTERNAL_SERVER_ERROR);
    }

    public JsonObject toJson() {
        JsonObject json = JsonObject.mapFrom(this);
        json.remove("cause");
        json.remove("suppressed");
        json.remove("localizedMessage");
        json.remove("stackTrace");
        return new JsonObject().put("errors", new JsonArray().add(json));
    }
}
