package com.wxmp.wxapi.service;

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
    String getPreAuthCode(String component_access_token, String component_appid);
}
