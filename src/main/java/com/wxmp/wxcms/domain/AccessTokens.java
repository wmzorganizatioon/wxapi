package com.wxmp.wxcms.domain;

import lombok.Data;

/**
 *@Author 86151
 *@Date 2020/12/21 16:17
 *Description
 * * @param  : null
 * * @return : null
 */
@Data
public class AccessTokens {
    private Long id;

    private String appId;

    private String appSecret;

    private String accessToken;

    private String createTime;

    public AccessTokens(){}

    public AccessTokens(String appId){
        this.appId = appId;
    }

    public AccessTokens(String appId, String appSecret, String accessToken, String createTime){
        this.appId = appId;
        this.accessToken = accessToken;
        this.createTime = createTime;
        this.appSecret = appSecret;
    }

    public AccessTokens(Long id, String appId, String appSecret, String accessToken, String createTime){
        this.id = id;
        this.appId = appId;
        this.accessToken = accessToken;
        this.createTime = createTime;
        this.appSecret = appSecret;
    }
}
