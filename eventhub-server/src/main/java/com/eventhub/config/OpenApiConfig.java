package com.eventhub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI eventHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventHub 接口文档")
                        .description("EventHub 用户端与管理端 REST API 契约")
                        .version("v1"))
                .servers(List.of(new Server().url("http://localhost:8080").description("本地开发环境")));
    }
}
