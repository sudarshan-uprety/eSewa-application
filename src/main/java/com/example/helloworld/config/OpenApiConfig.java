package com.example.helloworld.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server prodServer = new Server();
        prodServer.setUrl("https://esewaprod.sudarshan-uprety.com.np");
        prodServer.setDescription("Production Server");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Server");

        return new OpenAPI()
                .servers(List.of(prodServer, localServer))
                .info(new Info()
                        .title("eSewa Application API")
                        .version("1.0")
                        .description(
                                "API documentation for the eSewa Spring Boot application with PostgreSQL integration.")
                        .contact(new Contact()
                                .name("Sudarshan Uprety")
                                .url("https://esewaprod.sudarshan-uprety.com.np")));
    }
}
