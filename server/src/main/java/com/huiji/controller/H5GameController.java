package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.dto.GameDto;
import com.huiji.entity.Game;
import com.huiji.entity.GamePlay;
import com.huiji.entity.GamePrize;
import com.huiji.entity.Member;
import com.huiji.repository.GamePlayRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberContext;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    private final GamePlayRepository playRepository;

    /** 可玩的游戏列表(状态 ENABLED 且在有效期内)，带当前会员剩余次数 */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long tenantId,
                                                  @RequestParam(required = false) String type,
                                                  HttpServletRequest req) {
        Long tid = resolveTenantId(tenantId, req);
        Long memberId = MemberContext.tryMemberId(req, memberTokenUtil);
        List<Game> all = gameService.list(tid, "ENABLED");
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = all.stream()
                .filter(g -> now.isAfter(g.getStartTime()) && now.isBefore(g.getEndTime()))
                .filter(g -> type == null || type.isBlank() || type.equalsIgnoreCase(g.getType()))
                .map(g -> gameVO(g, remainingOf(tid, memberId, g)))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    /** 游戏详情(含奖品列表与剩余次数) */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id,
                                           @RequestParam(required = false) Long tenantId,
                                           HttpServletRequest req) {
        Long tid = resolveTenantId(tenantId, req);
        Long memberId = MemberContext.tryMemberId(req, memberTokenUtil);
        Game g = gameService.get(tid, id);
        List<GamePrize> prizes = gameService.prizes(tid, id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("game", gameVO(g, remainingOf(tid, memberId, g)));
        data.put("prizes", prizes);
        data.put("remaining", remainingOf(tid, memberId, g));
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

    /** 计算会员当日剩余可玩次数（未登录返回每日上限） */
    private long remainingOf(Long tenantId, Long memberId, Game g) {
        int dailyLimit = g.getDailyLimit() == null ? 1 : g.getDailyLimit();
        if (memberId == null) return dailyLimit;
        String dayKey = LocalDate.now().toString();
        long todayCount = playRepository.countByTenantIdAndGameIdAndMemberIdAndDayKey(tenantId, g.getId(), memberId, dayKey);
        return Math.max(0, dailyLimit - todayCount);
    }

    /** 游戏视图: 序列化常用字段并附带剩余次数 */
    private Map<String, Object> gameVO(Game g, long remaining) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", g.getId());
        vo.put("name", g.getName());
        vo.put("type", g.getType());
        vo.put("subtitle", g.getSubtitle());
        vo.put("coverImage", g.getCoverImage());
        vo.put("bgImage", g.getBgImage());
        vo.put("dailyLimit", g.getDailyLimit());
        vo.put("totalLimit", g.getTotalLimit());
        vo.put("pointsCost", g.getPointsCost());
        vo.put("rules", g.getRules());
        vo.put("startTime", g.getStartTime());
        vo.put("endTime", g.getEndTime());
        vo.put("status", g.getStatus());
        vo.put("storeId", g.getStoreId());
        vo.put("remaining", remaining);
        return vo;
    }

    /** 从 query 参数或 member token 推断 tenantId */
    private Long resolveTenantId(Long tenantId, HttpServletRequest req) {
        if (tenantId != null) return tenantId;
        return MemberContext.tryTenantId(req, memberTokenUtil);
    }

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
        return MemberContext.require(req, memberTokenUtil, memberId ->
                memberRepository.findById(memberId)
                        .filter(x -> !Boolean.TRUE.equals(x.getDeleted()))
                        .map(Member::getTenantId)
                        .orElse(null));
    }
}
