package com.seckill.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 文档：http://localhost:9090/swagger-ui.html
 * 右上角 Authorize 填入登录返回的 token 即可调试需要鉴权的接口。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI seckillOpenAPI() {
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("秒杀平台 API")
                        .description("电商秒杀实战项目：RBAC、JWT、Redis 缓存、Lua 预扣库存、Redis Stream 异步下单、订单超时关闭")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
