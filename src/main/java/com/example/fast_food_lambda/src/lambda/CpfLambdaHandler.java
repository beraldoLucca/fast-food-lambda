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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CpfLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String API_URL = "http://host.docker.internal:8080/api/v1/customer/";
    private static final String SECRET_NAME = "eks-service-url";
    private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

    //para ler a variavel de ambiente da aws:
    //private static final String API_URL = System.getenv("API_URL");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String cpf = event.getPathParameters().get("cpf");
        context.getLogger().log("Recebendo CPF: " + cpf);

        String apiUrl = getSecretValue();
        if (apiUrl == null) {
            return createErrorResponse(500, "Erro ao obter a URL da API.");
        }

        String responseBody;
        int statusCode;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl + cpf);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                responseBody = EntityUtils.toString(response.getEntity());
                statusCode = response.getCode();
            }
        } catch (IOException | ParseException e) {
            context.getLogger().log("Erro ao chamar API: " + e.getMessage());
            responseBody = "{\"error\": \"Erro interno ao chamar API\"}";
            statusCode = 500;
        }

        return createSuccessResponse(statusCode, responseBody);
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return headers;
    }

    private String getSecretValue() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(CpfLambdaHandler.SECRET_NAME)
                    .build();
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            return response.secretString();
        } catch (Exception e) {
            System.err.println("Erro ao buscar segredo: " + e.getMessage());
            return null;
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody("{\"error\": \"" + message + "\"}")
                .withHeaders(getHeaders());
    }

    private APIGatewayProxyResponseEvent createSuccessResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(getHeaders());
    }

}