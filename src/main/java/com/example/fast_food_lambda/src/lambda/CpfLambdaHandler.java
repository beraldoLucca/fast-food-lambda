package com.example.fast_food_lambda.src.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CpfLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String API_URL = "http://host.docker.internal:8080/api/v1/customer/";
    //para ler a variavel de ambiente da aws:
    //private static final String API_URL = System.getenv("API_URL");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String cpf = event.getPathParameters().get("cpf");
        context.getLogger().log("Recebendo CPF: " + cpf);

        String responseBody;
        int statusCode;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL + cpf);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                responseBody = EntityUtils.toString(response.getEntity());
                statusCode = response.getCode();
            }
        } catch (IOException | ParseException e) {
            context.getLogger().log("Erro ao chamar API: " + e.getMessage());
            responseBody = "{\"error\": \"Erro interno ao chamar API\"}";
            statusCode = 500;
        }

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setStatusCode(statusCode);
        responseEvent.setBody(responseBody);
        responseEvent.setHeaders(getHeaders());

        return responseEvent;
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return headers;
    }
}