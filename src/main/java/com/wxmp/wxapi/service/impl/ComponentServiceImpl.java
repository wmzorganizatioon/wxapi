package com.wxmp.wxapi.service.impl;

import com.wxmp.core.util.HttpConnectionUtil;
import com.wxmp.wxapi.process.HttpMethod;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.service.ComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        String uri = String.format(WxApi.COMPONENT_ACCESS_TOKEN, component_access_token	);
        String pre_auth_code = String.valueOf(WxApi.httpsRequest(uri, HttpMethod.POST, component_appid).get("pre_auth_code"));
        LOGGER.debug("获取预授权码：" + pre_auth_code);
        return pre_auth_code;
    }
}
