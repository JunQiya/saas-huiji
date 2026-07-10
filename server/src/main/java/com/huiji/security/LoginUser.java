package com.huiji.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户上下文(从 token 解析得到)。
 */
@Data
@Builder
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private Long tenantId;
    private String username;
    private String role;
    private Long storeId;
    private String name;
}
