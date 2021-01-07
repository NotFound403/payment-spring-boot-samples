package cn.felord.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayMarketingCampaignCashCreateRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayMarketingCampaignCashCreateResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 支付宝测试.
 */
@SpringBootTest
public class AliPayTests {
    @Autowired
    AlipayClient alipayClient;


    @Test
    public void campaignCash() throws AlipayApiException {
        AlipayMarketingCampaignCashCreateRequest request = new AlipayMarketingCampaignCashCreateRequest();
        request.setBizContent("{" +
                "\"coupon_name\":\"XXX周年庆红包\"," +
                "\"prize_type\":\"random\"," +
                "\"total_money\":\"10000.00\"," +
                "\"total_num\":\"1000\"," +
                "\"prize_msg\":\"XXX送您大红包\"," +
                "\"start_time\":\"2020-11-02 22:48:30\"," +
                "\"end_time\":\"2020-12-01 22:48:30\"," +
                "\"merchant_link\":\"http://www.weibo.com\"," +
                "\"send_freqency\":\"D3|L10\"" +
                "  }");

        AlipayMarketingCampaignCashCreateResponse execute = alipayClient.certificateExecute(request);

        System.out.println("execute = " + execute.getBody());
    }

    @Test
    public void F2F() throws AlipayApiException {

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest(); //创建API对应的request类
        request.setBizContent("{" +
                "\"out_trade_no\":\"20150320010101002\"," + //商户订单号
                "\"total_amount\":\"88.88\"," +
                "\"subject\":\"Iphone6 16G\"," +
                "\"store_id\":\"NJ_001\"," +
                "\"timeout_express\":\"90m\"}"); //订单允许的最晚付款时间
        AlipayTradePrecreateResponse response = alipayClient.certificateExecute(request);
        System.out.print(response.getBody());

    }

}
