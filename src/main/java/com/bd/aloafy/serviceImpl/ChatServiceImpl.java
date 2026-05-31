package com.bd.aloafy.serviceImpl;

import com.bd.aloafy.dto.request.ChatRequest;
import com.bd.aloafy.dto.response.ChatResponse;
import com.bd.aloafy.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    // Utilisation de RestTemplate pour envoyer la requête HTTP
    private final RestTemplate restTemplate = new RestTemplate();

    // L'URL de base de ton API sur Hugging Face (sans le /docs)
    private final String API_URL = "https://alaemoussi-aloafy-api.hf.space";

    @Override
    public ChatResponse generateReply(ChatRequest request) {
        String userMessage = request.getMessage();

        // 1. Préparation du JSON envoyé à ton FastAPI
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", userMessage); // Si ton FastAPI attend 'prompt', remplace "message" par "prompt"

        try {
            // 2. Appel POST vers ton endpoint /chat/ (comme vu sur l'image)
            Map<String, Object> apiResponse = restTemplate.postForObject(
                    API_URL + "/chat/",
                    requestBody,
                    Map.class
            );

            // 3. Récupération de la réponse textuelle de ton IA
            String botReply = "";
            if (apiResponse != null) {
                // Adapte "response" ou "reply" selon la clé retournée par ton FastAPI
                botReply = (String) apiResponse.get("response");
                if (botReply == null) {
                    botReply = (String) apiResponse.get("reply");
                }
            }

            return new ChatResponse(botReply);

        } catch (Exception e) {
            // Gestion d'erreur si ton espace Hugging Face met du temps à se réveiller (Cold Start)
            return new ChatResponse("Erreur de connexion avec ton API Hugging Face. Vérifie qu'elle n'est pas en veille : " + e.getMessage());
        }
    }
}