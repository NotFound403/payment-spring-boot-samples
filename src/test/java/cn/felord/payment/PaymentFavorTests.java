package cn.felord.payment;

import cn.felord.payment.wechat.enumeration.CouponBgColor;
import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import cn.felord.payment.wechat.v3.model.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

/**
 * 微信支付代金券测试，启用的为application-wechat.yml
 */
@SpringBootTest
class PaymentFavorTests {
    /**
     * 配置中的租户
     */
    String tenantId = "mobile";
    /**
     * The Wechat api provider.
     */
    @Autowired
    WechatApiProvider wechatApiProvider;


    /**
     * 制券测试，需要激活后才能流通使用.
     * <p>
     * 建议商户侧在制券成功后保存制券的批次元信息
     */
    @Test
    public void createStockTest() {
        StocksCreateParams params = new StocksCreateParams();
        // 券名称 不要出现 测试  TEST 商家 以及其它一些敏感词汇  推荐 满XX减XX
        params.setStockName("XX券满20减1");
        // 券备注 开发运营可见
        params.setComment("业务分类一");
        // 发券必须是未来时刻
        OffsetDateTime availableBeginTime = OffsetDateTime.now(ZoneOffset.ofHours(8)).plusMinutes(2);
        params.setAvailableBeginTime(availableBeginTime);
        // 结束时间应该大于开始时间  同时两者间隔不应该大于90Days
        OffsetDateTime availableEndTime = availableBeginTime.plusDays(1);
        params.setAvailableEndTime(availableEndTime);
        //  false 为预充值代金券   true 为免充值代金券
        params.setNoCash(false);
        // 商户侧唯一订单号 一定要唯一
        params.setOutRequestNo("Q20201213000001");

        // 核销规则
        CouponUseRule couponUseRule = new CouponUseRule();
        // 固定面额满减券使用规则  满20减1
        FixedNormalCoupon fixedNormalCoupon = new FixedNormalCoupon();
        // 优惠券面值  100分  ￥1块
        long couponAmount = 100L;
        fixedNormalCoupon.setCouponAmount(couponAmount);
        // 满减条件  2000分  ￥20块
        fixedNormalCoupon.setTransactionMinimum(2000L);
        couponUseRule.setFixedNormalCoupon(fixedNormalCoupon);
        // 可叠加
        couponUseRule.setCombineUse(true);
        //TODO 设置可核销商户列表  需要设置一个到多个微信支付商户号
        couponUseRule.setAvailableMerchants(Arrays.asList("XXX13XX2", "xxxxXXX"));
        params.setCouponUseRule(couponUseRule);

        //发放规则
        StockUseRule stockUseRule = new StockUseRule();
        // 发券数
        long maxCoupons = 5L;
        stockUseRule.setMaxCoupons(maxCoupons);
        // 最大预算  固定算法
        stockUseRule.setMaxAmount(maxCoupons * couponAmount);
        // 最高消耗金额  固定算法  couponAmount*n
        long n = 1L;
        stockUseRule.setMaxAmountByDay(couponAmount * n);
        // 每个批次
        stockUseRule.setMaxCouponsPerUser(60L);
        // 开启自然人防刷
        stockUseRule.setNaturalPersonLimit(true);
        // api发券防刷
        stockUseRule.setPreventApiAbuse(true);
        params.setStockUseRule(stockUseRule);

        // 样式
        PatternInfo patternInfo = new PatternInfo();
        // 设置优惠券描述
        patternInfo.setDescription("核销后不退、不找零；最终解释权归XXX公司所有；");
        // 优惠券背景色
        patternInfo.setBackgroundColor(CouponBgColor.COLOR070);
        // 设置商家名称
        patternInfo.setMerchantName("XX购物");
        params.setPatternInfo(patternInfo);

        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).createStock(params);
        // 成功后 响应体会返回 stock_id
        Assertions.assertThat(responseEntity.getBody().get("stock_id")).isNotNull();
    }

    /**
     * 代金券激活,制券与激活官方建议间隔大于1分钟
     * <p>
     * 建议商户侧在开发时回填激活状态
     */
    @Test
    public void startStockTest() {
//        15347538
        WechatResponseEntity<ObjectNode> wechatResponseEntity = wechatApiProvider.favorApi(tenantId).startStock("15347538");
        // 成功后 响应体会返回 stock_id
        Assertions.assertThat(wechatResponseEntity.getBody().get("stock_id")).isNotNull();
    }

    /**
     * 优惠券的发放.
     * <p>
     * 商户侧开发时建议增加发放流水记录。
     * <p>
     * 微信支付文档所要求的微信公众号服务号不是必须的，只要你有一个绑定了微信支付商户平台和开放平台的appid即可。
     * <p>
     * 流程为：
     * 1. appid 请求授权微信登录。
     * 2. 登录成功后，开发者在商户侧保存用户 <strong>对应此appid的openid</strong>。
     * 3. 通过 appid - openid 进行发券。
     */
    @Test
    public void sendStock() {

        final StocksSendParams params = new StocksSendParams();
        // 发券只需要传递以下三个参数
        // 批次id
        params.setStockId("15347538");
        // 用户对应 wechat.pay.v3.<tenantId>.app-id 的 openid
        params.setOpenid("ooadI5kQYrrCqpgbisvC8bEw_oUc");
        // 商户侧保证唯一的流水号
        params.setOutRequestNo("D12456202012161641219x");
        // 指定面额发券，面额  注意场景   选填
        // params.setCouponValue(100L);
        // 指定面额发券批次门槛 注意场景  选填
        // params.setCouponMinimum(2000L);
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).sendStock(params);
        // 成功后 响应体会返回 coupon_id 18762927655
        System.out.println("responseEntity = " + responseEntity);
        Assertions.assertThat(responseEntity.getBody().get("coupon_id")).isNotNull();
    }

    /**
     * 暂停代金券批次测试.
     */
    @Test
    public void pauseStockTest() {
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).pauseStock("15337294");
        // 成功后 响应体会返回 stock_id
        Assertions.assertThat(responseEntity.getBody().get("stock_id")).isNotNull();
    }

    /**
     * 暂停代金券批次测试.
     */
    @Test
    public void restartStockTest() {
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).restartStock("15337294");
        // 成功后 响应体会返回 stock_id
        Assertions.assertThat(responseEntity.getBody().get("stock_id")).isNotNull();
    }

    /**
     * 条件查询批次列表测试.
     */
    @Test
    public void queryStocksByMchTest() {
        StocksQueryParams params = new StocksQueryParams();
        // 分页页码 默认从0开始 必填
        params.setOffset(0);
        // 分页大小 最大10 必填
        params.setLimit(10);
        // 起始时间  选填
        // params.setCreateStartTime();
        // 终止时间 选填
        // params.setCreateEndTime();
        // 券状态 选填
        // params.setStatus();
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).queryStocksByMch(params);
        Assertions.assertThat(responseEntity).isNotNull();
    }

    /**
     * 查询批次详情测试.
     */
    @Test
    public void queryStockDetailTest() {
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).queryStockDetail("15337294");
        Assertions.assertThat(responseEntity).isNotNull();
    }


    /**
     * 查询代金券详情测试.
     */
    @Test
    public void queryCouponDetailsTest() {
        CouponDetailsQueryParams params = new CouponDetailsQueryParams();
        params.setCouponId("12646688788");
        params.setOpenId("omDFY5rnZd2_0f-pMWJs2A3zd57c");
        // appid 自动注入 注意  appid 和 openid 要对应
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).queryCouponDetails(params);
        Assertions.assertThat(responseEntity).isNotNull();
    }

    /**
     * 查询代金券可用商户测试.
     * <p>
     * 页码 超出会 {"code":"MCH_NOT_EXISTS","message":"商户号不合法"}
     */
    @Test
    public void queryMerchantsByStockIdTest() {
        MchQueryParams params = new MchQueryParams();
        // 分页页码，最大1000
        params.setOffset(0);
        // 分页大小，最大50
        params.setLimit(10);
        // 批次号
        params.setStockId("15337294");
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).queryMerchantsByStockId(params);
        // WechatResponseEntity(httpStatus=200, body={"data":["1xxxxx9"],"limit":10,"offset":0,"stock_id":"15337294","total_count":1})
        Assertions.assertThat(responseEntity).isNotNull();
    }


    /**
     * 查询代金券可用单品测试.
     * <p>
     * 页码 超出会 {"code":"MCH_NOT_EXISTS","message":"商户号不合法"}
     */
    @Test
    public void queryStockItemsTest() {
        MchQueryParams params = new MchQueryParams();
        // 分页页码，最大500
        params.setOffset(0);
        // 分页大小，最大100
        params.setLimit(10);
        // 批次号
        params.setStockId("15337294");
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).queryStockItems(params);
        Assertions.assertThat(responseEntity).isNotNull();
    }


    /**
     * 根据商户号查用户的券测试.
     * <p>
     * 页码 超出会 {"code":"MCH_NOT_EXISTS","message":"商户号不合法"}
     */
    @Test
    public void queryUserCouponsByMchIdTest() {

        UserCouponsQueryParams params = new UserCouponsQueryParams();
        params.setOpenId("omDFY5rnZd2_0f-pMWJs2A3zd57c");
        // appid 自动注入 注意  appid 和 openid 要对应
        // 其它参数参考文档
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).queryUserCouponsByMchId(params);
        Assertions.assertThat(responseEntity).isNotNull();
    }


    /**
     * 下载批次核销明细测试.
     */
    @Test
    public void downloadStockUseFlowTest() {
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).downloadStockUseFlow("15309595");
        String csv = responseEntity.getBody().get("csv").asText();
        // "批次id,优惠id,优惠类型,优惠金额（元）,订单总金额（元）,交易类型,支付单号,消耗时间,消耗商户号,设备号,银行流水号,单品信息\r\n`15*****5,`1818****032,`全场代金券,`1.00,`2.00,`支付,`42000************999,`2020-11-27 16:23:05,`1******9,\"1******0\",\"\",\"\"\r\n总条数\r\n`1\r\n"
        Assertions.assertThat(csv).isNotNull();
    }

    /**
     * 下载批次退款明细测试.
     */
    @Test
    public void downloadStockRefundFlowTest() {
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).downloadStockRefundFlow("15309595");
        String csv = responseEntity.getBody().get("csv").asText();

        Assertions.assertThat(csv).isNotNull();
    }

    /**
     * 设置消息通知地址测试.
     */
    @Test
    public void setMarketingFavorCallbackTest() {
        String notifyUrl = "https://felord.cn/wx/coupon_callback";
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).setMarketingFavorCallback(notifyUrl);

        // 成功后 响应体会返回 mchid notifyUrl
        Assertions.assertThat(responseEntity.getBody().get("mchid")).isNotNull();
        Assertions.assertThat(responseEntity.getBody().get("notify_url").asText()).isEqualTo(notifyUrl);
    }

    /**
     * 营销图片上传测试.
     */
    @Test
    public void marketingImageUploadTest() throws IOException {

        FileSystemResource fileSystemResource = new FileSystemResource("C:\\Pictures\\1_aGzEMxe9vxr9NHDl7kuKYg.png");

        String filename = "1_aGzEMxe9vxr9NHDl7kuKYg.png";
        InputStream inputStream = fileSystemResource.getInputStream();
        MultipartFile mockMultipartFile = new MockMultipartFile(filename, filename, null, inputStream);
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.favorApi(tenantId).marketingImageUpload(mockMultipartFile);
        // {"media_url":"https://wxpaylogo.qpic.cn/wxpaylogo/PiajxSqBRaEIPAeia7Imvtsp7V8fibhVcCHXcEGrPNeACFw0sBZ4vAUvQ/0"}
        Assertions.assertThat(responseEntity.getBody().get("media_url")).isNotNull();
    }

}
