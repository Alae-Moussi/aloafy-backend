package com.bd.aloafy.controller;

import com.bd.aloafy.dto.request.ChatRequest;
import com.bd.aloafy.dto.response.ChatResponse;
import com.bd.aloafy.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.generateReply(request);
        return ResponseEntity.ok(response);
    }
}