package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.AuthDto;
import com.huiji.entity.LoginLog;
import com.huiji.entity.User;
import com.huiji.repository.LoginLogRepository;
import com.huiji.repository.UserRepository;
import com.huiji.security.JwtUtil;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** 鉴权服务: 登录/登出/资料/改密。 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOCK_SECONDS = 300L;

    /** 登录失败计数(内存态, 单实例有效): username -> 失败信息 */
    private final Map<String, LoginFail> loginFails = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        String username = req.getUsername();
        // 防爆破: 检查是否锁定
        LoginFail fail = loginFails.get(username);
        long now = System.currentTimeMillis();
        if (fail != null && fail.lockUntil > now) {
            long left = TimeUnit.MILLISECONDS.toSeconds(fail.lockUntil - now);
            throw new BizException(ErrorCode.BIZ_ERROR, "尝试次数过多, 请 " + left + " 秒后再试");
        }
        User user = userRepository.findByUsernameAndDeletedFalse(req.getUsername())
                .orElseThrow(() -> {
                    recordLogin(null, req.getUsername(), "FAIL", "账号不存在");
                    markFailure(username);
                    return new BizException(ErrorCode.BIZ_ERROR, "账号或密码错误");
                });
        if (!"ACTIVE".equals(user.getStatus())) {
            recordLogin(user.getId(), user.getUsername(), "FAIL", "账号已停用");
            throw new BizException(ErrorCode.BIZ_ERROR, "账号已停用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordLogin(user.getId(), user.getUsername(), "FAIL", "密码错误");
            markFailure(username);
            throw new BizException(ErrorCode.BIZ_ERROR, "账号或密码错误");
        }
        // 登录成功: 清除失败计数
        loginFails.remove(username);
        String token = jwtUtil.generate(user.getId(), user.getTenantId(), user.getUsername(), user.getRole(), firstStoreId(user));
        recordLogin(user.getId(), user.getUsername(), "SUCCESS", "登录成功");

        AuthDto.LoginResponse resp = new AuthDto.LoginResponse();
        resp.setToken(token);
        resp.setExpiresIn(jwtUtil.getExpireSeconds());
        resp.setUser(toProfile(user));
        return resp;
    }

    /** 记录一次登录失败, 连续失败达到阈值则锁定账号一段时间 */
    private void markFailure(String username) {
        long now = System.currentTimeMillis();
        LoginFail f = loginFails.computeIfAbsent(username, k -> new LoginFail());
        if (f.lockUntil > now) return;
        if (now - f.firstFailAt > 60_000L) {
            // 超过 1 分钟的滑动窗口, 重置计数
            f.failCount = 0;
            f.firstFailAt = now;
        }
        f.failCount++;
        if (f.failCount >= MAX_LOGIN_FAILURES) {
            f.lockUntil = now + LOCK_SECONDS * 1000L;
            f.failCount = 0;
        }
    }

    /** 登录失败计数状态 */
    private static class LoginFail {
        long firstFailAt = System.currentTimeMillis();
        int failCount = 0;
        long lockUntil = 0L;
    }

    public AuthDto.UserProfile profile() {
        LoginUser cur = LoginUserHolder.get();
        User user = userRepository.findById(cur.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "用户不存在"));
        return toProfile(user);
    }

    @Transactional
    public void changePassword(AuthDto.PasswordRequest req) {
        LoginUser cur = LoginUserHolder.get();
        User user = userRepository.findById(cur.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    private AuthDto.UserProfile toProfile(User user) {
        AuthDto.UserProfile p = new AuthDto.UserProfile();
        p.setId(user.getId());
        p.setUsername(user.getUsername());
        p.setName(user.getName());
        p.setRole(user.getRole());
        p.setStoreId(firstStoreId(user));
        p.setTenantId(user.getTenantId());
        return p;
    }

    private Long firstStoreId(User user) {
        if (user.getStoreIds() == null || user.getStoreIds().isEmpty()) return null;
        return user.getStoreIds().get(0);
    }

    private void recordLogin(Long userId, String username, String status, String msg) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setStatus(status);
        log.setMessage(msg);
        log.setIp(currentIp());
        loginLogRepository.save(log);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
