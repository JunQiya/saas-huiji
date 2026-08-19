package com.huiji.init;

import com.huiji.entity.Agent;
import com.huiji.entity.Campaign;
import com.huiji.entity.CampaignLog;
import com.huiji.entity.Coupon;
import com.huiji.entity.CouponRecord;
import com.huiji.entity.DiningTable;
import com.huiji.entity.Game;
import com.huiji.entity.GamePlay;
import com.huiji.entity.GamePrize;
import com.huiji.entity.KitchenOrder;
import com.huiji.entity.MallCategory;
import com.huiji.entity.Member;
import com.huiji.entity.MemberTag;
import com.huiji.entity.MenuCategory;
import com.huiji.entity.MessageTask;
import com.huiji.entity.Order;
import com.huiji.entity.OrderItem;
import com.huiji.entity.Product;
import com.huiji.entity.Referral;
import com.huiji.entity.ReportTask;
import com.huiji.entity.Store;
import com.huiji.entity.Tenant;
import com.huiji.entity.TenantSetting;
import com.huiji.entity.User;
import com.huiji.entity.WalletTransaction;
import com.huiji.entity.WxAccount;
import com.huiji.repository.AgentRepository;
import com.huiji.repository.CampaignLogRepository;
import com.huiji.repository.CampaignRepository;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.CouponRepository;
import com.huiji.repository.DiningTableRepository;
import com.huiji.repository.GamePlayRepository;
import com.huiji.repository.GamePrizeRepository;
import com.huiji.repository.GameRepository;
import com.huiji.repository.KitchenOrderRepository;
import com.huiji.repository.MallCategoryRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.MemberTagRepository;
import com.huiji.repository.MenuCategoryRepository;
import com.huiji.repository.MessageTaskRepository;
import com.huiji.repository.OrderItemRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.ProductRepository;
import com.huiji.repository.ReferralRepository;
import com.huiji.repository.ReportTaskRepository;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.TenantRepository;
import com.huiji.repository.TenantSettingRepository;
import com.huiji.repository.UserRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.repository.WxAccountRepository;
import com.huiji.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final CampaignLogRepository campaignLogRepository;
    private final DiningTableRepository diningTableRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MallCategoryRepository mallCategoryRepository;
    private final GameRepository gameRepository;
    private final GamePrizeRepository gamePrizeRepository;
    private final GamePlayRepository gamePlayRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final AgentRepository agentRepository;
    private final WxAccountRepository wxAccountRepository;
    private final ReportTaskRepository reportTaskRepository;
    private final MessageTaskRepository messageTaskRepository;
    private final ReferralRepository referralRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final SettingsService settingsService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final javax.sql.DataSource dataSource;

    @Value("${huiji.init-data:false}")
    private boolean initData;

    @Bean
    public ApplicationRunner versionFixRunner() {
        return args -> {
            // 历史数据迁移：仅当表已存在且某行 version 为 null 时补 0。
            // H2(测试) 下表由 ddl-auto 全新创建且带引号小写, 无引号 SQL 大小写不匹配, 直接跳过;
            // MySQL 生产场景(表名/列名均小写) 才会真正执行修复。
            if (isH2()) {
                log.info("H2 环境跳过 version 字段历史修复(全新建表无此问题)");
                return;
            }
            String[] tables = {"tenant", "tenant_setting", "app_user", "member", "member_tag", "store",
                    "product", "coupon", "coupon_record", "campaign", "campaign_log",
                    "sales_order", "sales_order_item", "wallet_transaction", "dining_table",
                    "menu_category", "kitchen_order", "mall_category", "cart", "order_extend",
                    "game", "game_prize", "game_play", "wx_account", "agent", "referral",
                    "message_task", "login_log", "audit_log", "report_task"};
            transactionTemplate.execute(status -> {
                int total = 0;
                for (String table : tables) {
                    try {
                        int n = entityManager.createNativeQuery(
                                "UPDATE " + table + " SET version = 0 WHERE version IS NULL")
                                .executeUpdate();
                        total += n;
                    } catch (Exception ignored) { /* 表可能不存在或列已非空 */ }
                }
                log.info("version 字段修复完成, 共更新 {} 条记录", total);
                return null;
            });
        };
    }

    @Bean
    public ApplicationRunner dataSeedRunner() {
        return args -> {
            if (!initData) {
                return;
            }
            seed();
        };
    }

    private boolean isH2() {
        // try-with-resources 确保连接归还连接池, 避免 Hikari 连接泄漏告警
        try (var con = dataSource.getConnection()) {
            String url = con.getMetaData().getURL();
            return url != null && url.toLowerCase().contains("h2");
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    protected void seed() {
        // 整个种子数据放在一个事务内执行:
        // 1) 保证 @Version + IDENTITY 的 id 回填与 refresh 可用(需要活动事务)
        // 2) 中途失败整体回滚, 避免产生半截数据
        transactionTemplate.executeWithoutResult(status -> doSeed());
    }

    private void doSeed() {
        if (tenantRepository.count() > 0) {
            // 演示租户已存在: 不重建(避免影响正常操作), 仅补全缺失的演示数据模块
            log.info("演示数据已存在, 检查并补全缺失模块...");
            fillMissingModules();
            return;
        }
        log.info("开始初始化星河·会记演示数据...");

        // 1. 租户
        Tenant tenant = new Tenant();
        tenant.setName("星河·会记演示");
        tenant.setBrandColor("#4f46e5");
        tenant.setStatus("ACTIVE");
        entityManager.persist(tenant);
        entityManager.flush();
        Long tid = tenant.getId();
        TenantSetting ts = settingsService.getOrInit(tid, tenant.getName());
        // 演示租户初始短信余额(分), 便于消息中心/群发功能直接演示
        if (ts.getSmsBalance() == null || ts.getSmsBalance() <= 0) {
            ts.setSmsBalance(100_000);
            tenantSettingRepository.save(ts);
        }

        // 2. 门店
        Store s1 = store("星河·会记 旗舰店", "上海市黄浦区南京东路 100 号", "021-63008888", "09:00-22:00", tid);
        Store s2 = store("星河·会记 静安店", "上海市静安区南京西路 200 号", "021-62886666", "10:00-21:30", tid);
        s1.setLatitude(31.2397);
        s1.setLongitude(121.4757);
        s2.setLatitude(31.2286);
        s2.setLongitude(121.4581);

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
            m.setConsumeCount(0);
            m.setTotalAmount(0L);
            // 注册时间: 分散在过去 60 天
            m.setCreatedAt(LocalDateTime.now().minusDays(55 - i * 5));
            // 用 EntityManager 显式 persist + flush, 确保 H2 下 @Version+IDENTITY 正确回填主键
            entityManager.persist(m);
            entityManager.flush();
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
                long earnedPoints = price / 100; // 1元 = 1积分
                m.setPoints(m.getPoints() + earnedPoints);
                m.setLastConsumeAt(when);
                applyConsume(tid, m, price, when, opId, storeId, services[svcIdx]);
            }
            // 个别会员设为沉睡(>90 天无消费)
            if (i == 5 || i == 9) {
                m.setLastConsumeAt(now.minusDays(100 + i * 5));
            }            // 等级自动升级
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
                entityManager.persist(mc);
                entityManager.flush();
            }
        } catch (Exception e) {
            log.warn("商城分类演示数据初始化失败: {}", e.getMessage());
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
            entityManager.persist(wheel);
            entityManager.flush();

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
            egg.setRules("每人每天可砸1个金蛋，奖品包括优惠券和积分");
            entityManager.persist(egg);
            entityManager.flush();

            // 金蛋 3 个奖品
            GamePrize ep1 = new GamePrize();
            ep1.setGameId(egg.getId());
            ep1.setName("10元优惠券");
            ep1.setType("COUPON");
            ep1.setRefId(1L);
            ep1.setRefName("新人券");
            ep1.setProbability(150);
            ep1.setSortOrder(1);
            gamePrizeRepository.save(ep1);

            GamePrize ep2 = new GamePrize();
            ep2.setGameId(egg.getId());
            ep2.setName("80积分");
            ep2.setType("POINTS");
            ep2.setAmount(80);
            ep2.setProbability(300);
            ep2.setSortOrder(2);
            gamePrizeRepository.save(ep2);

            GamePrize ep3 = new GamePrize();
            ep3.setGameId(egg.getId());
            ep3.setName("谢谢参与");
            ep3.setType("EMPTY");
            ep3.setProbability(550);
            ep3.setSortOrder(3);
            gamePrizeRepository.save(ep3);
        } catch (Exception e) {
            log.warn("游戏演示数据初始化失败: {}", e.getMessage());
        }

        // 13. 商品演示数据: 6 个服务 + 4 个商品(部分商品有库存)
        try {
            Object[][] serviceProducts = {
                    {"男士剪发", 6800L, 1500L, null, 0},
                    {"女士烫染", 38800L, 12000L, null, 0},
                    {"头皮护理", 12800L, 3000L, null, 0},
                    {"造型设计", 9800L, 2500L, null, 0},
                    {"染发", 26800L, 8000L, null, 0},
                    {"洗发吹风", 3800L, 500L, null, 0},
            };
            for (Object[] row : serviceProducts) {
                Product p = new Product();
                p.setTenantId(tid);
                p.setName((String) row[0]);
                p.setCategory("SERVICE");
                p.setPrice((Long) row[1]);
                p.setCostPrice((Long) row[2]);
                p.setStatus("ACTIVE");
                p.setStoreIds(List.of(s1.getId()));
                p.setSoldCount((Integer) row[4]);
                productRepository.save(p);
            }
            Object[][] goodsProducts = {
                    {"护理套装", 29800L, 12000L, 50, 12},
                    {"洗发水 500ml", 8800L, 3500L, 100, 28},
                    {"护发素 500ml", 8800L, 3500L, 80, 18},
                    {"造型喷雾", 6800L, 2500L, 60, 8},
            };
            for (Object[] row : goodsProducts) {
                Product p = new Product();
                p.setTenantId(tid);
                p.setName((String) row[0]);
                p.setCategory("GOODS");
                p.setPrice((Long) row[1]);
                p.setCostPrice((Long) row[2]);
                p.setStock((Integer) row[3]);
                p.setStatus("ACTIVE");
                p.setStoreIds(List.of(s1.getId()));
                p.setSoldCount((Integer) row[4]);
                productRepository.save(p);
            }

            // 将前 4 个商品设为商城可见, 关联到商城分类(必须在商品创建之后)
            List<MallCategory> mallCats = mallCategoryRepository.findByTenantIdOrderBySortOrderAsc(tid);
            List<Product> products = productRepository.listActive(tid, null);
            if (!mallCats.isEmpty() && !products.isEmpty()) {
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
            log.warn("商品演示数据初始化失败: {}", e.getMessage());
        }

        // 13.5 商品关联菜单分类(点餐菜单依赖), 幂等
        ensureMenuLinks(tid, s1);

        // 14. Agent 代理演示数据
        try {
            Agent a1 = new Agent();
            a1.setTenantId(tid);
            a1.setName("张代理");
            a1.setContactName("张代理");
            a1.setContactPhone("13900000001");
            a1.setCommissionRate(150);
            a1.setStatus("ACTIVE");
            agentRepository.save(a1);

            Agent a2 = new Agent();
            a2.setTenantId(tid);
            a2.setName("李代理");
            a2.setContactName("李代理");
            a2.setContactPhone("13900000002");
            a2.setCommissionRate(80);
            a2.setStatus("ACTIVE");
            agentRepository.save(a2);
        } catch (Exception e) {
            log.warn("代理演示数据初始化失败: {}", e.getMessage());
        }

        // 15. 微信公众号配置演示数据
        try {
            WxAccount wx = new WxAccount();
            wx.setTenantId(tid);
            wx.setAppId("wx1234567890abcdef");
            wx.setAppSecret("***");
            wx.setMchId("1234567890");
            wx.setMchKey("***");
            wx.setTemplateIds("{}");
            wx.setDomain("https://h5.lxxno.cn");
            wx.setStatus("ACTIVE");
            wxAccountRepository.save(wx);
        } catch (Exception e) {
            log.warn("微信公众号配置演示数据初始化失败: {}", e.getMessage());
        }

        // 16. 报表任务演示数据
        try {
            ReportTask r1 = new ReportTask();
            r1.setTenantId(tid);
            r1.setName("每日营业报表");
            r1.setType("REVENUE");
            r1.setSchedule("DAILY");
            r1.setRecipients("admin@example.com");
            r1.setEnabled(true);
            reportTaskRepository.save(r1);

            ReportTask r2 = new ReportTask();
            r2.setTenantId(tid);
            r2.setName("每周会员报表");
            r2.setType("MEMBER");
            r2.setSchedule("WEEKLY");
            r2.setRecipients("admin@example.com");
            r2.setEnabled(true);
            reportTaskRepository.save(r2);
        } catch (Exception e) {
            log.warn("报表任务演示数据初始化失败: {}", e.getMessage());
        }

        // 17. 订单演示数据: 前 5 个会员每人 3 笔已支付订单(余额支付, 跨过去 30 天)
        try {
            List<Product> serviceList = new ArrayList<>();
            for (Product p : productRepository.findAll()) {
                if ("SERVICE".equals(p.getCategory()) && "ACTIVE".equals(p.getStatus())) {
                    serviceList.add(p);
                }
            }
            if (serviceList.size() >= 3) {
                int memberCount = Math.min(5, members.size());
                for (int mi = 0; mi < memberCount; mi++) {
                    Member m = members.get(mi);
                    // 余额不足则补充值
                    if (m.getBalance() < 50000L) {
                        m.setBalance(m.getBalance() + 200000L);
                        memberRepository.save(m);
                    }
                    for (int oi = 0; oi < 3; oi++) {
                        int svcCount = 1 + (oi % 3); // 1-3 个服务
                        long total = 0L;
                        List<OrderItem> oitems = new ArrayList<>();
                        for (int si = 0; si < svcCount; si++) {
                            Product p = serviceList.get((mi + oi + si) % serviceList.size());
                            long subtotal = p.getPrice();
                            total += subtotal;
                            OrderItem item = new OrderItem();
                            item.setTenantId(tid);
                            item.setProductId(p.getId());
                            item.setProductName(p.getName());
                            item.setUnitPrice(p.getPrice());
                            item.setQuantity(1);
                            item.setSubtotal(subtotal);
                            oitems.add(item);
                        }
                        LocalDateTime paidAt = now.minusDays((mi * 6) + (oi * 8))
                                .withHour(10 + ((mi + oi) % 10))
                                .withMinute(((mi * 7 + oi * 13) % 60))
                                .withSecond(0).withNano(0);
                        if (m.getBalance() < total) {
                            m.setBalance(m.getBalance() + total + 50000L);
                            memberRepository.save(m);
                        }
                        long afterBalance = m.getBalance() - total;
                        m.setBalance(afterBalance);
                        m.setConsumeCount((m.getConsumeCount() == null ? 0 : m.getConsumeCount()) + 1);
                        m.setTotalAmount((m.getTotalAmount() == null ? 0L : m.getTotalAmount()) + total);
                        m.setLastConsumeAt(paidAt);
                        m.setPoints(m.getPoints() + total / 100L);
                        memberRepository.save(m);

                        Order order = new Order();
                        order.setTenantId(tid);
                        order.setOrderNo(genOrderNo());
                        order.setStoreId((mi % 2 == 0) ? s1.getId() : s2.getId());
                        order.setMemberId(m.getId());
                        order.setCashierId((mi % 2 == 0) ? 3L : 4L);
                        order.setTotalAmount(total);
                        order.setDiscountAmount(0L);
                        order.setPaidAmount(total);
                        order.setPayMethod("BALANCE");
                        order.setStatus("PAID");
                        order.setPaidAt(paidAt);
                        order.setCreatedAt(paidAt);
                        order.setUpdatedAt(paidAt);
                        entityManager.persist(order);
                        entityManager.flush();

                        for (OrderItem it : oitems) {
                            it.setOrderId(order.getId());
                            it.setCreatedAt(paidAt);
                            it.setUpdatedAt(paidAt);
                        }
                        orderItemRepository.saveAll(oitems);

                        // 消费流水
                        WalletTransaction tx = new WalletTransaction();
                        tx.setTenantId(tid);
                        tx.setMemberId(m.getId());
                        tx.setType("CONSUME");
                        tx.setAmount(-total);
                        tx.setBalanceAfter(afterBalance);
                        tx.setStoreId(order.getStoreId());
                        tx.setOperatorId(order.getCashierId());
                        tx.setOrderNo(order.getOrderNo());
                        tx.setPayMethod("BALANCE");
                        tx.setRemark("订单消费");
                        tx.setCreatedAt(paidAt);
                        tx.setUpdatedAt(paidAt);
                        walletRepository.save(tx);

                        // 积分赠送流水
                        long pts = total / 100L;
                        if (pts > 0) {
                            WalletTransaction ptx = new WalletTransaction();
                            ptx.setTenantId(tid);
                            ptx.setMemberId(m.getId());
                            ptx.setType("POINT");
                            ptx.setAmount(pts);
                            ptx.setBalanceAfter(m.getPoints());
                            ptx.setStoreId(order.getStoreId());
                            ptx.setOrderNo(order.getOrderNo());
                            ptx.setRemark("消费赠送积分");
                            ptx.setCreatedAt(paidAt);
                            ptx.setUpdatedAt(paidAt);
                            walletRepository.save(ptx);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("订单演示数据初始化失败: {}", e.getMessage());
        }

        // 18. 厨房工单演示数据: 7 天内的 3-5 个订单生成厨房工单(SERVED)
        try {
            List<Order> recentOrders = new ArrayList<>();
            for (Order o : orderRepository.findAll()) {
                if ("PAID".equals(o.getStatus()) && o.getCreatedAt() != null
                        && o.getCreatedAt().isAfter(now.minusDays(7))) {
                    recentOrders.add(o);
                }
            }
            int kCount = Math.min(5, recentOrders.size());
            int kTake = Math.min(5, Math.max(3, kCount));
            int processed = 0;
            for (Order o : recentOrders) {
                if (processed >= kTake) break;
                List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(o.getId());
                if (items.isEmpty()) continue;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < items.size(); i++) {
                    OrderItem it = items.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"productId\":").append(it.getProductId())
                            .append(",\"name\":\"").append(it.getProductName()).append("\"")
                            .append(",\"quantity\":").append(it.getQuantity())
                            .append(",\"remark\":\"\"}");
                }
                sb.append("]");
                KitchenOrder ko = new KitchenOrder();
                ko.setTenantId(tid);
                ko.setStoreId(o.getStoreId());
                ko.setOrderId(o.getId());
                ko.setOrderType("DINE_IN");
                ko.setStatus("SERVED");
                ko.setItems(sb.toString());
                ko.setServedAt(o.getCreatedAt() == null ? now : o.getCreatedAt());
                ko.setCreatedAt(o.getCreatedAt() == null ? now : o.getCreatedAt());
                ko.setUpdatedAt(ko.getServedAt());
                kitchenOrderRepository.save(ko);
                processed++;
            }
        } catch (Exception e) {
            log.warn("厨房工单演示数据初始化失败: {}", e.getMessage());
        }

        // 19. 游戏参与记录: 前 3 个会员各 2 条游戏参与记录(过去 14 天内)
        try {
            Game wheel = null;
            for (Game g : gameRepository.findAll()) {
                if ("WHEEL".equals(g.getType())) { wheel = g; break; }
            }
            if (wheel != null) {
                int playMemberCount = Math.min(3, members.size());
                for (int mi = 0; mi < playMemberCount; mi++) {
                    Member m = members.get(mi);
                    for (int pi = 0; pi < 2; pi++) {
                        LocalDateTime playedAt = now.minusDays((mi * 5) + (pi * 3) + 1)
                                .withHour(14 + ((mi + pi) % 6))
                                .withMinute(((mi * 11 + pi * 7) % 60))
                                .withSecond(0).withNano(0);
                        int pick = (mi + pi) % 4;
                        String prizeName;
                        String prizeType;
                        boolean isWin;
                        Long prizeId = null;
                        if (pick == 0) {
                            prizeName = "5元优惠券";
                            prizeType = "COUPON";
                            isWin = true;
                            prizeId = 1L;
                        } else if (pick == 1) {
                            prizeName = "50积分";
                            prizeType = "POINTS";
                            isWin = true;
                            prizeId = 2L;
                        } else if (pick == 2) {
                            prizeName = "100积分";
                            prizeType = "POINTS";
                            isWin = true;
                            prizeId = 3L;
                        } else {
                            prizeName = "谢谢参与";
                            prizeType = "EMPTY";
                            isWin = false;
                            prizeId = 4L;
                        }
                        GamePlay play = new GamePlay();
                        play.setTenantId(tid);
                        play.setGameId(wheel.getId());
                        play.setMemberId(m.getId());
                        play.setPrizeId(prizeId);
                        play.setPrizeName(prizeName);
                        play.setPrizeType(prizeType);
                        play.setIsWin(isWin);
                        play.setPlayedAt(playedAt);
                        play.setDayKey(playedAt.toLocalDate().toString());
                        play.setCreatedAt(playedAt);
                        play.setUpdatedAt(playedAt);
                        gamePlayRepository.save(play);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("游戏参与记录初始化失败: {}", e.getMessage());
        }

        // 20. 营销活动触发日志(每个已启用活动造 3 条)
        try {
            for (Campaign c : campaignRepository.findAll()) {
                if (Boolean.TRUE.equals(c.getEnabled())) {
                    for (int i = 0; i < Math.min(3, members.size()); i++) {
                        Member m = members.get(i);
                        CampaignLog cl = new CampaignLog();
                        cl.setTenantId(tid);
                        cl.setCampaignId(c.getId());
                        cl.setCampaignName(c.getName());
                        cl.setMemberId(m.getId());
                        cl.setMemberName(m.getName());
                        cl.setAction(i == 0 ? "TRIGGERED" : (i == 1 ? "REACHED" : "CONVERTED"));
                        cl.setDetail("自动化演示触发: " + c.getName());
                        cl.setCreatedAt(now.minusDays((i + 1) * 2L));
                        cl.setUpdatedAt(cl.getCreatedAt());
                        campaignLogRepository.save(cl);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("营销活动触发日志初始化失败: {}", e.getMessage());
        }

        log.info("演示数据初始化完成: 租户={}, 会员={}, 流水={}",
                tid, members.size(), walletRepository.count());
    }

    /**
     * 演示数据补全: 逐模块检查, 为空则补齐, 不覆盖已有数据、不影响正常操作。
     * 保证演示系统"任何时候打开都有数据"(如消息中心/推荐裂变/核心业务模块被清空后可恢复)。
     */
    private void fillMissingModules() {
        try {
            Tenant tenant = tenantRepository.findAll().stream().findFirst().orElse(null);
            if (tenant == null) return;
            Long tid = tenant.getId();
            List<Member> members = memberRepository.findByTenantIdAndDeletedFalse(tid);
            List<Store> stores = storeRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tid);
            Store s1 = stores.isEmpty() ? null : stores.get(0);
            if (members.isEmpty() || s1 == null) {
                log.warn("演示租户缺少基础数据(会员/门店), 跳过补全");
                return;
            }
            ensureDemoMessages(tid, members);
            ensureDemoReferrals(tid, members);
            ensureDemoCoupons(tid, s1, members);
            ensureDemoCampaigns(tid);
            ensureDemoTables(tid, s1);
            ensureDemoMenus(tid, s1);
            ensureDemoGames(tid, s1);
            ensureDemoProducts(tid, s1);
            ensureDemoMallCategories(tid);
            ensureDemoAgents(tid);
            ensureMenuLinks(tid, s1);
        } catch (Exception e) {
            log.warn("演示数据补全异常: {}", e.getMessage());
        }
    }

    /**
     * 商品关联菜单分类(点餐菜单依赖): 幂等, 仅处理 menuCategoryId 为空的商品。
     * 首次 seed 与每次启动补全都会执行, 保证点餐页面有菜可点。
     */
    private void ensureMenuLinks(Long tid, Store s1) {
        try {
            // 演示菜单分类可能挂在任一门店, 用租户级查询, 不依赖具体门店
            List<MenuCategory> cats = menuCategoryRepository
                    .findByTenantIdAndDeletedFalseOrderBySortOrderAscIdAsc(tid);
            if (cats.isEmpty()) return;
            List<Product> unlinked = productRepository.findByTenantIdAndMenuCategoryIdNullAndDeletedFalseOrderByIdAsc(tid);
            int n = 0;
            for (int i = 0; i < unlinked.size(); i++) {
                Product p = unlinked.get(i);
                p.setMenuCategoryId(cats.get(i % cats.size()).getId());
                productRepository.save(p);
                n++;
            }
            if (n > 0) log.info("演示数据补全: 商品关联菜单分类 {} 个", n);
        } catch (Exception e) {
            log.warn("商品关联菜单分类失败: {}", e.getMessage());
        }
    }

    // ---- 模块级补全 ----

    /** 消息中心: 至少 3 条不同状态/渠道的演示消息 */
    private void ensureDemoMessages(Long tid, List<Member> members) {
        try {
            if (messageTaskRepository.count() > 0) return;
            String[] ids = members.subList(0, Math.min(3, members.size()))
                    .stream().map(m -> "\"" + m.getId() + "\"").toArray(String[]::new);
            String json = "[" + String.join(",", ids) + "]";
            LocalDateTime now = LocalDateTime.now();

            MessageTask t1 = new MessageTask();
            t1.setTenantId(tid);
            t1.setChannel("IN_APP");
            t1.setTemplateType("CAMPAIGN");
            t1.setSubject("三月焕新季 · 会员专属");
            t1.setContent("尊敬的会员, 星河·会记三月焕新季开启, 洗护套餐 8 折起, 期待您的光临!");
            t1.setMemberIds(json);
            t1.setTotalCount(ids.length);
            t1.setSentCount(ids.length);
            t1.setStatus("COMPLETED");
            t1.setCost(0L);
            t1.setScheduledAt(now.minusDays(3));
            messageTaskRepository.save(t1);

            MessageTask t2 = new MessageTask();
            t2.setTenantId(tid);
            t2.setChannel("SMS");
            t2.setTemplateType("COUPON_EXPIRE");
            t2.setSubject("优惠券即将到期提醒");
            t2.setContent("您有一张优惠券将在 3 天后到期, 欢迎到店使用。【星河会记】");
            t2.setMemberIds(json);
            t2.setTotalCount(ids.length);
            t2.setSentCount(0);
            t2.setFailedCount(1);
            t2.setCost(ids.length * 50L);
            t2.setStatus("PENDING");
            t2.setScheduledAt(now.plusMinutes(30));
            messageTaskRepository.save(t2);

            MessageTask t3 = new MessageTask();
            t3.setTenantId(tid);
            t3.setChannel("WECHAT");
            t3.setTemplateType("BIRTHDAY");
            t3.setSubject("生日关怀");
            t3.setContent("亲爱的会员, 生日快乐! 赠您一张 100 元礼券, 愿美好常伴左右。");
            t3.setMemberIds("[" + ids[0] + "]");
            t3.setTotalCount(1);
            t3.setSentCount(1);
            t3.setStatus("COMPLETED");
            t3.setCost(0L);
            t3.setScheduledAt(now.minusDays(1));
            messageTaskRepository.save(t3);

            log.info("演示数据补全: 消息中心 {} 条", 3);
        } catch (Exception e) {
            log.warn("消息中心演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 推荐裂变: 前 3 个会员作为推荐人, 各邀请后续会员 */
    private void ensureDemoReferrals(Long tid, List<Member> members) {
        try {
            if (referralRepository.countByTenantIdAndDeletedFalse(tid) > 0) return;
            String[] codes = {"XH001", "XH002", "XH003"};
            for (int i = 0; i < 3 && i < members.size(); i++) {
                Member referrer = members.get(i);
                for (int j = 1; j <= 2; j++) {
                    int idx = 3 + i * 2 + j - 1;
                    if (idx >= members.size()) break;
                    Member referee = members.get(idx);
                    Referral r = new Referral();
                    r.setTenantId(tid);
                    r.setReferrerId(referrer.getId());
                    r.setRefereeId(referee.getId());
                    r.setRefereeName(referee.getName());
                    r.setRefereePhone(referee.getPhone());
                    r.setCode(codes[i]);
                    r.setStatus("REGISTERED");
                    r.setRewardAmount(0L);
                    referralRepository.save(r);
                }
            }
            log.info("演示数据补全: 推荐裂变关系 {} 条", referralRepository.countByTenantIdAndDeletedFalse(tid));
        } catch (Exception e) {
            log.warn("推荐裂变演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 优惠券: 空则补 4 张并发放 */
    private void ensureDemoCoupons(Long tid, Store s1, List<Member> members) {
        try {
            if (couponRepository.countByTenantIdAndDeletedFalse(tid) > 0) return;
            LocalDate now = LocalDate.now();
            Coupon c1 = coupon(tid, "新人 50 元券", "FULL_CUT", 5000L, 0L, "DAYS", 30, null, null, 100, 1, "ALL");
            Coupon c2 = coupon(tid, "满 300 打 8.5 折", "PERCENT", 85L, 30000L, "DAYS", 30, null, null, 200, 1, "ALL");
            Coupon c3 = coupon(tid, "免费头皮护理体验", "EXPERIENCE", 12800L, 0L, "RANGE", null, now, now.plusDays(60), 50, 1, "ALL");
            coupon(tid, "生日 100 元礼券", "BIRTHDAY", 10000L, 0L, "DAYS", 15, null, null, null, 1, "ALL");
            grant(tid, c1, members.get(0), members.get(1));
            grant(tid, c2, members.get(2), members.get(4));
            List<CouponRecord> recs = couponRecordRepository.findByCoupon(tid, c1.getId());
            if (!recs.isEmpty()) {
                CouponRecord r = recs.get(0);
                r.setStatus("USED");
                r.setUsedAt(LocalDateTime.now().minusDays(3));
                r.setUsedStoreId(s1.getId());
                couponRecordRepository.save(r);
                c1.setUsedCount(1);
                couponRepository.save(c1);
            }
            log.info("演示数据补全: 优惠券");
        } catch (Exception e) {
            log.warn("优惠券演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 营销活动: 空则补 3 个 */
    private void ensureDemoCampaigns(Long tid) {
        try {
            if (campaignRepository.countByTenantIdAndDeletedFalse(tid) > 0) return;
            LocalDateTime now = LocalDateTime.now();
            campaign(tid, "生日自动关怀", "BIRTHDAY", "生日前 3 天", "level>=1", "WECHAT",
                    "亲爱的会员, 生日快乐! 赠您 100 元礼券, 祝您美好的一天。", now.minusDays(20), now.plusDays(60), true, 12, 12, 5);
            campaign(tid, "沉睡会员唤醒", "DORMANT", "90 天未到店", "lastConsume<90d", "SMS",
                    "好久不见! 回店即享 8.5 折优惠, 期待您的再次光临。", now.minusDays(10), now.plusDays(30), true, 8, 8, 2);
            campaign(tid, "复购激励", "REPURCHASE", "消费后 7 天", "consumeCount>=1", "IN_APP",
                    "感谢您的光临, 再次消费可领专属优惠券。", now.minusDays(5), now.plusDays(90), false, 0, 0, 0);
            log.info("演示数据补全: 营销活动");
        } catch (Exception e) {
            log.warn("营销活动演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 桌台: 空则补 8 张 */
    private void ensureDemoTables(Long tid, Store s1) {
        try {
            if (diningTableRepository.count() > 0) return;
            String[][] tableSeed = {
                    {"A1", "大厅", "4"}, {"A2", "大厅", "4"}, {"A3", "大厅", "4"}, {"A4", "大厅", "4"},
                    {"B1", "包间", "8"}, {"B2", "包间", "8"}, {"C1", "露台", "6"}, {"C2", "露台", "6"}
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
            log.info("演示数据补全: 桌台");
        } catch (Exception e) {
            log.warn("桌台演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 菜单分类: 空则补 */
    private void ensureDemoMenus(Long tid, Store s1) {
        try {
            if (menuCategoryRepository.count() > 0) return;
            String[] names = {"招牌洗护", "头皮护理", "造型设计", "精选好物"};
            for (int i = 0; i < names.length; i++) {
                MenuCategory mc = new MenuCategory();
                mc.setTenantId(tid);
                mc.setStoreId(s1.getId());
                mc.setName(names[i]);
                mc.setSortOrder(i + 1);
                menuCategoryRepository.save(mc);
            }
            log.info("演示数据补全: 菜单分类");
        } catch (Exception e) {
            log.warn("菜单分类演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 商城分类: 空则补 */
    private void ensureDemoMallCategories(Long tid) {
        try {
            if (mallCategoryRepository.count() > 0) return;
            String[][] cats = {{"洗护好物", "商品"}, {"男士专区", "商品"}, {"体验服务", "服务"}};
            for (String[] c : cats) {
                MallCategory mc = new MallCategory();
                mc.setTenantId(tid);
                mc.setName(c[0]);
                mc.setIcon(c[1]);
                mc.setStatus("ACTIVE");
                mc.setSortOrder(0);
                mallCategoryRepository.save(mc);
            }
            log.info("演示数据补全: 商城分类");
        } catch (Exception e) {
            log.warn("商城分类演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 游戏: 空则补 2 个带奖品 */
    private void ensureDemoGames(Long tid, Store s1) {
        try {
            if (gameRepository.count() > 0) return;
            LocalDateTime now = LocalDateTime.now();

            Game wheel = new Game();
            wheel.setTenantId(tid);
            wheel.setStoreId(s1.getId());
            wheel.setName("幸运大转盘");
            wheel.setType("WHEEL");
            wheel.setSubtitle("转动好运");
            wheel.setStartTime(now.minusDays(30));
            wheel.setEndTime(now.plusDays(90));
            wheel.setDailyLimit(3);
            wheel.setTotalLimit(0);
            wheel.setPointsCost(0);
            wheel.setStatus("ENABLED");
            wheel.setRules("每人每天可转动3次, 奖品包括优惠券和积分");
            entityManager.persist(wheel);
            entityManager.flush();
            Long wid = wheel.getId();
            GamePrize wp1 = prize(wid, "5元优惠券", "COUPON", 1L, "新人券", 200, 1);
            GamePrize wp2 = prize(wid, "50积分", "POINTS", null, null, 150, 2);
            GamePrize wp3 = prize(wid, "谢谢参与", "EMPTY", null, null, 650, 3);

            Game egg = new Game();
            egg.setTenantId(tid);
            egg.setStoreId(s1.getId());
            egg.setName("金蛋好运");
            egg.setType("EGG");
            egg.setSubtitle("砸出你的好运来");
            egg.setStartTime(now.minusDays(30));
            egg.setEndTime(now.plusDays(90));
            egg.setDailyLimit(1);
            egg.setTotalLimit(0);
            egg.setPointsCost(0);
            egg.setStatus("ENABLED");
            egg.setRules("每人每天可砸1个金蛋，奖品包括优惠券和积分");
            entityManager.persist(egg);
            entityManager.flush();
            Long eid = egg.getId();
            prize(eid, "10元优惠券", "COUPON", 1L, "新人券", 150, 1);
            prize(eid, "80积分", "POINTS", null, null, 300, 2);
            prize(eid, "谢谢参与", "EMPTY", null, null, 550, 3);
            log.info("演示数据补全: 游戏");
        } catch (Exception e) {
            log.warn("游戏演示数据补全失败: {}", e.getMessage());
        }
    }

    private GamePrize prize(Long gameId, String name, String type, Long refId, String refName, int prob, int sort) {
        GamePrize p = new GamePrize();
        p.setGameId(gameId);
        p.setName(name);
        p.setType(type);
        p.setRefId(refId);
        p.setRefName(refName);
        p.setProbability(prob);
        p.setSortOrder(sort);
        gamePrizeRepository.save(p);
        return p;
    }

    /** 商品: 空则补 6 服务 + 4 商品并关联商城分类 */
    private void ensureDemoProducts(Long tid, Store s1) {
        try {
            if (productRepository.count() > 0) return;
            Object[][] serviceProducts = {
                    {"男士剪发", 6800L, 1500L, 0},
                    {"女士烫染", 38800L, 12000L, 0},
                    {"头皮护理", 12800L, 3000L, 0},
                    {"造型设计", 9800L, 2500L, 0},
                    {"染发", 26800L, 8000L, 0},
                    {"洗发吹风", 3800L, 500L, 0},
            };
            for (Object[] row : serviceProducts) {
                Product p = new Product();
                p.setTenantId(tid);
                p.setName((String) row[0]);
                p.setCategory("SERVICE");
                p.setPrice((Long) row[1]);
                p.setCostPrice((Long) row[2]);
                p.setStatus("ACTIVE");
                p.setStoreIds(List.of(s1.getId()));
                productRepository.save(p);
            }
            Object[][] goodsProducts = {
                    {"护理套装", 29800L, 12000L, 50, 12},
                    {"洗发水 500ml", 8800L, 3500L, 100, 28},
                    {"护发素 500ml", 8800L, 3500L, 80, 18},
                    {"造型喷雾", 6800L, 2500L, 60, 8},
            };
            for (Object[] row : goodsProducts) {
                Product p = new Product();
                p.setTenantId(tid);
                p.setName((String) row[0]);
                p.setCategory("GOODS");
                p.setPrice((Long) row[1]);
                p.setCostPrice((Long) row[2]);
                p.setStock((Integer) row[3]);
                p.setStatus("ACTIVE");
                p.setStoreIds(List.of(s1.getId()));
                p.setSoldCount((Integer) row[4]);
                productRepository.save(p);
            }
            List<MallCategory> mallCats = mallCategoryRepository.findByTenantIdOrderBySortOrderAsc(tid);
            List<Product> products = productRepository.listActive(tid, null);
            if (!mallCats.isEmpty() && !products.isEmpty()) {
                int bound = Math.min(4, products.size());
                for (int i = 0; i < bound; i++) {
                    Product p = products.get(i);
                    p.setMallVisible(true);
                    p.setMallCategoryId(mallCats.get(i % mallCats.size()).getId());
                    productRepository.save(p);
                }
            }
            log.info("演示数据补全: 商品");
        } catch (Exception e) {
            log.warn("商品演示数据补全失败: {}", e.getMessage());
        }
    }

    /** 代理商: 空则补 2 个(必须带 tenantId, 租户隔离查询依赖) */
    private void ensureDemoAgents(Long tid) {
        try {
            if (agentRepository.countByTenantIdAndDeletedFalse(tid) > 0) return;
            Agent a1 = new Agent();
            a1.setTenantId(tid);
            a1.setName("张代理");
            a1.setContactName("张代理");
            a1.setContactPhone("13900000001");
            a1.setCommissionRate(150);
            a1.setStatus("ACTIVE");
            agentRepository.save(a1);
            Agent a2 = new Agent();
            a2.setTenantId(tid);
            a2.setName("李代理");
            a2.setContactName("李代理");
            a2.setContactPhone("13900000002");
            a2.setCommissionRate(80);
            a2.setStatus("ACTIVE");
            agentRepository.save(a2);
            log.info("演示数据补全: 代理商");
        } catch (Exception e) {
            log.warn("代理商演示数据补全失败: {}", e.getMessage());
        }
    }

    // ---- 构造辅助 ----

    private String genOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) (Math.random() * 9000) + 1000;
        return "OD" + ts + rand;
    }

    private Store store(String name, String addr, String phone, String hours, Long tid) {
        Store s = new Store();
        s.setTenantId(tid);
        s.setName(name);
        s.setAddress(addr);
        s.setPhone(phone);
        s.setBusinessHours(hours);
        s.setStatus("ACTIVE");
        entityManager.persist(s);
        entityManager.flush();
        return s;
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

    private void applyRecharge(Long tid, Member m, long amount, long gift, LocalDateTime when, Long opId, Long storeId) {        WalletTransaction tx = new WalletTransaction();
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
        entityManager.persist(c);
        entityManager.flush();
        return c;
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
        entityManager.persist(c);
        entityManager.flush();
        return c;
    }
}
