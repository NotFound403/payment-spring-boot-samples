package cn.felord.payment;

import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import cn.felord.payment.wechat.v3.model.Amount;
import cn.felord.payment.wechat.v3.model.PayParams;
import cn.felord.payment.wechat.v3.model.Payer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 支付直连模式测试.
 *
 * @author Dax
 * @since 13 :39
 */
@SpringBootTest
public class PaymentDirectTests {
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
     * APP支付商户服务端测试.
     */
    @Test
    public void appPayTest() {
        PayParams payParams = new PayParams();

        payParams.setDescription("felord-tool");
        payParams.setOutTradeNo("X1354444202012161240");
        payParams.setNotifyUrl("/wx/pay/notify");
        Amount amount = new Amount();
        amount.setTotal(100);
        payParams.setAmount(amount);
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.directPayApi(tenantId).appPay(payParams);

        Assertions.assertThat(responseEntity.is2xxSuccessful()).isTrue();
        // responseEntity = WechatResponseEntity(httpStatus=200, body={"prepay_id":"wx1613461177695369fdbcfbd5ba8d0f0000"})
        Assertions.assertThat(responseEntity.getBody().get("prepay_id").asText()).isNotBlank();

    }

    /**
     * JSAPI、小程序支付商户服务端测试.
     */
    @Test
    public void jsPayTest() {
        PayParams payParams = new PayParams();

        payParams.setDescription("felord-tool");
        payParams.setOutTradeNo("X1354444202012161348");
        payParams.setNotifyUrl("/wx/pay/notify");
        Amount amount = new Amount();
        amount.setTotal(100);
        payParams.setAmount(amount);
        // 此类支付  Payer 必传  且openid需要同appid有绑定关系 具体去看文档
        Payer payer = new Payer();
        payer.setOpenid("ooadI5kQYrrCqpgbisvC8bEw_oUc");
        payParams.setPayer(payer);

        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.directPayApi(tenantId).jsPay(payParams);
        Assertions.assertThat(responseEntity.is2xxSuccessful()).isTrue();
// responseEntity = WechatResponseEntity(httpStatus=200, body={"prepay_id":"wx16140503583504b53b0ddcd64cc2430000"})
        Assertions.assertThat(responseEntity.getBody().get("prepay_id").asText()).isNotBlank();
    }

    /**
     * Native支付商户服务端测试
     */
    @Test
    public void nativePayTest() {
        PayParams payParams = new PayParams();

        payParams.setDescription("felord-tool");
        payParams.setOutTradeNo("X1354444202012161341");
        payParams.setNotifyUrl("/wx/pay/notify");
        Amount amount = new Amount();
        amount.setTotal(100);
        payParams.setAmount(amount);
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.directPayApi(tenantId).nativePay(payParams);
        Assertions.assertThat(responseEntity.is2xxSuccessful()).isTrue();
        // responseEntity ---->  WechatResponseEntity(httpStatus=200, body={"code_url":"weixin://wxpay/bizpayurl?pr=R4KZBgV00"})
        Assertions.assertThat(responseEntity.getBody().get("code_url").asText()).isNotBlank();
    }

    // 其它不再举例说明 结合微信文档进行处理。
}
