package com.eventhub.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Pattern(regexp = "^$|^[A-Za-z0-9_]{4,32}$", message = "用户名应为 4 至 32 位字母、数字或下划线") String username,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的中国大陆手机号") String phone,

        @NotBlank(message = "密码不能为空") @Size(min = 8, max = 72, message = "密码长度应为 8 至 72 位") String password,

        @NotBlank(message = "昵称不能为空") @Size(max = 64, message = "昵称不能超过 64 位") String displayName) {

    @AssertTrue(message = "用户名和手机号至少填写一项") @Schema(hidden = true)
    public boolean isIdentifierPresent() {
        return hasText(username) || hasText(phone);
    }

    public String normalizedUsername() {
        return hasText(username) ? username.trim() : null;
    }

    public String normalizedPhone() {
        return hasText(phone) ? phone.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
