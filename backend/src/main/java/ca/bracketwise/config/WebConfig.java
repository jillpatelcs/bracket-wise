package ca.bracketwise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${bracketwise.cors-origins}")
    private List<String> origins;

    @jakarta.annotation.PostConstruct
    void validateOrigins() {
        for (String origin : origins) {
            URI uri;
            try {
                uri = URI.create(origin);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("CORS origins must be explicit HTTP(S) origins.", ex);
            }
            if (origin.contains("*")
                    || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getRawUserInfo() != null) {
                throw new IllegalStateException("CORS origins must be explicit HTTP(S) origins without paths or wildcards.");
            }
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "X-Request-Id")
                .maxAge(3600);
    }
}
