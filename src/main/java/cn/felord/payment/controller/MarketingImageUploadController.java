package cn.felord.payment.controller;


import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 注意为了演示该配置在使用微信配置application-wechat.yaml才生效
 * <p>
 * 微信营销图片上传.
 */
@Profile({"wechat"})
@RestController
@RequestMapping("/marketing")
public class MarketingImageUploadController {
    @Autowired
    private WechatApiProvider wechatApiProvider;

    /**
     * Upload wechat response entity.
     *
     * @param file the file
     * @return the wechat response entity
     */
    @PostMapping("/upload")
    public WechatResponseEntity<ObjectNode> upload(MultipartFile file) {
        String tenantId = "mobile";
        return wechatApiProvider.favorApi(tenantId).marketingImageUpload(file);
    }

}
