package com.ascendcorp.contract.converter;

import com.ascendcorp.contract.model.Mountebank;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.cloud.contract.spec.Contract;
import org.springframework.cloud.contract.spec.internal.Header;
import org.springframework.cloud.contract.spec.internal.QueryParameter;
import org.springframework.cloud.contract.spec.internal.QueryParameters;
import org.springframework.cloud.contract.spec.internal.RegexProperty;
import org.springframework.cloud.contract.verifier.converter.StubGenerator;
import org.springframework.cloud.contract.verifier.file.ContractMetadata;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.first;

public class MountebankConverter implements StubGenerator {

    @Override
    public Map<Contract, String> convertContents(String rootName, ContractMetadata contract) {

        List<Contract> httpContracts = httpContracts(contract);
        if (httpContracts.isEmpty()) {
            return new HashMap<>();
        }
        if (contract.getConvertedContract().size() == 1) {
            return Collections.singletonMap(
                    first((List<Contract>) contract.getConvertedContract()),
                    convertASingleContract(first((List<Contract>) contract.getConvertedContract())));
        }
        return convertContracts(httpContracts);
    }

    @Override
    public String generateOutputFileNameForInput(String inputFileName) {
        return inputFileName + Mountebank.FILE_TYPE;
    }

    private List<Contract> httpContracts(ContractMetadata contract) {
        return contract.getConvertedContract().stream()
                .filter(c -> c.getRequest() != null).collect(Collectors.toList());
    }

    private String convertASingleContract(Contract dsl) {
        Mountebank mb = new Mountebank(dsl);
        JSONArray predicates = processRequest(mb);
        JSONArray responses = processResponse(mb);

        JSONObject mbObj = new JSONObject();
        mbObj.put(Mountebank.OPERATION_PREDICATES, predicates);
        mbObj.put(Mountebank.OPERATION_RESPONSES, responses);

        return mbObj.toString(2);
    }

    private JSONArray processResponse(Mountebank mb) {
        JSONObject responseData = new JSONObject();
        responseData.put(Mountebank.OPERATION_STATUS_CODE, mb.getResponse().getStatus());

        responseData.put(Mountebank.OPERATION_BODY, parseJson(mb.getResponse().getBody()));
        extractResponseHeader(mb, responseData);

        JSONObject responseContainer = new JSONObject();
        extractWait(mb, responseContainer);
        responseContainer.put(Mountebank.OPERATION_IS, responseData);

        JSONArray responses = new JSONArray();
        responses.put(responseContainer);
        return responses;
    }

    private Object parseJson(Object body) {
        String bodyStr = body.toString();
        if (StringUtils.isEmpty(bodyStr)) {
            return "";
        }

        if (bodyStr.trim().startsWith("[")) {
            return new JSONArray(bodyStr);
        } else {
            return new JSONObject(bodyStr);
        }
    }

    private JSONArray processRequest(Mountebank mb) {
        JSONObject schemaData = getSchema(mb);
        JSONObject schemaContainer = new JSONObject();
        schemaContainer.put(mb.getRequest().isUrlRegex() ? Mountebank.OPERATION_MATCHES : Mountebank.OPERATION_EQUALS, schemaData);

        JSONArray container = new JSONArray();
        container.put(schemaContainer);

        mb.getRequest().getHeaders().ifPresent(c -> {
            JSONObject headers = getHeaders(c.getEntries());
            container.put(headers);
        });

        mb.getRequest().getBody().ifPresent(c -> {
            JSONObject body = getBody(c.getClientValue().toString());
            container.put(body);
        });

        JSONObject and = new JSONObject();
        and.put(Mountebank.OPERATION_AND, container);

        JSONArray predicates = new JSONArray();
        predicates.put(and);
        return predicates;
    }

    private JSONObject getBody(String body) {
        JSONObject bodyRequest = new JSONObject();
        JSONObject jsonBody = new JSONObject();
        try {
            boolean isArray = body.trim().startsWith("[");
            bodyRequest.put(Mountebank.OPERATION_BODY, isArray ? new JSONArray(body) : new JSONObject(body));
            jsonBody.put(Mountebank.OPERATION_MATCHES, bodyRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonBody;
    }

    private JSONObject getHeaders(Set<Header> headers1) {
        JSONObject headers = new JSONObject();
        headers1.stream()
                .forEach(c-> updateJSONObject(headers, c.getName(), c.getClientValue()));

        Boolean isRegex = headers1.stream()
                .map(c -> c.getClientValue() instanceof RegexProperty)
                .reduce(false, (val1, val2) -> val1 || val2);

        JSONObject headerCondition = new JSONObject();
        updateJSONObject(headerCondition, Mountebank.OPERATION_HEADERS, headers);

        JSONObject headersContainer = new JSONObject();
        updateJSONObject(headersContainer, Boolean.TRUE.equals(isRegex) ? Mountebank.OPERATION_MATCHES : Mountebank.OPERATION_EQUALS, headerCondition);

        return headersContainer;
    }

    private void extractResponseHeader(Mountebank mb, JSONObject responseData) {
        mb.getResponse().getHeaders().ifPresent(c->{
            JSONObject headerRes = new JSONObject();
            c.getEntries().forEach(header -> updateJSONObject(headerRes, header.getName(), header.getClientValue().toString()));
            updateJSONObject(responseData, Mountebank.OPERATION_HEADERS, headerRes);
        });
    }

    private void extractWait(Mountebank mb, JSONObject responseContainer) {
        mb.getResponse().getDelay().ifPresent(time -> {
            JSONObject wait = new JSONObject();
            updateJSONObject(wait, Mountebank.OPERATION_WAIT, time);
            updateJSONObject(responseContainer, Mountebank.OPERATION_BEHAVIORS, wait);
        });
    }

    private void updateJSONObject(JSONObject jsonObject, String name, Object value) {
        try {
            jsonObject.put(name, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getSchema(Mountebank mb) {
        JSONObject schemaData = new JSONObject();

        // Path
        schemaData.put(Mountebank.OPERATION_PATH, mb.getRequest().getUrl());
        // Method
        schemaData.put(Mountebank.OPERATION_METHOD, mb.getRequest().getMethod());
        //Query
        Map<String, String> query = mb.getRequest().getQueryParameters()
                .map(QueryParameters::getParameters).orElse(new ArrayList<>()).stream()
                .collect(Collectors.toMap(QueryParameter::getName, q -> q.getServerValue().toString(), (a, b) -> b));
        if (query.size() > 0) {
            schemaData.put(Mountebank.OPERATION_QUERY, new JSONObject(query));
        }

        return schemaData;
    }

    private Map<Contract, String> convertContracts(List<Contract> contractsWithRequest) {
        Map<Contract, String> convertedContracts = new LinkedHashMap<>();
        for (int i = 0; i < contractsWithRequest.size(); i++) {
            Contract dsl = contractsWithRequest.get(i);
            convertedContracts.put(dsl, convertASingleContract(dsl));
        }
        return convertedContracts;
    }

}
