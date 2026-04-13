package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class analysisService {

    // Java will automatically grab the URL from your application.properties file!
    @Value("${python.ai.service.url}")
    private String pythonApiUrl;

    public Map<String, Object> analyseVideo(MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.mp4";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Use the dynamic variable here instead of the hardcoded string
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, requestEntity, Map.class);

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Failed to reach Python AI Service at " + pythonApiUrl + " | Error: " + e.getMessage());
            return Map.of("is_fake", false, "confidence", 0.0, "error", "AI Service Offline");
        }
    }
}
