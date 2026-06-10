package com.eventhub.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "账号不能为空") String identifier,
        @NotBlank(message = "密码不能为空") String password,
        @NotNull(message = "客户端类型不能为空") ClientType clientType) {}
