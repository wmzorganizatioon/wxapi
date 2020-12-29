package com.wxmp.wxapi.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.util.HttpConnectionUtil;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.process.WxOpenApi;
import com.wxmp.wxapi.service.ComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class ComponentServiceImpl implements ComponentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentServiceImpl.class);

    /**
     *@Author 86151
     *@Date 2020/12/28 16:09
     *Description 获取预授权码
     * * @param  : component_access_token * @param  : component_appid
     * * @return : java.lang.String
     */
    @Override
    public String getPreAuthCode(String component_access_token, String component_appid) {
        String uri = String.format(WxApi.COMPONENT_ACCESS_TOKEN, component_access_token);
//        String pre_auth_code = String.valueOf(WxApi.httpsRequest(uri, HttpMethod.POST, component_appid).get("pre_auth_code"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("component_appid",component_appid);

        HashMap header = new HashMap();
        header.put("Content-Type","application/json");
        String result = null;
        try {
            // result = HttpClientUtils.sendHttpPostJson(tockenUrl,map);
              result = HttpConnectionUtil.post(uri,header,jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject json  = new JSONObject();
        if(result!=null){
            json = JSONObject.parseObject(result);
        }
        if(json.getString("errcode")!=null){
            LOGGER.debug("errcode：" + json.getString("errcode"));
        }else {
            LOGGER.debug("获取预授权码：" + json.getString("pre_auth_code"));
        }
        return json.getString("pre_auth_code");
    }
    @Override
    public String buildComponent_access_token(String appId, String ticket) throws WxErrorException {
        String component_appid = "wx4d553967d6422132";
        String component_appsecret = "f0a6b4545311382164e96733e0f885ed";
        LOGGER.info("ticket:"+ticket);
        String component_verify_ticket = ticket;
        String component_access_token = new WxOpenApi().getcomponent_access_token(component_appid,component_appsecret,component_verify_ticket);
        LOGGER.info("component_access_token:"+component_access_token);
        return component_access_token;
    }

    @Override
    public boolean refreshtoken(String component_access_token, String authorizer_appid) {
        String component_appid = "wx4d553967d6422132";
        String authorizer_refresh_token = "";
        if (component_access_token == null) {
            //获取component_access_token
        }else{
            //通过authorizer_appid获取authorizer_refresh_token

        }
        boolean result = new WxOpenApi().refreshtoken(component_access_token,component_appid,authorizer_appid,authorizer_refresh_token);
        return result;
    }
}
