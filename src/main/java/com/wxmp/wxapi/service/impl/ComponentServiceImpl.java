package com.wxmp.wxapi.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.util.HttpConnectionUtil;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.process.WxOpenApi;
import com.wxmp.wxapi.service.ComponentService;
import com.wxmp.wxapi.vo.TemplateMessage;
import com.wxmp.wxcms.domain.Authorizer;
import com.wxmp.wxcms.mapper.ComponentDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
public class ComponentServiceImpl implements ComponentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentServiceImpl.class);

    @Resource
    private ComponentDao componentDao;

    /**
     *@Author 86151
     *@Date 2020/12/28 16:09
     *Description 获取预授权码
     * * @param  : component_access_token * @param  : component_appid
     * * @return : java.lang.String
     */
    @Override
    public String getPreAuthCode(String component_access_token, String component_appid) throws Exception{
        String uri = String.format(WxApi.COMPONENT_ACCESS_TOKEN, component_access_token);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("component_appid",component_appid);

        HashMap header = new HashMap();
        header.put("Content-Type","application/json");
        String result = null;
        result = HttpConnectionUtil.post(uri,header,jsonObject.toString());

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
        Authorizer authorizer = componentDao.queryAuthInfo(authorizer_appid);
        String authorizer_refresh_token = "";
        if(authorizer!=null){
            authorizer_refresh_token = authorizer.getAuthorizerRefershToken();
        }
        if (component_access_token == null) {
            //获取component_access_token
        } else {
            //通过authorizer_appid获取authorizer_refresh_token

        }
        Authorizer newauthorizer1 = new WxOpenApi().refreshtoken(component_access_token, component_appid, authorizer_appid, authorizer_refresh_token);
        if(newauthorizer1!=null){
            authorizer.setAuthorizerRefershToken(newauthorizer1.getAuthorizerRefershToken());
            authorizer.setAuthorizerAccessToken(newauthorizer1.getAuthorizerAccessToken());
            componentDao.updateAuthInfo(authorizer);
            return true;
        }else {
            return false;
        }



    }
    @Override
    public String selectAuthInfo(String component_access_token, String component_appid, String authorization_code) {
        String uri = String.format(WxApi.QUERY_COMPONENT_INFO, component_access_token);

        JSONObject jsonObject = new JSONObject();
        JSONObject json  = new JSONObject();

        jsonObject.put("component_appid",component_appid);
        jsonObject.put("authorization_code",authorization_code);

        HashMap header = new HashMap();

        header.put("Content-Type","application/json");
        String result = null;

        result = HttpConnectionUtil.post(uri,header,jsonObject.toString());

        if(result!=null){
            json = JSONObject.parseObject(result);
        }
        if(json.getString("errcode")!=null){
            throw new NullPointerException("errcode：" + json.getString("errcode"));
        }else {
            String authorizer_appid = json.getJSONObject("authorization_info").getString("authorizer_appid");
            String authorizer_access_token = json.getJSONObject("authorization_info").getString("authorizer_access_token");
            String authorizer_refresh_token = json.getJSONObject("authorization_info").getString("authorizer_refresh_token");
            long start = System.currentTimeMillis();
            String time = String.valueOf(start / 1000);

            Authorizer authorizer = componentDao.queryAuthInfo(authorizer_appid);

            if (authorizer == null) {
                Authorizer authorizer1 = new Authorizer(authorizer_appid, authorizer_access_token, authorizer_refresh_token, time);
                componentDao.addAuthInfo(authorizer1);
                return "success";
            }
            long end = Long.parseLong(authorizer.getCreateTime());
            if ((Long.parseLong(time) - end) >= 7200) {
                Authorizer authorizer1 = new Authorizer(authorizer_appid, authorizer_access_token, authorizer_refresh_token, time);
                authorizer1.setId(authorizer.getId());
                componentDao.updateAuthInfo(authorizer1);
                return "success";
            }
        }
        return null;
    }

    @Override
    public boolean sendTemplateMessage(TemplateMessage tplMsg, String authorizer_appid) {
        Authorizer authorizer = componentDao.queryAuthInfo(authorizer_appid);
        String authorizerAccessToken = "";
        if(authorizer!=null){
            authorizerAccessToken = authorizer.getAuthorizerAccessToken();
        }else{
            LOGGER.info("authorizer_appid:"+authorizer_appid+"查询不到authorizerAccessToken");
            return false;
        }
        boolean result = new WxOpenApi().sendTemplateMessage(tplMsg,authorizerAccessToken);

        return result;

    }

    @Override
    public String longtToshorturl(String authorizer_appid, String long_url) {
        Authorizer authorizer = componentDao.queryAuthInfo(authorizer_appid);
        String authorizerAccessToken = "";
        if(authorizer!=null){
            authorizerAccessToken = authorizer.getAuthorizerAccessToken();
        }else{
            LOGGER.info("authorizer_appid:"+authorizer_appid+"查询不到authorizerAccessToken");
            return null;
        }
        String result = new WxOpenApi().longtToshorturl(authorizerAccessToken,long_url);
        return null;
    }

    @Override
    public JSONObject createqrcode(String authorizerAppid, String action_name, String expire_seconds, HashMap action_info) {
        Authorizer authorizer = componentDao.queryAuthInfo(authorizerAppid);
        String authorizerAccessToken = "";
        if(authorizer!=null){
            authorizerAccessToken = authorizer.getAuthorizerAccessToken();
        }else{
            LOGGER.info("authorizer_appid:"+authorizerAppid+"查询不到authorizerAccessToken");
            return null;
        }
        JSONObject result = new WxOpenApi().createqrcode(authorizerAccessToken,action_name,expire_seconds,action_info);
        return result;
    }
}
