package com.flashcardapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flashcardapp.dto.TranslationSuggestionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TranslationSuggestionService {

    private static final String PROVIDER_AZURE = "azure";
    private static final int MAX_INPUT_LENGTH = 240;

    private final RestClient restClient;
    private final String provider;
    private final String defaultTargetLanguage;
    private final String azureKey;
    private final String azureRegion;

    public TranslationSuggestionService(@Value("${app.translation.provider:none}") String provider,
                                        @Value("${app.translation.default-target:vi}") String defaultTargetLanguage,
                                        @Value("${app.translation.azure.endpoint:https://api.cognitive.microsofttranslator.com}") String azureEndpoint,
                                        @Value("${app.translation.azure.key:}") String azureKey,
                                        @Value("${app.translation.azure.region:}") String azureRegion) {
        this.provider = normalizeProvider(provider);
        this.defaultTargetLanguage = normalizeLanguage(defaultTargetLanguage, "vi");
        this.azureKey = azureKey == null ? "" : azureKey.trim();
        this.azureRegion = azureRegion == null ? "" : azureRegion.trim();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(azureEndpoint))
                .requestFactory(requestFactory)
                .build();
    }

    public TranslationSuggestionResponse suggest(String text, String sourceLanguage, String targetLanguage) {
        String normalizedInput = normalizeInput(text);
        if (normalizedInput.isBlank()) {
            return TranslationSuggestionResponse.disabled("Vui lòng nhập từ vựng trước.");
        }
        if (normalizedInput.length() > MAX_INPUT_LENGTH) {
            return TranslationSuggestionResponse.disabled("Nội dung hơi dài, hãy nhập một từ hoặc cụm ngắn.");
        }
        if (!PROVIDER_AZURE.equals(provider)) {
            return TranslationSuggestionResponse.disabled("Chưa bật gợi ý dịch.");
        }
        if (azureKey.isBlank()) {
            return TranslationSuggestionResponse.disabled("Thiếu Azure Translator key.");
        }

        String source = normalizeNullableLanguage(sourceLanguage);
        String target = normalizeLanguage(targetLanguage, defaultTargetLanguage);
        return translate(normalizedInput, source, target);
    }

    private TranslationSuggestionResponse translate(String normalizedInput, String source, String target) {
        AzureTranslationResult result = translateWithAzure(normalizedInput, source, target);
        if (result.translatedText().isBlank()) {
            TranslationSuggestionResponse response = baseResponse(normalizedInput, source, target);
            response.setMessage(result.message().isBlank() ? "Chưa tìm được gợi ý phù hợp." : result.message());
            return response;
        }

        TranslationSuggestionResponse response = baseResponse(normalizedInput, source, target);
        response.setDetectedLanguage(result.detectedLanguage());
        response.setSuggestions(uniqueSuggestions(result.translatedText()));
        return response;
    }

    private AzureTranslationResult translateWithAzure(String normalizedInput, String source, String target) {
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/translate")
                                .queryParam("api-version", "3.0")
                                .queryParam("to", target);
                        if (source != null && !source.isBlank()) {
                            uriBuilder.queryParam("from", source);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Ocp-Apim-Subscription-Key", azureKey)
                    .header("X-ClientTraceId", UUID.randomUUID().toString());
            if (!azureRegion.isBlank() && !"global".equalsIgnoreCase(azureRegion)) {
                request.header("Ocp-Apim-Subscription-Region", azureRegion);
            }

            List<Map<String, String>> body = List.of(Map.of("Text", normalizedInput));
            JsonNode response = request.body(body).retrieve().body(JsonNode.class);
            JsonNode firstResult = response == null || !response.isArray() || response.isEmpty() ? null : response.get(0);
            JsonNode firstTranslation = firstResult == null ? null : firstResult.path("translations").path(0);
            if (firstTranslation == null || firstTranslation.isMissingNode()) {
                return AzureTranslationResult.empty();
            }

            String translatedText = firstTranslation.path("text").asText("");
            String detected = firstResult.path("detectedLanguage").path("language").asText(null);
            return new AzureTranslationResult(trimToEmpty(translatedText), detected, "");
        } catch (RestClientResponseException exception) {
            return AzureTranslationResult.empty(messageForAzureStatus(exception.getStatusCode().value()));
        } catch (Exception ignored) {
            return AzureTranslationResult.empty("Dịch đang phản hồi chậm. Thử lại sau một chút.");
        }
    }

    private String messageForAzureStatus(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return "Azure từ chối key hoặc region. Kiểm tra lại Key 1 và Location của Translator.";
        }
        if (statusCode == 429) {
            return "Azure đang giới hạn lượt gọi. Thử lại sau một chút.";
        }
        if (statusCode >= 500) {
            return "Azure đang lỗi tạm thời. Thử lại sau.";
        }
        return "Azure chưa trả về gợi ý. Kiểm tra cấu hình Translator.";
    }

    private TranslationSuggestionResponse baseResponse(String inputText, String sourceLanguage, String targetLanguage) {
        TranslationSuggestionResponse response = new TranslationSuggestionResponse();
        response.setEnabled(true);
        response.setInputText(inputText);
        response.setSourceLanguage(sourceLanguage);
        response.setTargetLanguage(targetLanguage);
        return response;
    }

    private List<String> uniqueSuggestions(String translatedText) {
        Set<String> suggestions = new LinkedHashSet<>();
        suggestions.add(trimToEmpty(translatedText));

        for (String part : translatedText.split("[;,/]|\\n")) {
            String value = trimToEmpty(part);
            if (!value.isBlank()) {
                suggestions.add(value);
            }
        }

        return suggestions.stream()
                .filter(value -> !value.isBlank())
                .sorted(Comparator.comparingInt(String::length))
                .limit(4)
                .toList();
    }

    private String normalizeProvider(String value) {
        return value == null ? "none" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeInput(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLanguage(String value, String fallback) {
        String normalized = normalizeNullableLanguage(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeNullableLanguage(String value) {
        if (value == null || value.trim().isEmpty() || "auto".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null || value.isBlank()
                ? "https://api.cognitive.microsofttranslator.com"
                : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record AzureTranslationResult(String translatedText, String detectedLanguage, String message) {
        static AzureTranslationResult empty() {
            return empty("");
        }

        static AzureTranslationResult empty(String message) {
            return new AzureTranslationResult("", null, message);
        }
    }
}
