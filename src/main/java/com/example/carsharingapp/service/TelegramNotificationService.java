package com.example.carsharingapp.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class TelegramNotificationService implements NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final String TELEGRAM_API_URL =
            "https://api.telegram.org/bot8307707648:AAGRNC6QgS_zxOyHoBAAQvAcwVf6FlPHcaE/{method}";
    private final RestTemplate restTemplate;
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.chat.id}")
    private String chatId;

    @Override
    public void sendMessage(String message) {
        String url = TELEGRAM_API_URL.replace("{token}", botToken)
                .replace("{method}", "sendMessage");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("text", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            LOGGER.info("Successfully sent message to Telegram chat ID: {}", chatId);
        } catch (RestClientException e) {
            LOGGER.error("Failed to send Telegram message to chat ID: {}. Error: {}", chatId,
                    e.getMessage());
        }
    }
}
