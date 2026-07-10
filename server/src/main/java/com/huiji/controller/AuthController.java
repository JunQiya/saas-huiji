package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.AuthDto;
import com.huiji.service.AuditHelper;
import com.huiji.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 鉴权接口 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditHelper auditHelper;

    @PostMapping("/login")
    public Result<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        auditHelper.record("退出登录", "auth:logout");
        return Result.success();
    }

    @GetMapping("/profile")
    public Result<AuthDto.UserProfile> profile() {
        return Result.success(authService.profile());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody AuthDto.PasswordRequest req) {
        authService.changePassword(req);
        return Result.success();
    }
}
