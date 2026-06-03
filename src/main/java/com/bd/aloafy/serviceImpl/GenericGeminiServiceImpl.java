package com.bd.aloafy.serviceImpl;

import com.bd.aloafy.service.GenericGeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class GenericGeminiServiceImpl implements GenericGeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GenericGeminiServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String API_URL = "https://alaemoussi-aloafy-api.hf.space/chat/";

    public GenericGeminiServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T generateContent(String prompt, Class<T> responseType) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        logger.info("Envoi du prompt à Aloafy API...");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", 1);
        requestBody.put("message", prompt);

        try {
            Map<?, ?> apiResponse = restTemplate.postForObject(API_URL, requestBody, Map.class);
            logger.info("Réponse reçue : {}", apiResponse);

            String botReply = null;
            if (apiResponse != null && apiResponse.containsKey("response")) {
                botReply = String.valueOf(apiResponse.get("response"));
            }

            if (botReply == null || botReply.trim().isEmpty()) {
                throw new RuntimeException("Réponse vide de l'API");
            }

            return parseResponse(botReply, responseType);

        } catch (Exception ex) {
            logger.error("Erreur : {}", ex.getMessage());
            if (responseType == String.class) {
                return responseType.cast("Erreur: " + ex.getMessage());
            }
            throw new RuntimeException("Aloafy API error: " + ex.getMessage(), ex);
        }
    }

    private <T> T parseResponse(String response, Class<T> responseType) {
        if (responseType == String.class) {
            return responseType.cast(response);
        }

        try {
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            return objectMapper.readValue(json.trim(), responseType);
        } catch (Exception ex) {
            logger.error("JSON non conforme pour {} : {}", responseType.getSimpleName(), response);
            throw new RuntimeException("Failed to parse response: " + ex.getMessage(), ex);
        }
    }
}