package com.wxmp.wxapi.process;



import com.alibaba.fastjson.JSONObject;

import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.util.HttpClientUtils;
import com.wxmp.core.util.HttpConnectionUtil;

import java.util.HashMap;

public class WxOpenApi {
    /**
     * 微信 API、微信开放平台接口
     *
     */
    // 获取component_access_token 接口
    public static final String GET_component_access_token = "https://api.weixin.qq.com/cgi-bin/component/api_component_token";


    // 获取OAuth2.0 Token
    public static String getcomponent_access_token(String component_appid, String component_appsecret, String component_verify_ticket) throws WxErrorException {
        OAuthAccessToken token = null;
        String tockenUrl = GET_component_access_token;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("component_appid",component_appid);
        jsonObject.put("component_appsecret",component_appsecret);
        jsonObject.put("component_verify_ticket",component_verify_ticket);
        HashMap header = new HashMap();
        header.put("Content-Type","application/json");

        String result = null;
        try {
           // result = HttpClientUtils.sendHttpPostJson(tockenUrl,map);
            result = HttpConnectionUtil.post(tockenUrl,header,jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject json  = new JSONObject();
        if(result!=null){
             json = JSONObject.parseObject(result);
        }
        String component_access_token = json.getString("component_access_token");
        String errorcode = json.getString("errcode");
        if(errorcode!=null){
            return errorcode;
        }
        return component_access_token;
    }
}
