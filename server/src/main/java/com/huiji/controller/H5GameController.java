package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.dto.GameDto;
import com.huiji.entity.Game;
import com.huiji.entity.GamePlay;
import com.huiji.entity.GamePrize;
import com.huiji.entity.Member;
import com.huiji.repository.MemberRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.GameService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** H5 会员端游戏接口: 浏览公开, 玩游戏和记录需 member token。 */
@RestController
@RequestMapping("/api/h5/games")
@RequiredArgsConstructor
public class H5GameController {

    private final GameService gameService;
    private final MemberTokenUtil memberTokenUtil;
    private final MemberRepository memberRepository;

    /** 可玩的游戏列表(状态 ENABLED 且在有效期内) */
    @GetMapping
    public Result<List<Game>> list(@RequestParam Long tenantId,
                                   @RequestParam(required = false) String type) {
        List<Game> all = gameService.list(tenantId, "ENABLED");
        LocalDateTime now = LocalDateTime.now();
        List<Game> result = all.stream()
                .filter(g -> now.isAfter(g.getStartTime()) && now.isBefore(g.getEndTime()))
                .filter(g -> type == null || type.isBlank() || type.equalsIgnoreCase(g.getType()))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    /** 游戏详情(含奖品列表) */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id,
                                           @RequestParam Long tenantId) {
        Game g = gameService.get(tenantId, id);
        List<GamePrize> prizes = gameService.prizes(tenantId, id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("game", g);
        data.put("prizes", prizes);
        return Result.success(data);
    }

    /** 玩游戏(需 member token) */
    @PostMapping("/{id}/play")
    public Result<GameDto.PlayResult> play(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        long memberId = ctx[0];
        long tenantId = ctx[1];
        bindAsMember(memberId, tenantId);
        try {
            return Result.success(gameService.play(tenantId, memberId, id));
        } finally {
            LoginUserHolder.clear();
        }
    }

    /** 我的游戏记录(需 member token) */
    @GetMapping("/{id}/my-plays")
    public Result<List<GamePlay>> myPlays(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        long memberId = ctx[0];
        long tenantId = ctx[1];
        return Result.success(gameService.myPlays(tenantId, memberId, id));
    }

    // ---- 内部方法 ----

    private void bindAsMember(long memberId, long tenantId) {
        LoginUser lu = LoginUser.builder()
                .userId(memberId)
                .tenantId(tenantId)
                .username(String.valueOf(memberId))
                .role("MEMBER")
                .build();
        LoginUserHolder.set(lu);
    }

    /** 解析 memberToken, 返回 [memberId, tenantId] */
    private long[] currentMember(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "请先登录");
        }
        String token = header.substring(7);
        try {
            Claims claims = memberTokenUtil.parse(token);
            if (!"MEMBER".equals(claims.get("type", String.class))) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            Long memberId = claims.get("memberId", Long.class);
            Long tenantId = claims.get("tenantId", Long.class);
            if (memberId == null) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            // tenantId 兜底: 从会员记录取
            if (tenantId == null) {
                Member m = memberRepository.findById(memberId)
                        .filter(x -> Boolean.FALSE.equals(x.getDeleted()))
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
                tenantId = m.getTenantId();
            }
            return new long[]{memberId, tenantId};
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "登录已过期");
        }
    }
}
