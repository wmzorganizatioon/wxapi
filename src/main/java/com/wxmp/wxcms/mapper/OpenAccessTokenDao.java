package com.wxmp.wxcms.mapper;

public interface OpenAccessTokenDao {

    OpenAccessTokenDao getById(String appId);

    void add(OpenAccessTokenDao entity);

    void update(OpenAccessTokenDao entity);
}
