package com.wxmp.wxapi.process;



import com.alibaba.fastjson.JSONObject;

import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.util.HttpConnectionUtil;
import com.wxmp.wxapi.service.ComponentService;
import com.wxmp.wxapi.service.impl.ComponentServiceImpl;
import com.wxmp.wxapi.vo.TemplateMessage;
import com.wxmp.wxcms.domain.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

public class WxOpenApi {

    @Autowired
    ComponentService componentService;

    private static final Logger LOGGER = LoggerFactory.getLogger(WxOpenApi.class);
    /**
     * 微信 API、微信开放平台接口
     *
     */
    // 获取component_access_token 接口
    public static final String GET_component_access_token = "https://api.weixin.qq.com/cgi-bin/component/api_component_token";
    // 刷新接口调用令牌
    public static final String authorizer_refresh_token = "https://api.weixin.qq.com/cgi-bin/component/api_authorizer_token?component_access_token=%s";
    // 刷新接口调用令牌
    public static final String sendtemplate = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";

    public static final String GET_TEMPLATE_INFO = "https://api.weixin.qq.com/cgi-bin/template/get_all_private_template?access_token=%s";
    //长连接转短连接
    public static final String longtToshorturl = "https://api.weixin.qq.com/cgi-bin/shorturl?access_token=%s";
    //生成带参数的二维码
    public static final String createqrcode = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=%s";

    // 获取token接口
    public static String getAuthorizer_refresh_tokenUrl(String component_access_token) {
        return String.format(authorizer_refresh_token,component_access_token);
    }
    // 生成带参数的二维码
    public static String geCreateqrcodeUrl(String component_access_token) {
        return String.format(createqrcode,component_access_token);
    }
    // 获取longtToshorturl接口
    public static String getLongtToshorturl(String component_access_token) {
        return String.format(longtToshorturl,component_access_token);
    }
    // 获取发送模板url
    public static String getsendtemplateUrl(String authorizerAccessToken) {
        return String.format(sendtemplate,authorizerAccessToken);
    }
    // 获取OAuth2.0 Token
    public String getcomponent_access_token(String component_appid, String component_appsecret, String component_verify_ticket) throws WxErrorException {
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
        String component_access_token = null;
        String errorcode = json.getString("errcode");
        if(errorcode!=null){
            LOGGER.info("获取component_access_token时errcode:"+errorcode);
        }else{
            component_access_token = json.getString("component_access_token");
        }
        try {
            componentService.getPreAuthCode(component_access_token, component_appid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return component_access_token;
    }

    public Authorizer refreshtoken(String component_access_token, String component_appid, String authorizer_appid, String authorizer_refresh_token) {
        boolean res;
        String refreshtokenurl = WxOpenApi.getAuthorizer_refresh_tokenUrl(component_access_token);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("authorizer_appid",authorizer_appid);
        jsonObject.put("authorizer_refresh_token",authorizer_refresh_token);
        jsonObject.put("component_appid",component_appid);
        HashMap header = new HashMap();
        header.put("Content-Type","application/json");
        String result = null;
        try {
            // result = HttpClientUtils.sendHttpPostJson(tockenUrl,map);
            result = HttpConnectionUtil.post(refreshtokenurl,header,jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject json  = new JSONObject();
        if(result!=null){
            json = JSONObject.parseObject(result);
        }
        String newauthorizer_access_token = null;
        String newauthorizer_refresh_token = null;
        String errorcode = json.getString("errcode");
        if(errorcode!=null&&!errorcode.equals("0")){
            LOGGER.info("更新authorizer_access_token时errcode:"+errorcode);
            return null;
        }else{
            newauthorizer_access_token = json.getString("authorizer_access_token");
            newauthorizer_refresh_token = json.getString("authorizer_refresh_token");
            //入库
            res = true;
        }
        Authorizer authorizer = new Authorizer();
        authorizer.setAuthorizerAccessToken(newauthorizer_access_token);
        authorizer.setAuthorizerRefershToken(newauthorizer_refresh_token);



        return authorizer;
    }

    public boolean sendTemplateMessage(TemplateMessage tplMsg, String authorizerAccessToken) {
        String refreshtokenurl = WxOpenApi.getsendtemplateUrl(authorizerAccessToken);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("touser",tplMsg.getOpenid());
        jsonObject.put("template_id",tplMsg.getTemplateId());
        jsonObject.put("url",tplMsg.getUrl());
        jsonObject.put("data",tplMsg.getData());
        HashMap header = new HashMap();
        header.put("Content-Type","application/json");
        String result = null;
        try {
            // result = HttpClientUtils.sendHttpPostJson(tockenUrl,map);
            result = HttpConnectionUtil.post(refreshtokenurl,header,jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject json  = new JSONObject();
        if(result!=null){
            json = JSONObject.parseObject(result);
        }
        String errorcode = json.getString("errcode");
        if(errorcode!=null){
            LOGGER.info("发送模板时errorcode:"+errorcode);
            if(errorcode.equals("0")){
                return true;
            }else {
                return false;
            }
        }else{
            return false;
        }


    }

    public String longtToshorturl(String authorizerAccessToken, String long_url) {
        String longtToshorturl = WxOpenApi.getLongtToshorturl(authorizerAccessToken);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action","long2short");
        jsonObject.put("long_url",long_url);

        HashMap header = new HashMap();
        header.put("Content-Type","application/json");
        String result = null;
        try {
            result = HttpConnectionUtil.post(longtToshorturl,header,jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject json  = new JSONObject();
        if(result!=null){
            json = JSONObject.parseObject(result);
        }
        String errorcode = json.getString("errcode");
        String short_url = null;
        if(errorcode!=null){
            LOGGER.info("长连接转短连接时:"+errorcode);
            if(errorcode.equals("0")){
                //成功
                short_url = json.getString("short_url");
                return short_url;
            }else {
                return null;
            }
        }else{
            short_url = json.getString("short_url");
            return short_url;
        }


    }

    public JSONObject createqrcode(String authorizerAccessToken, String action_name, String expire_seconds, HashMap action_info) {
        String createqrcodeurl = WxOpenApi.geCreateqrcodeUrl(authorizerAccessToken);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action_name",action_name);
        jsonObject.put("expire_seconds",expire_seconds);
        jsonObject.put("action_info",action_info);

        HashMap header = new HashMap();
        header.put("Content-Type","application/json");
        String result = null;
        try {
            result = HttpConnectionUtil.post(createqrcodeurl,header,jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject json  = new JSONObject();
        if(result!=null){
            json = JSONObject.parseObject(result);
        }
        String errorcode = json.getString("errcode");
        JSONObject reresult  = new JSONObject();
        if(errorcode!=null){
            LOGGER.info("生成带参数的二维码:"+errorcode);
            return null;

        }else{
            //成功
            String ticket = json.getString("ticket");
            String resexpire_seconds = json.getString("expire_seconds");
            String url = json.getString("url");
            reresult.put("ticket",ticket);
            reresult.put("url",url);
            reresult.put("expire_seconds",resexpire_seconds);

            return reresult;
        }

    }
}
