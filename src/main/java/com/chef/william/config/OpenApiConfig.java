package com.chef.william.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cookingAppOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cooking App API")
                        .description("API for foods, ingredients and recipe management")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
