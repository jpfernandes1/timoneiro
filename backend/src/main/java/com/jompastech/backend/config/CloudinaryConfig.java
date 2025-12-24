package com.jompastech.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "cloudinary")
@PropertySource(value = "classpath:.env.properties", ignoreResourceNotFound = true)
@Data
public class CloudinaryConfig {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private boolean secure;

    @PostConstruct
    public void init() {
        System.out.println("=== Cloudinary Config (.env.properties) ===");
        System.out.println("Cloud Name: " + cloudName);
        System.out.println("API Key: " + (apiKey != null && !apiKey.isEmpty() ? "***CONFIGURADA***" : "NÃO CONFIGURADA"));
        System.out.println("API Secret: " + (apiSecret != null && !apiSecret.isEmpty() ? "***CONFIGURADA***" : "NÃO CONFIGURADA"));
        System.out.println("Secure: " + secure);

        // Log também as variáveis diretamente do sistema
        System.out.println("CLOUDINARY_CLOUD_NAME do sistema: " + System.getenv("CLOUDINARY_CLOUD_NAME"));
    }
}