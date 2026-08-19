package com.huiji.task;

import com.huiji.entity.Order;
import com.huiji.repository.OrderRepository;
import com.huiji.service.OrderService;
import com.huiji.service.RechargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付超时关单: 每分钟扫描超过 30 分钟未支付的充值单与订单, 置为 CANCELLED。
 * 防止用户下单后不支付, 长期占用待支付状态; 关闭订单时归还占用的库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayCloseTask {

    private final RechargeService rechargeService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /** 超时分钟数 */
    private static final int EXPIRE_MINUTES = 30;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void closeExpired() {
        try {
            int rc = rechargeService.cancelExpired(EXPIRE_MINUTES);
            int oc = closeExpiredOrders();
            if (rc > 0 || oc > 0) {
                log.info("支付关单完成: 充值单 {} 笔, 订单 {} 笔", rc, oc);
            }
        } catch (Exception e) {
            log.error("支付关单任务异常", e);
        }
    }

    private int closeExpiredOrders() {
        List<Order> expired = orderRepository
                .findByStatusAndCreatedAtBefore("PENDING", LocalDateTime.now().minusMinutes(EXPIRE_MINUTES));
        int n = 0;
        for (Order o : expired) {
            orderService.restoreStock(o);
            o.setStatus("CANCELLED");
            orderRepository.save(o);
            n++;
        }
        return n;
    }
}
