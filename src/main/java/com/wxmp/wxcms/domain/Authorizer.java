package com.wxmp.wxcms.domain;

import lombok.Data;

@SuppressWarnings("seria")
@Data
public class Authorizer {
    private Long id;

    private String authorizerAppId;

    private String authorizerAccessToken;

    private String authorizerRefershToken;

    private String createTime;

    public Authorizer(){}

    public Authorizer(String authorizerAppId, String authorizerAccessToken, String authorizerRefershToken, String createTime){
        this.authorizerAppId = authorizerAppId;
        this.authorizerAccessToken = authorizerAccessToken;
        this.authorizerRefershToken = authorizerRefershToken;
        this.createTime = createTime;
    }
}
