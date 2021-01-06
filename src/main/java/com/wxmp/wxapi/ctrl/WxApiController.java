package com.wxmp.wxapi.ctrl;

import com.qq.weixin.mp.aes.AesException;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;
import com.wxmp.core.common.BaseCtrl;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class WxApiController extends BaseCtrl {

    private static Logger log = LogManager.getLogger(WxApiCtrl.class);

    private String encodingAesKey = "ndbBco26cAH3rBvDfFP0JmdCdxJ2AdqWbmQKr3gtwTB";

    private String messageCheckToken = "0c5cc9cebcc54c4ea1b2a7e1a00db569";

    private String componentAppId = "wx4d553967d6422132";

    private String componentAppSecret = "f0a6b4545311382164e96733e0f885ed";

    /**
     *@Author 86151
     *@Date 2021/1/5 15:29
     *Description 进入获取消息事件接收接口
     * * @param  : appId * @param  : timestamp * @param  : nonce * @param  : msgSignature * @param  : postData
     * * @return : java.lang.String
     */
    @PostMapping("/{APPID}/callback")
    @ResponseBody
    public String getCallBack(@PathVariable("APPID") String appId, @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce, @RequestParam("msg_signature")String msgSignature, @RequestBody String postData) throws AesException {
        log.debug("成功进入获取消息事件接收接口");
        log.debug("timestamp：" + timestamp);
        log.debug("appId的值：" + appId);
        log.debug("nonce：" + nonce);
        log.debug("msgSignature：" + msgSignature);
        log.debug("postData：" + postData);

        // 解密类型aes
        WXBizMsgCrypt pc = new WXBizMsgCrypt(messageCheckToken, encodingAesKey, componentAppId);

        // 进行解密
        String decryData = pc.decryptMsg(msgSignature, timestamp, nonce, postData);

        log.debug("\n");

        log.debug("解密xml文件：" + decryData);

        return "success";
    }
}
