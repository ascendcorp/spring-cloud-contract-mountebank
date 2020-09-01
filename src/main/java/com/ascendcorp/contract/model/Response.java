package com.ascendcorp.contract.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.contract.spec.internal.Headers;

import java.util.Optional;

@Data
@Builder
public class Response {

    private String body;
    private String status;
    private Optional<Integer> delay;
    private Optional<Headers> headers;

}
