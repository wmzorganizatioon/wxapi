package com.wxmp.wxapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.util.HttpConnectionUtil;
import com.wxmp.wxapi.process.HttpMethod;
import com.wxmp.wxapi.process.WxApi;
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
}
