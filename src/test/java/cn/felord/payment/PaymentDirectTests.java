package cn.felord.payment;

import cn.felord.payment.wechat.v3.SignatureProvider;
import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.WechatMetaBean;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import cn.felord.payment.wechat.v3.model.*;
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

    /**
     * The Wechat api provider.
     */
    @Autowired
    WechatApiProvider wechatApiProvider;
    /**
     * The Signature provider.
     */
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
        payParams.setOutTradeNo("X135444420201521613448");
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

        System.out.println("responseEntity = " + responseEntity);
// responseEntity = WechatResponseEntity(httpStatus=200, body={"prepay_id":"wx16140503583504b53b0ddcd64cc2430000"})
//        Assertions.assertThat(responseEntity.getBody().get("prepay_id").asText()).isNotBlank();
    }

    /**
     * Native支付商户服务端测试
     */
    @Test
    public void nativePayTest() {
        PayParams payParams = new PayParams();
        // 商品描述
        payParams.setDescription("felord-tool");
        payParams.setOutTradeNo("X13544442020121611341");
        payParams.setNotifyUrl("/wx/pay/notify");
        Amount amount = new Amount();
        amount.setTotal(100);
        payParams.setAmount(amount);
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.directPayApi(tenantId).nativePay(payParams);
        Assertions.assertThat(responseEntity.is2xxSuccessful()).isTrue();
        // responseEntity ---->  WechatResponseEntity(httpStatus=200, body={"code_url":"weixin://wxpay/bizpayurl?pr=R4KZBgV00"})
        Assertions.assertThat(responseEntity.getBody().get("code_url").asText()).isNotBlank();
    }

    /**
     * H5支付测试用例.
     */
    @Test
    public void h5PayTest() {
        PayParams payParams = new PayParams();

        payParams.setDescription("felord-tool");
        payParams.setOutTradeNo("X135144420201521613448");
        // 需要定义回调通知
        payParams.setNotifyUrl("/wx/pay/notify");
        Amount amount = new Amount();
        amount.setTotal(100);
        payParams.setAmount(amount);
        // h5支付需要传递场景信息 具体去看文档 这里只写必填项
        SceneInfo sceneInfo = new SceneInfo();

        sceneInfo.setPayerClientIp("127.0.0.1");

        H5Info h5Info = new H5Info();
        // 只有类型是必填项
        h5Info.setType("IOS");
        h5Info.setAppName("码农小胖哥");

        sceneInfo.setH5Info(h5Info);

        payParams.setSceneInfo(sceneInfo);
        WechatResponseEntity<ObjectNode> responseEntity = wechatApiProvider.directPayApi(tenantId).h5Pay(payParams);

        System.out.println("responseEntity = " + responseEntity);
    }
}
