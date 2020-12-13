package cn.felord.payment.controller;


import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 营销图片上传.
 */
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
