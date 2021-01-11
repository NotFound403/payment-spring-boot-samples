package cn.felord.payment.controller;

import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.model.Amount;
import cn.felord.payment.wechat.v3.model.PayParams;
import cn.felord.payment.wechat.v3.model.Payer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口开发样例，以小程序支付为例.
 */
@Profile({"wechat", "dev"})
@RestController
@RequestMapping("/marketing")
public class PayController {
    @Autowired
    private WechatApiProvider wechatApiProvider;

    /**
     * 总流程建议为 生成商品订单 -> 生成对应的支付订单 -> 支付操作 -> 支付结果回调更新 -> 结束
     * <p>
     * 此处建议在商品订单生成之后调用
     *
     * @param orderId 商品订单id
     * @return the object node
     */
    @PostMapping("/js")
    public ObjectNode js(@RequestParam String orderId) {

        //TODO
        // 查询该orderId下是否生成了支付订单
        // 如果没有
        // 新增支付订单存入数据库 并标明支付状态为【待支付】
        // 根据新生成的支付订单信息向微信支付发起支付 并根据返回结果进行处理
        // 如果有状态为待支付
        // 根据待支付订单信息向微信支付发起支付 并根据返回结果进行处理
        // 如果有状态为待支付之外的状态
        // 根据产品的业务设计自行实现
        // 支付状态更新逻辑在【回调接口 /wx/pay/notify】中处理  需要幂等处理

        // 开发时需要指定使用的商户租户配置 这里为 mobile 请参考 application-wechat.yml
        String tenantId = "mobile";

        PayParams payParams = new PayParams();

        payParams.setDescription("felord.cn");
        //
        // 商户侧唯一订单号 建议为商户侧支付订单号 订单表主键 或者唯一标识字段
        payParams.setOutTradeNo("X135423420201521613448");
        // 需要定义回调通知
        payParams.setNotifyUrl("/wx/pay/notify");
        Amount amount = new Amount();
        amount.setTotal(100);
        payParams.setAmount(amount);
        // 此类支付  Payer 必传  且openid需要同appid有绑定关系 具体去看文档
        Payer payer = new Payer();
        payer.setOpenid("ooadI5kQYrrCqpgbisvC8bEw_oUc");
        payParams.setPayer(payer);

        return wechatApiProvider.directPayApi(tenantId)
                .jsPay(payParams)
                .getBody();
    }
}
