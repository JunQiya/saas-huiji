package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.CouponDto;
import com.huiji.dto.GameDto;
import com.huiji.dto.MemberDto;
import com.huiji.entity.Game;
import com.huiji.entity.GamePlay;
import com.huiji.entity.GamePrize;
import com.huiji.repository.GamePlayRepository;
import com.huiji.repository.GamePrizeRepository;
import com.huiji.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** 赢奖小游戏服务: 游戏配置、奖品配置、抽奖核心逻辑、记录与统计。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GamePrizeRepository prizeRepository;
    private final GamePlayRepository playRepository;
    private final CouponService couponService;
    private final MemberService memberService;
    private final AuditHelper auditHelper;

    /** 每(游戏,会员)一把锁, 串行化抽奖次数校验, 防并发刷奖 */
    private final Map<String, Object> playLocks = new ConcurrentHashMap<>();

    private Object lockFor(Long gameId, Long memberId) {
        return playLocks.computeIfAbsent(gameId + ":" + memberId, k -> new Object());
    }

    // ---- 游戏管理 ----

    /** 游戏列表 */
    public List<Game> list(Long tenantId, String status) {
        return gameRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /** 游戏详情(含奖品列表) */
    public Game get(Long tenantId, Long id) {
        Game g = gameRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        return g;
    }

    /** 创建/更新游戏 */
    @Transactional
    public Game save(Long tenantId, GameDto.GameRequest req) {
        Game g;
        if (req.getId() != null) {
            g = gameRepository.findByIdAndTenantIdAndDeletedFalse(req.getId(), tenantId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        } else {
            g = new Game();
            g.setTenantId(tenantId);
        }
        g.setName(req.getName());
        g.setType(req.getType());
        g.setSubtitle(req.getSubtitle());
        g.setCoverImage(req.getCoverImage());
        g.setStartTime(req.getStartTime());
        g.setEndTime(req.getEndTime());
        if (req.getDailyLimit() != null) g.setDailyLimit(req.getDailyLimit());
        if (req.getTotalLimit() != null) g.setTotalLimit(req.getTotalLimit());
        if (req.getPointsCost() != null) g.setPointsCost(req.getPointsCost());
        if (req.getStatus() != null) g.setStatus(req.getStatus());
        g.setRules(req.getRules());
        g.setBgImage(req.getBgImage());
        g.setStoreId(req.getStoreId());
        gameRepository.save(g);
        auditHelper.record(req.getId() == null ? "创建游戏" : "编辑游戏",
                "game:" + g.getId(), g.getName());
        return g;
    }

    /** 切换启停状态(无需回传整对象) */
    @Transactional
    public Game toggleStatus(Long tenantId, Long id, String status) {
        Game g = gameRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        g.setStatus("ENABLED".equals(status) ? "ENABLED" : "DISABLED");
        gameRepository.save(g);
        auditHelper.record("切换游戏状态", "game:" + id, g.getStatus());
        return g;
    }

    /** 删除游戏(连带删奖品) */
    @Transactional
    public void remove(Long tenantId, Long id) {
        Game g = gameRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        g.setDeleted(true);
        gameRepository.save(g);
        // 软删奖品
        List<GamePrize> prizes = prizeRepository.findByGameIdOrderBySortOrderAsc(id);
        for (GamePrize p : prizes) {
            p.setDeleted(true);
            prizeRepository.save(p);
        }
        auditHelper.record("删除游戏", "game:" + id, g.getName());
    }

    // ---- 奖品管理 ----

    /** 奖品列表 */
    public List<GamePrize> prizes(Long tenantId, Long gameId) {
        gameRepository.findByIdAndTenantIdAndDeletedFalse(gameId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        return prizeRepository.findByGameIdAndDeletedFalseOrderBySortOrderAsc(gameId);
    }

    /** 创建/更新奖品 */
    @Transactional
    public GamePrize savePrize(Long tenantId, Long gameId, GameDto.PrizeRequest req) {
        gameRepository.findByIdAndTenantIdAndDeletedFalse(gameId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        GamePrize p;
        if (req.getId() != null) {
            p = prizeRepository.findByGameIdAndId(gameId, req.getId())
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "奖品不存在"));
        } else {
            p = new GamePrize();
            p.setTenantId(tenantId);
            p.setGameId(gameId);
        }
        p.setName(req.getName());
        p.setType(req.getType());
        p.setRefId(req.getRefId());
        p.setRefName(req.getRefName());
        p.setAmount(req.getAmount());
        if (req.getProbability() != null) p.setProbability(req.getProbability());
        p.setImageUrl(req.getImageUrl());
        if (req.getSortOrder() != null) p.setSortOrder(req.getSortOrder());
        prizeRepository.save(p);
        return p;
    }

    /** 删除奖品 */
    @Transactional
    public void removePrize(Long tenantId, Long prizeId) {
        GamePrize p = prizeRepository.findById(prizeId)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "奖品不存在"));
        p.setDeleted(true);
        prizeRepository.save(p);
    }

    // ---- 抽奖核心 ----

    /**
     * 玩游戏核心逻辑:
     * 1. 校验游戏状态和时间
     * 2. 校验每日次数和总次数
     * 3. 如有积分消耗, 扣减会员积分
     * 4. 按概率随机抽奖
     * 5. 中奖则发放奖品(优惠券/积分)
     * 6. 记录 GamePlay
     * 7. 返回 PlayResult
     */
    @Transactional
    public GameDto.PlayResult play(Long tenantId, Long memberId, Long gameId) {
        // 同(游戏,会员)串行, 防止并发同时通过次数校验后重复抽奖
        synchronized (lockFor(gameId, memberId)) {
            return doPlay(tenantId, memberId, gameId);
        }
    }

    private GameDto.PlayResult doPlay(Long tenantId, Long memberId, Long gameId) {
        Game g = gameRepository.findByIdAndTenantIdAndDeletedFalse(gameId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));

        // 1. 校验状态
        if (!"ENABLED".equals(g.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "游戏已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(g.getStartTime()) || now.isAfter(g.getEndTime())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "不在游戏有效期内");
        }

        // 2. 校验次数
        String dayKey = LocalDate.now().toString();
        long todayCount = playRepository.countByTenantIdAndGameIdAndMemberIdAndDayKey(tenantId, gameId, memberId, dayKey);
        int dailyLimit = g.getDailyLimit() == null ? 1 : g.getDailyLimit();
        if (todayCount >= dailyLimit) {
            throw new BizException(ErrorCode.BIZ_ERROR, "今日次数已用完");
        }
        if (g.getTotalLimit() != null && g.getTotalLimit() > 0) {
            long totalCount = playRepository.countByTenantIdAndGameIdAndMemberId(tenantId, gameId, memberId);
            if (totalCount >= g.getTotalLimit()) {
                throw new BizException(ErrorCode.BIZ_ERROR, "总次数已用完");
            }
        }

        // 3. 扣减积分
        int pointsCost = g.getPointsCost() == null ? 0 : g.getPointsCost();
        if (pointsCost > 0) {
            MemberDto.PointsAdjustRequest costReq = new MemberDto.PointsAdjustRequest();
            costReq.setDelta(-(long) pointsCost);
            costReq.setReason("玩游戏消耗: " + g.getName());
            memberService.adjustPoints(memberId, costReq);
        }

        // 4. 按概率抽奖
        List<GamePrize> prizeList = prizeRepository.findByGameIdAndDeletedFalseOrderBySortOrderAsc(gameId);
        GamePrize wonPrize = drawPrize(prizeList);

        // 5. 发放奖品
        GameDto.PlayResult result = new GameDto.PlayResult();
        GamePlay play = new GamePlay();
        play.setTenantId(tenantId);
        play.setGameId(gameId);
        play.setMemberId(memberId);
        play.setPlayedAt(now);
        play.setDayKey(dayKey);

        if (wonPrize != null) {
            play.setPrizeId(wonPrize.getId());
            play.setPrizeName(wonPrize.getName());
            play.setPrizeType(wonPrize.getType());
            play.setIsWin(true);
            result.setIsWin(true);
            result.setPrizeName(wonPrize.getName());
            result.setPrizeType(wonPrize.getType());
            result.setPrizeImage(wonPrize.getImageUrl());
            // 实际发放
            grantPrize(tenantId, memberId, wonPrize);
        } else {
            play.setIsWin(false);
            result.setIsWin(false);
        }

        playRepository.save(play);
        return result;
    }

    /** 按概率抽奖: 随机数 0-999, 落在奖品区间则中奖 */
    private GamePrize drawPrize(List<GamePrize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            return null;
        }
        int rand = ThreadLocalRandom.current().nextInt(1000);
        int cumulative = 0;
        for (GamePrize p : prizes) {
            int prob = p.getProbability() == null ? 0 : p.getProbability();
            cumulative += prob;
            if (rand < cumulative) {
                return p;
            }
        }
        return null;
    }

    /** 发放奖品: COUPON 发优惠券, POINTS 加积分, EMPTY 不发放 */
    private void grantPrize(Long tenantId, Long memberId, GamePrize prize) {
        try {
            if ("COUPON".equals(prize.getType()) && prize.getRefId() != null) {
                CouponDto.GrantRequest grantReq = new CouponDto.GrantRequest();
                grantReq.setMemberIds(List.of(memberId));
                couponService.grant(prize.getRefId(), grantReq);
            } else if ("POINTS".equals(prize.getType())) {
                int amount = prize.getAmount() == null ? 0 : prize.getAmount();
                if (amount > 0) {
                    MemberDto.PointsAdjustRequest pointsReq = new MemberDto.PointsAdjustRequest();
                    pointsReq.setDelta((long) amount);
                    pointsReq.setReason("游戏中奖: " + prize.getName());
                    memberService.adjustPoints(memberId, pointsReq);
                }
            }
            // EMPTY 类型不发放
        } catch (Exception e) {
            // 发放失败不影响游戏记录, 但必须记录日志便于人工补发
            log.error("游戏奖品发放失败: tenantId={}, memberId={}, prizeId={}, prizeType={}",
                    tenantId, memberId, prize.getId(), prize.getType(), e);
        }
    }

    // ---- 记录与统计 ----

    /** 我的游戏记录 */
    public List<GamePlay> myPlays(Long tenantId, Long memberId, Long gameId) {
        return playRepository.findByTenantIdAndGameIdAndMemberIdOrderByPlayedAtDesc(tenantId, gameId, memberId);
    }

    /** 统计: 总参与人次、中奖人次、各奖品发放量 */
    public Map<String, Object> stats(Long tenantId, Long gameId) {
        gameRepository.findByIdAndTenantIdAndDeletedFalse(gameId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "游戏不存在"));
        long totalPlays = playRepository.countByTenantIdAndGameId(tenantId, gameId);
        long winPlays = playRepository.countWinByTenantIdAndGameId(tenantId, gameId);
        List<Object[]> prizeCounts = playRepository.countByPrize(tenantId, gameId);
        List<Map<String, Object>> prizeStats = new ArrayList<>();
        for (Object[] row : prizeCounts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("prizeId", row[0]);
            m.put("prizeName", row[1]);
            m.put("count", row[2]);
            prizeStats.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPlays", totalPlays);
        result.put("winPlays", winPlays);
        result.put("prizeStats", prizeStats);
        return result;
    }
}
