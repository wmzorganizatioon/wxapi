package com.wxmp.wxapi.service;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.wxapi.vo.TemplateMessage;

/**
 *@Author Wisdom
 *@date 2020/12/28 16:07
 *@description 第三方信息一直到预授权码
 *return
 */
public interface ComponentService {

    /**
     *@Author 86151
     *@Date 2020/12/28 16:09
     *Description 获取预授权码
     * * @param  : component_access_token * @param  : component_appid
     * * @return : java.lang.String
     */
    //第三方Component_access_token
    public String buildComponent_access_token(String appId,String ticket)throws WxErrorException;

    boolean refreshtoken(String component_access_token, String authorizer_appid);

    String getPreAuthCode(String component_access_token, String component_appid) throws Exception;

    String selectAuthInfo(String component_access_token, String component_appid, String authorization_code);


    boolean sendTemplateMessage(TemplateMessage tplMsg, String authorizer_appid);
}
