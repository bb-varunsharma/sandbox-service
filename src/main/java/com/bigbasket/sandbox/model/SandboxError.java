package com.bigbasket.sandbox.model;

import java.util.Set;

import com.bigbasket.core.common.JsonSerializable;
import com.fasterxml.jackson.annotation.JsonProperty;

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
                "You are Missing any Required Headers " + requiredHeaders,
                "You are Missing any Required Header",
                SandboxError.BAD_REQUEST);
    }

    public static SandboxError unknownException(String error) {
        return new SandboxError(SandboxError.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "INTERNAL_SERVER_ERROR " + error,
                "INTERNAL SERVER ERROR",
                SandboxError.INTERNAL_SERVER_ERROR);
    }

}
