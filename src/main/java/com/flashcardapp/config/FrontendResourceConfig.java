package com.flashcardapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontendResourceConfig implements WebMvcConfigurer {

    private static final String TEMPLATE_URL_PATTERN = "/views/**";
    private static final String TEMPLATE_CLASSPATH_LOCATION = "classpath:/templates/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(TEMPLATE_URL_PATTERN)
                .addResourceLocations(TEMPLATE_CLASSPATH_LOCATION);
    }
}
