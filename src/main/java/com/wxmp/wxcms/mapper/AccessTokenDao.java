package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.AccessTokens;

/**
 *@Author Wisdom
 *@date 2020/12/21 16:20
 *@description 保存access_token使用
 *return
 */
public interface AccessTokenDao {

    AccessTokens getById(String appId);

    void add(AccessTokens entity);

    void update(AccessTokens entity);

}
