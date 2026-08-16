package com.flashcardapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flashcardapp.dto.HandwritingRecognitionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class HandwritingRecognitionService {

    private static final String PROVIDER_AZURE = "azure";
    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private final RestClient restClient;
    private final String provider;
    private final String azureEndpoint;
    private final String azureKey;

    public HandwritingRecognitionService(@Value("${app.handwriting.provider:none}") String provider,
                                         @Value("${app.handwriting.azure.endpoint:}") String azureEndpoint,
                                         @Value("${app.handwriting.azure.key:}") String azureKey) {
        this.provider = normalizeProvider(provider);
        this.azureEndpoint = trimTrailingSlash(azureEndpoint);
        this.azureKey = azureKey == null ? "" : azureKey.trim();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public HandwritingRecognitionResponse recognize(String imageData, String language) {
        if (!PROVIDER_AZURE.equals(provider)) {
            return HandwritingRecognitionResponse.disabled("Chưa bật nhận dạng chữ viết.");
        }
        if (azureKey.isBlank()) {
            return HandwritingRecognitionResponse.disabled("Thiếu Azure Vision key.");
        }
        if (azureEndpoint.isBlank()) {
            return HandwritingRecognitionResponse.disabled("Thiếu Azure Vision endpoint.");
        }
        if (!hasValidEndpoint(azureEndpoint)) {
            return HandwritingRecognitionResponse.disabled("Azure Vision endpoint chưa hợp lệ. Endpoint cần bắt đầu bằng https://");
        }

        byte[] imageBytes = decodeImageData(imageData);
        if (imageBytes.length == 0) {
            return HandwritingRecognitionResponse.disabled("Bảng vẽ chưa có dữ liệu hợp lệ.");
        }
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            return HandwritingRecognitionResponse.disabled("Nét vẽ hơi lớn, hãy xóa bớt rồi thử lại.");
        }

        return recognizeWithAzure(imageBytes, normalizeLanguage(language));
    }

    private HandwritingRecognitionResponse recognizeWithAzure(byte[] imageBytes, String language) {
        try {
            JsonNode response = restClient.post()
                    .uri(azureAnalyzeUri(language))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Ocp-Apim-Subscription-Key", azureKey)
                    .body(imageBytes)
                    .retrieve()
                    .body(JsonNode.class);

            List<String> lines = readLines(response);
            if (lines.isEmpty()) {
                return HandwritingRecognitionResponse.success("", "Chưa nhận ra chữ nào, thử viết rõ hơn một chút.");
            }
            return HandwritingRecognitionResponse.success(String.join(" ", lines).trim(), "Đã nhận dạng xong.");
        } catch (RestClientResponseException exception) {
            return HandwritingRecognitionResponse.disabled(messageForAzureStatus(exception.getStatusCode().value()));
        } catch (Exception ignored) {
            return HandwritingRecognitionResponse.disabled("Nhận dạng chữ viết đang phản hồi chậm. Thử lại sau một chút.");
        }
    }

    private URI azureAnalyzeUri(String language) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(azureEndpoint)
                .path("/computervision/imageanalysis:analyze")
                .queryParam("overload", "stream")
                .queryParam("api-version", "2024-02-01")
                .queryParam("features", "read");
        if (!language.isBlank()) {
            uriBuilder.queryParam("language", language);
        }
        return uriBuilder.build(true).toUri();
    }

    private List<String> readLines(JsonNode response) {
        List<String> lines = new ArrayList<>();
        if (response == null || response.isMissingNode()) {
            return lines;
        }

        collectModernReadLines(response.path("readResult").path("blocks"), lines);
        collectPageReadLines(response.path("readResult").path("pages"), lines);
        collectPageReadLines(response.path("analyzeResult").path("readResults"), lines);
        return lines.stream()
                .map(this::trimToEmpty)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private void collectModernReadLines(JsonNode blocks, List<String> lines) {
        if (!blocks.isArray()) {
            return;
        }
        blocks.forEach(block -> collectLineTexts(block.path("lines"), lines));
    }

    private void collectPageReadLines(JsonNode pages, List<String> lines) {
        if (!pages.isArray()) {
            return;
        }
        pages.forEach(page -> collectLineTexts(page.path("lines"), lines));
    }

    private void collectLineTexts(JsonNode lineNodes, List<String> lines) {
        if (!lineNodes.isArray()) {
            return;
        }
        lineNodes.forEach(line -> {
            String text = trimToEmpty(line.path("text").asText(""));
            if (!text.isBlank()) {
                lines.add(text);
            }
        });
    }

    private byte[] decodeImageData(String imageData) {
        String normalized = trimToEmpty(imageData);
        if (normalized.isBlank()) {
            return new byte[0];
        }
        int commaIndex = normalized.indexOf(',');
        if (normalized.startsWith("data:") && commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ignored) {
            return new byte[0];
        }
    }

    private String messageForAzureStatus(int statusCode) {
        if (statusCode == 400) {
            return "Azure Vision chưa đọc được ảnh vẽ này. Hãy thử viết to và rõ hơn.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Azure Vision từ chối key hoặc endpoint. Kiểm tra lại Key và Endpoint.";
        }
        if (statusCode == 429) {
            return "Azure Vision đang giới hạn lượt gọi. Thử lại sau một chút.";
        }
        if (statusCode >= 500) {
            return "Azure Vision đang lỗi tạm thời. Thử lại sau.";
        }
        return "Azure Vision chưa trả về kết quả. Kiểm tra cấu hình Vision.";
    }

    private String normalizeProvider(String value) {
        return value == null ? "none" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLanguage(String value) {
        if (value == null || value.trim().isBlank() || "auto".equalsIgnoreCase(value.trim())) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean hasValidEndpoint(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
