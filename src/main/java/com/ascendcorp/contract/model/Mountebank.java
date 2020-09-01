package com.ascendcorp.contract.model;

import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cloud.contract.spec.Contract;
import org.springframework.cloud.contract.spec.internal.Body;
import org.springframework.cloud.contract.spec.internal.DslProperty;
import org.springframework.cloud.contract.spec.internal.RegexProperty;
import org.springframework.cloud.contract.spec.internal.Url;
import org.springframework.cloud.contract.verifier.util.MapConverter;

import java.util.*;
import java.util.regex.Pattern;

@Data
public class Mountebank {

    public static final String OPERATION_MATCHES = "matches";
    public static final String OPERATION_EQUALS = "equals";
    public static final String OPERATION_AND = "and";
    public static final String OPERATION_BODY = "body";
    public static final String OPERATION_PREDICATES = "predicates";
    public static final String OPERATION_RESPONSES = "responses";
    public static final String OPERATION_STATUS_CODE = "statusCode";
    public static final String OPERATION_IS = "is";
    public static final String OPERATION_HEADERS = "headers";
    public static final String OPERATION_BEHAVIORS = "_behaviors";
    public static final String OPERATION_WAIT = "wait";
    public static final String OPERATION_PATH = "path";
    public static final String OPERATION_METHOD = "method";
    public static final String OPERATION_QUERY = "query";
    public static final String FILE_TYPE = ".ejs";
    private Request request;
    private Response response;

    public Mountebank(Contract dsl) {
        Url url = Optional.ofNullable(dsl.getRequest().getUrl()).orElse(dsl.getRequest().getUrlPath());
        this.request = Request.builder()
                .method(dsl.getRequest().getMethod().getClientValue().toString())
                .url(url.getClientValue().toString())
                .isUrlRegex(url.getClientValue() instanceof Pattern || url.getClientValue() instanceof RegexProperty)
                .queryParameters(Optional.ofNullable(url.getQueryParameters()))
                .headers(Optional.ofNullable(dsl.getRequest().getHeaders()))
                .body(Optional.ofNullable(getBody(dsl.getRequest().getBody())))
                .build();

        this.response = Response.builder()
                .body(Optional.ofNullable(getBody(dsl.getResponse().getBody())).orElse(new Body("")).getClientValue().toString())
                .status(dsl.getResponse().getStatus().getClientValue().toString())
                .delay(Optional.ofNullable(dsl.getResponse().getDelay()).map(DslProperty<Integer>::getClientValue))
                .headers(Optional.ofNullable(dsl.getResponse().getHeaders()))
                .build();
    }

    private Body getBody(Body bodyIn) {
        if (bodyIn != null) {
            Object body = MapConverter.getStubSideValues(bodyIn);
            if (body instanceof Map) {
                HashMap<String, String> map = new HashMap<>();

                ((Map)body).entrySet().forEach(c -> {
                    if (((Map.Entry) c).getValue() != null) {
                        String key = ((Map.Entry) c).getKey().toString();
                        String value = ((Map.Entry) c).getValue().toString();
                        map.put(key, value);
                    }
                });

                return new Body(new JSONObject(map).toString());
            }
            else if (body instanceof List) {
                List result = new ArrayList();
                ((List) body).forEach(c-> {
                    if (c instanceof Map) {
                        result.add(MapConverter.getStubSideValues(c));
                    }
                    else {
                        result.add(c);
                    }
                });
                return new Body(new JSONArray(result).toString());

            }
        }

        return bodyIn;
    }



}
