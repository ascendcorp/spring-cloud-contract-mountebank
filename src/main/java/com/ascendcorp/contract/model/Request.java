package com.ascendcorp.contract.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.contract.spec.internal.Body;
import org.springframework.cloud.contract.spec.internal.Headers;
import org.springframework.cloud.contract.spec.internal.QueryParameters;

import java.util.Optional;

@Data
@Builder
public class Request {
    private String method;
    private String url;
    private boolean isUrlRegex;
    private Optional<QueryParameters> queryParameters;
    private Optional<Headers> headers;
    private Optional<Body> body;

}
