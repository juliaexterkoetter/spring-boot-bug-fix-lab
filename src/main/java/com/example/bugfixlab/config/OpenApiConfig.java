package com.example.bugfixlab.config;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenApiCustomizer documentedSchemas() {
        return openApi -> {
            // @Size's default minimum otherwise masks the @NotEmpty constraint in the generated schema.
            Schema<?> orderRequest = openApi.getComponents().getSchemas().get("OrderRequest");
            orderRequest.getProperties().get("items").setMinItems(1);
            // Jackson flattens ProblemDetail extensions into the response object.
            var problem = openApi.getComponents().getSchemas().get("ProblemDetail");
            problem.getProperties().remove("properties");
            problem.addProperty("errors", new ArraySchema()
                    .description("Present for request validation failures.")
                    .items(new ObjectSchema()
                            .addProperty("field", new StringSchema().example("items"))
                            .addProperty("message", new StringSchema().example("At least one item is required"))));
        };
    }
}
