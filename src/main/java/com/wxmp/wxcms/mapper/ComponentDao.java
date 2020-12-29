package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.Authorizer;

public interface ComponentDao {

    Authorizer queryAuthInfo(String authorizerAppId);

    void addAuthInfo(Authorizer authorizer);

    void updateAuthInfo(Authorizer authorizer);
}
