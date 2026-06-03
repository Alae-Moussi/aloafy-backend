package com.bd.aloafy.service;

public interface GenericGeminiService {
    <T> T generateContent(String prompt, Class<T> responseType);
}