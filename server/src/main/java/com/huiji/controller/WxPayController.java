package com.huiji.controller;

import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.entity.Member;
import com.huiji.entity.Order;
import com.huiji.entity.OrderItem;
import com.huiji.entity.RechargeOrder;
import com.huiji.entity.WxAccount;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.OrderItemRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.RechargeOrderRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberContext;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.OrderService;
import com.huiji.service.RechargeService;
import com.huiji.service.WxMpConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信支付: JSAPI 下单、支付回调、订单查询。
 * 路径前缀 /api/wxpay, notify 公开, 其余需要 memberToken 或 adminToken。
 */
@Slf4j
@RestController
@RequestMapping("/api/wxpay")
@RequiredArgsConstructor
public class WxPayController {

    private final WxMpConfigService wxMpConfigService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final OrderService orderService;
    private final MemberTokenUtil memberTokenUtil;
    private final RechargeService rechargeService;
    private final RechargeOrderRepository rechargeOrderRepository;

    /** 1. JSAPI 支付下单(需要 memberToken) */
    @PostMapping("/order/{orderId}")
    public Result<Map<String, Object>> createPayOrder(HttpServletRequest req, @PathVariable Long orderId) {
        long[] ctx = currentMember(req);
        Long tenantId = ctx[1];

        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "订单状态不可支付: " + order.getStatus());
        }

        WxPayService payService = wxMpConfigService.getPayService(tenantId);
        if (payService == null) {
            return Result.fail("NOT_CONFIGURED", "未配置微信支付");
        }

        // 获取会员 openid
        if (order.getMemberId() == null) {
            throw new BizException(ErrorCode.BIZ_ERROR, "订单未绑定会员, 无法发起微信支付");
        }
        Member member = memberRepository.findByIdAndTenantIdAndDeletedFalse(order.getMemberId(), tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        String openid = member.getWxOpenid();
        if (openid == null || openid.isBlank()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "会员未绑定微信, 无法发起微信支付");
        }

        // 商品名称取订单第一个商品, 没有则用订单号
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        String body = items.isEmpty() ? "订单" + order.getOrderNo() : items.get(0).getProductName();

        // 金额(分)
        int totalFee = order.getTotalAmount() == null ? 0 : order.getTotalAmount().intValue();

        // 回调地址
        WxAccount account = wxMpConfigService.getAccount(tenantId);
        String notifyUrl = resolveNotifyUrl(account);

        WxPayUnifiedOrderRequest request = new WxPayUnifiedOrderRequest();
        request.setBody(body);
        request.setOutTradeNo(order.getOrderNo());
        request.setTotalFee(totalFee);
        request.setSpbillCreateIp(getClientIp(req));
        request.setNotifyUrl(notifyUrl);
        request.setTradeType("JSAPI");
        request.setOpenid(openid);

        try {
            // getPayInfo 返回 JSAPI 支付参数: appId, timeStamp, nonceStr, package, signType, paySign
            Map<String, String> payInfo = payService.getPayInfo(request);
            Map<String, Object> payParams = new LinkedHashMap<>(payInfo);
            return Result.success(payParams);
        } catch (WxPayException e) {
            log.error("微信支付下单失败 orderNo={}", order.getOrderNo(), e);
            return Result.fail("PAY_FAIL", "微信支付下单失败，请稍后重试");
        }
    }

    /** 2. 微信支付回调(公开, 微信服务器调用) */
    @PostMapping(value = "/notify", produces = MediaType.APPLICATION_XML_VALUE)
    public String notify(@RequestBody String xmlData) {
        try {
            // 先从 XML 中提取 out_trade_no, 用于定位租户
            String outTradeNo = extractXmlTag(xmlData, "out_trade_no");
            if (outTradeNo == null || outTradeNo.isBlank()) {
                log.warn("微信支付回调缺少 out_trade_no");
                return WxPayNotifyResponse.fail("缺少 out_trade_no");
            }
            // 充值单回调(RC 前缀)走充值入账
            if (outTradeNo.startsWith("RC")) {
                return handleRechargeNotify(xmlData, outTradeNo);
            }

            Order order = orderRepository.findByOrderNoAndDeletedFalse(outTradeNo)
                    .orElse(null);
            if (order == null) {
                log.warn("微信支付回调订单不存在: {}", outTradeNo);
                return WxPayNotifyResponse.fail("订单不存在");
            }

            Long tenantId = order.getTenantId();
            WxPayService payService = wxMpConfigService.getPayService(tenantId);
            if (payService == null) {
                log.warn("租户 {} 未配置微信支付", tenantId);
                return WxPayNotifyResponse.fail("未配置微信支付");
            }

            // 验签并解析
            WxPayOrderNotifyResult result = payService.parseOrderNotifyResult(xmlData);
            if (!"SUCCESS".equalsIgnoreCase(result.getReturnCode())
                    || !"SUCCESS".equalsIgnoreCase(result.getResultCode())) {
                log.warn("微信支付回调失败: outTradeNo={}, returnCode={}, resultCode={}, returnMsg={}",
                        outTradeNo, result.getReturnCode(), result.getResultCode(), result.getReturnMsg());
                return WxPayNotifyResponse.fail("支付未成功");
            }

            // 金额校验: 微信实付金额必须等于订单应付, 防止金额篡改
            long payable = order.getTotalAmount()
                    - (order.getDiscountAmount() == null ? 0L : order.getDiscountAmount());
            if (result.getTotalFee() == null || result.getTotalFee().intValue() != (int) payable) {
                log.warn("微信支付回调金额不一致: orderNo={}, expected={}, actual={}",
                        outTradeNo, payable, result.getTotalFee());
                return WxPayNotifyResponse.fail("金额不一致");
            }

            // 标记订单已支付
            orderService.markPaidByWxNotify(outTradeNo, result.getTransactionId());
            log.info("微信支付回调成功: outTradeNo={}, transactionId={}", outTradeNo, result.getTransactionId());
            return WxPayNotifyResponse.success("OK");
        } catch (Exception e) {
            log.error("微信支付回调处理异常", e);
            return WxPayNotifyResponse.fail("处理失败");
        }
    }

    /** 充值单支付回调: 验签 + 金额校验 + 幂等入账 */
    private String handleRechargeNotify(String xmlData, String outTradeNo) {
        try {
            RechargeOrder order = rechargeOrderRepository.findByOutTradeNoAndDeletedFalse(outTradeNo)
                    .orElse(null);
            if (order == null) {
                log.warn("微信支付回调充值单不存在: {}", outTradeNo);
                return WxPayNotifyResponse.fail("充值单不存在");
            }
            Long tenantId = order.getTenantId();
            WxPayService payService = wxMpConfigService.getPayService(tenantId);
            if (payService == null) {
                log.warn("租户 {} 未配置微信支付", tenantId);
                return WxPayNotifyResponse.fail("未配置微信支付");
            }
            WxPayOrderNotifyResult result = payService.parseOrderNotifyResult(xmlData);
            if (!"SUCCESS".equalsIgnoreCase(result.getReturnCode())
                    || !"SUCCESS".equalsIgnoreCase(result.getResultCode())) {
                log.warn("充值支付回调失败: outTradeNo={}, resultCode={}, returnMsg={}",
                        outTradeNo, result.getResultCode(), result.getReturnMsg());
                return WxPayNotifyResponse.fail("支付未成功");
            }
            if (result.getTotalFee() == null || result.getTotalFee().intValue() != order.getAmount().intValue()) {
                log.warn("充值支付回调金额不一致: outTradeNo={}, expected={}, actual={}",
                        outTradeNo, order.getAmount(), result.getTotalFee());
                return WxPayNotifyResponse.fail("金额不一致");
            }
            rechargeService.markPaidByNotify(outTradeNo, result.getTransactionId());
            log.info("充值支付回调成功: outTradeNo={}, transactionId={}", outTradeNo, result.getTransactionId());
            return WxPayNotifyResponse.success("OK");
        } catch (Exception e) {
            log.error("充值支付回调处理异常", e);
            return WxPayNotifyResponse.fail("处理失败");
        }
    }

    /** 3. 查询支付状态(需要 memberToken 或 adminToken) */
    @GetMapping("/query/{orderId}")
    public Result<Map<String, Object>> queryOrder(HttpServletRequest req, @PathVariable Long orderId) {
        Long tenantId = resolveTenantId(req);
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));

        WxPayService payService = wxMpConfigService.getPayService(tenantId);
        if (payService == null) {
            return Result.fail("NOT_CONFIGURED", "未配置微信支付");
        }

        try {
            WxPayOrderQueryResult result = payService.queryOrder(order.getOrderNo(), null);
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("orderNo", result.getOutTradeNo());
            vo.put("transactionId", result.getTransactionId());
            vo.put("tradeState", result.getTradeState());
            vo.put("tradeStateDesc", result.getTradeStateDesc());
            vo.put("totalFee", result.getTotalFee());
            return Result.success(vo);
        } catch (WxPayException e) {
            log.error("微信支付查询失败 orderNo={}", order.getOrderNo(), e);
            return Result.fail("QUERY_FAIL", "查询支付状态失败，请稍后重试");
        }
    }

    // ---- 工具方法 ----

    /** 解析会员或管理员身份, 返回 tenantId */
    private Long resolveTenantId(HttpServletRequest req) {
        // 优先取 adminToken(JwtAuthFilter 已注入 LoginUserHolder)
        LoginUser admin = LoginUserHolder.getOrNull();
        if (admin != null && admin.getTenantId() != null) {
            return admin.getTenantId();
        }
        // 再取 memberToken
        long[] ctx = currentMember(req);
        return ctx[1];
    }

    /** 解析 memberToken, 返回 [memberId, tenantId] */
    private long[] currentMember(HttpServletRequest req) {
        return MemberContext.require(req, memberTokenUtil);
    }

    /** 从 XML 中提取标签值(兼容 CDATA) */
    private String extractXmlTag(String xml, String tag) {
        String openTag = "<" + tag + ">";
        String closeTag = "</" + tag + ">";
        int start = xml.indexOf(openTag);
        if (start < 0) return null;
        start += openTag.length();
        int end = xml.indexOf(closeTag, start);
        if (end < 0) return null;
        String value = xml.substring(start, end);
        // 去除 CDATA 包裹
        if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
            value = value.substring(9, value.length() - 3);
        }
        return value;
    }

    /** 拼接支付回调地址 */
    private String resolveNotifyUrl(WxAccount account) {
        String domain = null;
        if (account != null && account.getDomain() != null && !account.getDomain().isBlank()) {
            domain = account.getDomain().replaceAll("/+$", "");
        }
        if (domain == null || domain.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "未配置回调域名, 请先在微信配置中设置 domain");
        }
        return domain + "/api/wxpay/notify";
    }

    /** 获取客户端 IP */
    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return req.getRemoteAddr();
    }
}
