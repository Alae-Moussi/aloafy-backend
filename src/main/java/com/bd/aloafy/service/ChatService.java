package com.bd.aloafy.service;

import com.bd.aloafy.dto.request.ChatRequest;
import com.bd.aloafy.dto.response.ChatResponse;

public interface ChatService {
    ChatResponse generateReply(ChatRequest request);
}