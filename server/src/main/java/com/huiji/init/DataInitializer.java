package com.huiji.init;

import com.huiji.entity.Campaign;
import com.huiji.entity.Coupon;
import com.huiji.entity.CouponRecord;
import com.huiji.entity.DiningTable;
import com.huiji.entity.Game;
import com.huiji.entity.GamePrize;
import com.huiji.entity.MallCategory;
import com.huiji.entity.Member;
import com.huiji.entity.MemberTag;
import com.huiji.entity.MenuCategory;
import com.huiji.entity.Product;
import com.huiji.entity.Store;
import com.huiji.entity.Tenant;
import com.huiji.entity.User;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.CampaignRepository;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.CouponRepository;
import com.huiji.repository.DiningTableRepository;
import com.huiji.repository.GamePrizeRepository;
import com.huiji.repository.GameRepository;
import com.huiji.repository.MallCategoryRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.MemberTagRepository;
import com.huiji.repository.MenuCategoryRepository;
import com.huiji.repository.ProductRepository;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.TenantRepository;
import com.huiji.repository.UserRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 启动时初始化演示数据: 默认租户(tenantId=1)、管理员 admin/123456、
 * 2 个门店、若干员工/会员/券/活动, 并生成跨 60 天的真实资金流水使看板数据有意义。
 * 仅在 huiji.init-data=true 时执行; 幂等(已存在租户则跳过)。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final MemberTagRepository memberTagRepository;
    private final WalletTransactionRepository walletRepository;
    private final CouponRepository couponRepository;
    private final CouponRecordRepository couponRecordRepository;
    private final CampaignRepository campaignRepository;
    private final DiningTableRepository diningTableRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MallCategoryRepository mallCategoryRepository;
    private final GameRepository gameRepository;
    private final GamePrizeRepository gamePrizeRepository;
    private final ProductRepository productRepository;
    private final SettingsService settingsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${huiji.init-data:false}")
    private boolean initData;

    @Bean
    public ApplicationRunner dataSeedRunner() {
        return args -> {
            if (!initData) {
                return;
            }
            seed();
        };
    }

    @Transactional
    protected void seed() {
        if (tenantRepository.count() > 0) {
            log.info("演示数据已存在, 跳过初始化");
            return;
        }
        log.info("开始初始化星河·会记演示数据...");

        // 1. 租户
        Tenant tenant = new Tenant();
        tenant.setName("星河·会记演示");
        tenant.setBrandColor("#4f46e5");
        tenant.setStatus("ACTIVE");
        tenantRepository.save(tenant);
        Long tid = tenant.getId();
        settingsService.getOrInit(tid, tenant.getName());

        // 2. 门店
        Store s1 = store("星河·会记 旗舰店", "上海市黄浦区南京东路 100 号", "021-63008888", "09:00-22:00", tid);
        Store s2 = store("星河·会记 静安店", "上海市静安区南京西路 200 号", "021-62886666", "10:00-21:30", tid);

        // 3. 员工
        user(tid, "admin", "123456", "超级管理员", "13800000000", "TENANT_ADMIN", List.of());
        user(tid, "wang mgr", "123456", "王店长", "13800000010", "STORE_MANAGER", List.of(s1.getId()));
        user(tid, "li staff", "123456", "李师傅", "13800000011", "STAFF", List.of(s1.getId()));
        user(tid, "zhao cash", "123456", "赵收银", "13800000012", "CASHIER", List.of(s2.getId()));

        // 4. 会员(注册时间分散在 60 天内, 便于增长曲线)
        String[][] memberSeed = {
                {"张伟", "13800000001", "MALE", "1990-03-15"},
                {"李娜", "13800000002", "FEMALE", "1992-07-22"},
                {"王强", "13800000003", "MALE", "1988-11-08"},
                {"刘洋", "13800000004", "FEMALE", "1995-01-30"},
                {"陈静", "13800000005", "FEMALE", "1991-09-12"},
                {"赵磊", "13800000006", "MALE", "1985-05-20"},
                {"孙丽", "13800000007", "FEMALE", "1993-12-05"},
                {"周杰", "13800000008", "MALE", "1989-04-18"},
                {"吴敏", "13800000009", "FEMALE", "1996-08-25"},
                {"郑涛", "13800000013", "MALE", "1987-02-14"},
        };
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < memberSeed.length; i++) {
            String[] d = memberSeed[i];
            Member m = new Member();
            m.setTenantId(tid);
            m.setName(d[0]);
            m.setPhone(d[1]);
            m.setGender(d[2]);
            m.setBirthday(LocalDate.parse(d[3]));
            m.setStoreIds(List.of((i % 2 == 0) ? s1.getId() : s2.getId()));
            m.setBalance(0L);
            m.setPoints(0L);
            m.setConsumeCount(0);
            m.setTotalAmount(0L);
            // 注册时间: 分散在过去 60 天
            m.setCreatedAt(LocalDateTime.now().minusDays(55 - i * 5));
            memberRepository.save(m);
            members.add(m);
        }
        // 标签
        tag(tid, members.get(0).getId(), "VIP");
        tag(tid, members.get(0).getId(), "老客");
        tag(tid, members.get(1).getId(), "VIP");
        tag(tid, members.get(2).getId(), "老客");
        tag(tid, members.get(4).getId(), "高频");
        tag(tid, members.get(6).getId(), "高频");

        // 5. 资金流水(充值 + 消费), 跨 60 天, 按服务项聚合便于看板
        String[] services = {"男士剪发", "女士烫染", "头皮护理", "造型设计", "染发", "洗发吹风"};
        long[] servicePrices = {6800, 38800, 12800, 9800, 26800, 3800};
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            Long storeId = (i % 2 == 0) ? s1.getId() : s2.getId();
            Long opId = (i % 2 == 0) ? 3L : 4L; // 李师傅/赵收银
            // 每人先充值 2-3 次(含赠送)
            int recharges = 2 + (i % 2);
            for (int r = 0; r < recharges; r++) {
                long amount = 100000L + r * 100000L; // 1000 / 2000 / 3000 元
                long gift = settingsService.matchGift(tid, amount);
                LocalDateTime when = now.minusDays(50 - r * 18).minusHours(i);
                long after = m.getBalance() + amount + gift;
                m.setBalance(after);
                applyRecharge(tid, m, amount, gift, when, opId, storeId);
            }
            // 每人多次消费, 时间分散在 60 天内、小时分散在营业时段
            int consumes = 8 + (i % 3);
            for (int c = 0; c < consumes; c++) {
                int svcIdx = (i + c) % services.length;
                long price = servicePrices[svcIdx];
                if (m.getBalance() < price) {
                    // 余额不足则补一次充值
                    long amount = 100000L;
                    long gift = settingsService.matchGift(tid, amount);
                    long after = m.getBalance() + amount + gift;
                    m.setBalance(after);
                    applyRecharge(tid, m, amount, gift, now.minusDays(40 - c * 4), opId, storeId);
                }
                // 时间: 从 45 天前到今天, 小时在 10-21 点分布
                int dayAgo = 45 - c * (45 / Math.max(1, consumes));
                int hour = 10 + ((i + c) % 11);
                LocalDateTime when = now.minusDays(Math.max(0, dayAgo)).withHour(hour).withMinute((i * 7 + c * 3) % 60).withSecond(0).withNano(0);
                long after = m.getBalance() - price;
                m.setBalance(after);
                m.setConsumeCount(m.getConsumeCount() + 1);
                m.setTotalAmount(m.getTotalAmount() + price);
                m.setLastConsumeAt(when);
                applyConsume(tid, m, price, when, opId, storeId, services[svcIdx]);
            }
            // 个别会员设为沉睡(>90 天无消费)
            if (i == 5 || i == 9) {
                m.setLastConsumeAt(now.minusDays(100 + i * 5));
            }
            // 等级自动升级
            var rule = settingsService.resolveLevel(tid, m.getTotalAmount());
            if (rule != null) {
                m.setLevel(rule.getLevel());
            }
            memberRepository.save(m);
        }

        // 6. 优惠券
        Coupon c1 = coupon(tid, "新人 50 元券", "FULL_CUT", 5000L, 0L, "DAYS", 30, null, null, 100, 1, "ALL");
        Coupon c2 = coupon(tid, "满 300 打 8.5 折", "PERCENT", 85L, 30000L, "DAYS", 30, null, null, 200, 1, "ALL");
        Coupon c3 = coupon(tid, "免费头皮护理体验", "EXPERIENCE", 12800L, 0L, "RANGE", null, LocalDate.now(), LocalDate.now().plusDays(60), 50, 1, "ALL");
        Coupon c4 = coupon(tid, "生日 100 元礼券", "BIRTHDAY", 10000L, 0L, "DAYS", 15, null, null, null, 1, "ALL");

        // 发放几张券给前几个会员
        grant(tid, c1, members.get(0), members.get(1));
        grant(tid, c2, members.get(2), members.get(4));
        // 把其中一张标记为已核销
        List<CouponRecord> recs = couponRecordRepository.findByCoupon(tid, c1.getId());
        if (!recs.isEmpty()) {
            CouponRecord r = recs.get(0);
            r.setStatus("USED");
            r.setUsedAt(now.minusDays(3));
            r.setUsedStoreId(s1.getId());
            couponRecordRepository.save(r);
            c1.setUsedCount(1);
            couponRepository.save(c1);
        }

        // 7. 营销活动
        campaign(tid, "生日自动关怀", "BIRTHDAY", "生日前 3 天", "level>=1", "WECHAT",
                "亲爱的会员, 生日快乐! 赠您 100 元礼券, 祝您美好的一天。", now.minusDays(20), now.plusDays(60), true, 12, 12, 5);
        campaign(tid, "沉睡会员唤醒", "DORMANT", "90 天未到店", "lastConsume<90d", "SMS",
                "好久不见! 回店即享 8.5 折优惠, 期待您的再次光临。", now.minusDays(10), now.plusDays(30), true, 8, 8, 2);
        campaign(tid, "复购激励", "REPURCHASE", "消费后 7 天", "consumeCount>=1", "IN_APP",
                "感谢您的光临, 再次消费可领专属优惠券。", now.minusDays(5), now.plusDays(90), false, 0, 0, 0);

        // 8. 桌台演示数据
        try {
            String[][] tableSeed = {
                    {"A1", "大厅", "4"}, {"A2", "大厅", "4"},
                    {"A3", "大厅", "4"}, {"A4", "大厅", "4"},
                    {"B1", "包间", "8"}, {"B2", "包间", "8"},
                    {"C1", "露台", "6"}, {"C2", "露台", "6"},
            };
            for (int i = 0; i < tableSeed.length; i++) {
                DiningTable t = new DiningTable();
                t.setTenantId(tid);
                t.setStoreId(s1.getId());
                t.setName(tableSeed[i][0]);
                t.setArea(tableSeed[i][1]);
                t.setSeats(Integer.parseInt(tableSeed[i][2]));
                t.setStatus("IDLE");
                t.setSortOrder(i + 1);
                diningTableRepository.save(t);
            }
        } catch (Exception e) {
            log.warn("桌台演示数据初始化失败: {}", e.getMessage());
        }

        // 9. 菜单分类演示数据
        try {
            String[] menuCatNames = {"招牌洗护", "头皮护理", "造型设计", "精选好物"};
            for (int i = 0; i < menuCatNames.length; i++) {
                MenuCategory mc = new MenuCategory();
                mc.setTenantId(tid);
                mc.setStoreId(s1.getId());
                mc.setName(menuCatNames[i]);
                mc.setSortOrder(i + 1);
                mc.setStatus("ENABLED");
                menuCategoryRepository.save(mc);
            }
        } catch (Exception e) {
            log.warn("菜单分类演示数据初始化失败: {}", e.getMessage());
        }

        // 10. 商城分类演示数据
        try {
            String[] mallCatNames = {"精选好物", "护理套装", "洗护用品"};
            for (int i = 0; i < mallCatNames.length; i++) {
                MallCategory mc = new MallCategory();
                mc.setTenantId(tid);
                mc.setName(mallCatNames[i]);
                mc.setSortOrder(i + 1);
                mc.setStatus("ENABLED");
                mallCategoryRepository.save(mc);
            }
        } catch (Exception e) {
            log.warn("商城分类演示数据初始化失败: {}", e.getMessage());
        }

        // 11. 将前 4 个商品设为商城可见, 关联到商城分类
        try {
            List<Product> products = productRepository.listActive(tid, null);
            List<MallCategory> mallCats = mallCategoryRepository.findByTenantIdOrderBySortOrderAsc(tid);
            if (!mallCats.isEmpty()) {
                int bound = Math.min(4, products.size());
                for (int i = 0; i < bound; i++) {
                    Product p = products.get(i);
                    p.setMallVisible(true);
                    // 4 个商品轮流分配到 3 个商城分类
                    p.setMallCategoryId(mallCats.get(i % mallCats.size()).getId());
                    productRepository.save(p);
                }
            }
        } catch (Exception e) {
            log.warn("商品更新为商城可见失败: {}", e.getMessage());
        }

        // 12. 游戏演示数据: 大转盘 + 砸金蛋
        try {
            LocalDateTime gameStart = LocalDateTime.now();
            LocalDateTime gameEnd = gameStart.plusDays(30);

            // 大转盘游戏
            Game wheel = new Game();
            wheel.setTenantId(tid);
            wheel.setStoreId(s1.getId());
            wheel.setName("周年庆幸运转盘");
            wheel.setType("WHEEL");
            wheel.setSubtitle("转一转，好礼转出来");
            wheel.setStartTime(gameStart);
            wheel.setEndTime(gameEnd);
            wheel.setDailyLimit(1);
            wheel.setTotalLimit(0);
            wheel.setPointsCost(0);
            wheel.setStatus("ENABLED");
            wheel.setRules("每人每天可抽奖1次，奖品包括优惠券和积分");
            gameRepository.save(wheel);

            // 大转盘 4 个奖品
            GamePrize gp1 = new GamePrize();
            gp1.setGameId(wheel.getId());
            gp1.setName("5元优惠券");
            gp1.setType("COUPON");
            gp1.setRefId(1L);
            gp1.setRefName("新人券");
            gp1.setProbability(100);
            gp1.setSortOrder(1);
            gamePrizeRepository.save(gp1);

            GamePrize gp2 = new GamePrize();
            gp2.setGameId(wheel.getId());
            gp2.setName("50积分");
            gp2.setType("POINTS");
            gp2.setAmount(50);
            gp2.setProbability(150);
            gp2.setSortOrder(2);
            gamePrizeRepository.save(gp2);

            GamePrize gp3 = new GamePrize();
            gp3.setGameId(wheel.getId());
            gp3.setName("100积分");
            gp3.setType("POINTS");
            gp3.setAmount(100);
            gp3.setProbability(50);
            gp3.setSortOrder(3);
            gamePrizeRepository.save(gp3);

            GamePrize gp4 = new GamePrize();
            gp4.setGameId(wheel.getId());
            gp4.setName("谢谢参与");
            gp4.setType("EMPTY");
            gp4.setProbability(700);
            gp4.setSortOrder(4);
            gamePrizeRepository.save(gp4);

            // 砸金蛋游戏
            Game egg = new Game();
            egg.setTenantId(tid);
            egg.setStoreId(s1.getId());
            egg.setName("金蛋好运");
            egg.setType("EGG");
            egg.setSubtitle("砸出你的好运来");
            egg.setStartTime(gameStart);
            egg.setEndTime(gameEnd);
            egg.setDailyLimit(1);
            egg.setTotalLimit(0);
            egg.setPointsCost(0);
            egg.setStatus("ENABLED");
            egg.setRules("每人每天可抽奖1次，奖品包括优惠券和积分");
            gameRepository.save(egg);
        } catch (Exception e) {
            log.warn("游戏演示数据初始化失败: {}", e.getMessage());
        }

        log.info("演示数据初始化完成: 租户={}, 会员={}, 流水={}",
                tid, members.size(), walletRepository.count());
    }

    // ---- 构造辅助 ----

    private Store store(String name, String addr, String phone, String hours, Long tid) {
        Store s = new Store();
        s.setTenantId(tid);
        s.setName(name);
        s.setAddress(addr);
        s.setPhone(phone);
        s.setBusinessHours(hours);
        s.setStatus("ACTIVE");
        return storeRepository.save(s);
    }

    private void user(Long tid, String username, String pwd, String name, String phone, String role, List<Long> storeIds) {
        User u = new User();
        u.setTenantId(tid);
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(pwd));
        u.setName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setStoreIds(storeIds);
        u.setStatus("ACTIVE");
        userRepository.save(u);
    }

    private void tag(Long tid, Long memberId, String tag) {
        MemberTag t = new MemberTag();
        t.setTenantId(tid);
        t.setMemberId(memberId);
        t.setTag(tag);
        memberTagRepository.save(t);
    }

    private void applyRecharge(Long tid, Member m, long amount, long gift, LocalDateTime when, Long opId, Long storeId) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tid);
        tx.setMemberId(m.getId());
        tx.setType("RECHARGE");
        tx.setAmount(amount);
        tx.setGift(gift);
        tx.setBalanceAfter(m.getBalance());
        tx.setPayMethod("WECHAT");
        tx.setOperatorId(opId);
        tx.setStoreId(storeId);
        tx.setCreatedAt(when);
        tx.setUpdatedAt(when);
        walletRepository.save(tx);
        if (gift > 0) {
            WalletTransaction g = new WalletTransaction();
            g.setTenantId(tid);
            g.setMemberId(m.getId());
            g.setType("GIFT");
            g.setAmount(gift);
            g.setBalanceAfter(m.getBalance());
            g.setRemark("充值赠送");
            g.setOperatorId(opId);
            g.setCreatedAt(when);
            g.setUpdatedAt(when);
            walletRepository.save(g);
        }
    }

    private void applyConsume(Long tid, Member m, long price, LocalDateTime when, Long opId, Long storeId, String service) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tid);
        tx.setMemberId(m.getId());
        tx.setType("CONSUME");
        tx.setAmount(-price);
        tx.setBalanceAfter(m.getBalance());
        tx.setStoreId(storeId);
        tx.setOperatorId(opId);
        tx.setOrderNo("OD" + when.toLocalDate() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        tx.setRemark(service);
        tx.setCreatedAt(when);
        tx.setUpdatedAt(when);
        walletRepository.save(tx);
    }

    private Coupon coupon(Long tid, String name, String type, Long faceValue, Long threshold,
                          String validType, Integer validDays, LocalDate start, LocalDate end,
                          Integer total, int perLimit, String scope) {
        Coupon c = new Coupon();
        c.setTenantId(tid);
        c.setName(name);
        c.setType(type);
        c.setFaceValue(faceValue);
        c.setThreshold(threshold == null ? 0L : threshold);
        c.setValidType(validType);
        c.setValidDays(validDays);
        c.setValidStart(start);
        c.setValidEnd(end);
        c.setTotal(total);
        c.setGrantedCount(0);
        c.setUsedCount(0);
        c.setPerLimit(perLimit);
        c.setScope(scope);
        c.setStatus("ACTIVE");
        return couponRepository.save(c);
    }

    private void grant(Long tid, Coupon c, Member... ms) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expire = "DAYS".equals(c.getValidType()) ? now.plusDays(c.getValidDays()) : now.plusDays(30);
        for (Member m : ms) {
            CouponRecord r = new CouponRecord();
            r.setTenantId(tid);
            r.setCouponId(c.getId());
            r.setCouponName(c.getName());
            r.setMemberId(m.getId());
            r.setMemberName(m.getName());
            r.setCode("CP" + System.currentTimeMillis() % 1000000 + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
            r.setStatus("UNUSED");
            r.setGrantedAt(now);
            r.setExpireAt(expire);
            couponRecordRepository.save(r);
            c.setGrantedCount((c.getGrantedCount() == null ? 0 : c.getGrantedCount()) + 1);
        }
        couponRepository.save(c);
    }

    private Campaign campaign(Long tid, String name, String type, String trigger, String audience,
                              String channel, String content, LocalDateTime startAt, LocalDateTime endAt,
                              boolean enabled, int triggered, int reached, int converted) {
        Campaign c = new Campaign();
        c.setTenantId(tid);
        c.setName(name);
        c.setType(type);
        c.setTrigger(trigger);
        c.setAudience(audience);
        c.setChannel(channel);
        c.setContent(content);
        c.setStartAt(startAt);
        c.setEndAt(endAt);
        c.setEnabled(enabled);
        c.setStatTriggered(triggered);
        c.setStatReached(reached);
        c.setStatConverted(converted);
        return campaignRepository.save(c);
    }
}
