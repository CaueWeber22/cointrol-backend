package com.fcproject.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    OpenAPI configOpenAPI(){
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ).addSecuritySchemes("cookieAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE).name("access_token"))
                 .addSecuritySchemes("csrf", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN"))
                 .addSecuritySchemes("refreshCookie", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE).name("refresh_token")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .info(new Info()
                        .title("Cointrol API")
                        .version("v1")
                        .description("JWT via HttpOnly access_token cookie. Authorization Bearer takes precedence when present. "
                                + "All mutations require X-CSRF-TOKEN and the CSRF session cookie: first GET /api/v1/auth/csrf "
                                + "with credentials. Login/refresh/logout return 204 and Set-Cookie, never tokens in JSON. "
                                + "Fetch a new CSRF token after login/refresh. CSRF failures return 403 CSRF_INVALID."));
    }

    @Bean
    org.springdoc.core.customizers.OpenApiCustomizer cookieSecurityContract() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean mutation = !java.util.Set.of(io.swagger.v3.oas.models.PathItem.HttpMethod.GET,
                    io.swagger.v3.oas.models.PathItem.HttpMethod.HEAD,
                    io.swagger.v3.oas.models.PathItem.HttpMethod.OPTIONS).contains(method);
            if (path.equals("/api/v1/auth/csrf")) {
                operation.setSecurity(java.util.List.of());
            } else if (path.equals("/api/v1/auth/refresh")) {
                operation.setSecurity(java.util.List.of(new SecurityRequirement().addList("csrf").addList("refreshCookie")));
            } else if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/logout")
                    || (path.equals("/api/v1/users") && mutation)) {
                operation.setSecurity(java.util.List.of(new SecurityRequirement().addList("csrf")));
            } else if (mutation) {
                operation.setSecurity(java.util.List.of(new SecurityRequirement().addList("cookieAuth").addList("csrf"),
                        new SecurityRequirement().addList("bearerAuth").addList("csrf")));
            }
            if (path.startsWith("/api/v1/auth/") && mutation && operation.getResponses().get("204") != null) {
                operation.getResponses().get("204").addHeaderObject("Set-Cookie", new io.swagger.v3.oas.models.headers.Header()
                        .description("Separate HttpOnly; Secure; SameSite=None cookies: access_token Path=/api/v1, "
                                + "refresh_token Path=/api/v1/auth; no Domain. Logout uses Max-Age=0.")
                        .schema(new io.swagger.v3.oas.models.media.StringSchema()));
            }
        }));
    }
}
