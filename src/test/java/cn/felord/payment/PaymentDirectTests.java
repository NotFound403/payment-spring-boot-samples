package cn.felord.payment;

import cn.felord.payment.wechat.v3.*;
import cn.felord.payment.wechat.v3.model.Amount;
import cn.felord.payment.wechat.v3.model.PayParams;
import cn.felord.payment.wechat.v3.model.Payer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Base64Utils;
import org.springframework.util.IdGenerator;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 微信支付直连模式测试，启用的为application-wechat.yml
 *
 * @author Dax
 * @since 13 :39
 */
@ActiveProfiles("wechat")
@SpringBootTest
public class PaymentDirectTests {
    /**
     * 配置中的租户
     */
    String tenantId = "mobile";

    @Autowired
    WechatApiProvider wechatApiProvider;
    @Autowired
    SignatureProvider signatureProvider;

    /**
     * 签名验证.
     */
    @SneakyThrows
    @Test
    void signAndVerifyTest() {

        WechatMetaBean wechatMetaBean = signatureProvider.wechatMetaContainer().getWechatMeta(tenantId);
        Signature signer = Signature.getInstance("SHA256withRSA");
        // 私钥加签
        signer.initSign(wechatMetaBean.getKeyPair().getPrivate());

        long timestamp = System.currentTimeMillis() / 1000;
        System.out.println("appid = wx55a75ae9fd5d3b78");
        System.out.println("timestamp = " + timestamp);
        IdGenerator ID_GENERATOR = new AlternativeJdkIdGenerator();
        String nonceStr = ID_GENERATOR.generateId()
                .toString()
                .replaceAll("-", "");
        System.out.println("nonceStr = " + nonceStr);
        String prepay_id = "wx201410272009395522657a690389285100";
        System.out.println("prepay_id = " + prepay_id);
        String signatureStr = Stream.of("wx55a75ae9fd5d3b78", String.valueOf(timestamp), nonceStr, prepay_id)
                .collect(Collectors.joining("\n", "", "\n"));

        signer.update(signatureStr.getBytes(StandardCharsets.UTF_8));
        String encode = Base64Utils.encodeToString(signer.sign());
        // 公钥 验证签名
        signer.initVerify(wechatMetaBean.getKeyPair().getPublic());
        signer.update(signatureStr.getBytes(StandardCharsets.UTF_8));
        boolean verify = signer.verify(Base64Utils.decode(encode.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(verify).isTrue();
    }

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
        // 需要定义回调通知
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
    // 小程序拉起支付参数测试

    @Autowired
    WechatPayClient wechatPayClient;
    @SneakyThrows
    @Test
    public void miniApp() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = new AlternativeJdkIdGenerator().generateId()
                .toString()
                .replaceAll("-", "");

        String packageStr = "prepay_id=wx1613461177695369fdbcfbd5ba8d0f0000";
        WechatMetaContainer wechatMetaContainer = wechatPayClient.signatureProvider().wechatMetaContainer();
        Signature signer = Signature.getInstance("SHA256withRSA");
        WechatMetaBean wechatMetaBean = wechatMetaContainer.getWechatMeta("mobile");
        signer.initSign(wechatMetaBean.getKeyPair().getPrivate());
        String signatureStr = Stream.of(wechatMetaBean.getV3().getAppId(), timestamp, nonceStr, packageStr)
                .collect(Collectors.joining("\n", "", "\n"));

        signer.update(signatureStr.getBytes(StandardCharsets.UTF_8));
        String paySign = Base64Utils.encodeToString(signer.sign());

        // 公钥 验证签名
        signer.initVerify(wechatMetaBean.getKeyPair().getPublic());
        signer.update(signatureStr.getBytes(StandardCharsets.UTF_8));
        boolean verify = signer.verify(Base64Utils.decode(paySign.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(verify).isTrue();


        Map<String, String> map = new HashMap<>();
        map.put("timeStamp", timestamp);
        map.put("nonceStr", nonceStr);
        map.put("package", packageStr);
        map.put("signType", "RSA");
        map.put("paySign", paySign);
        System.out.println("map = " + new ObjectMapper().writeValueAsString(map));
    }

}
