package com.fit.service;

import com.fit.entity.User;

/**
 * 微信服务接口 —— 封装微信 code2Session 和 access_token 获取
 */
public interface WxService {

    /**
     * 使用小程序临时 code 换取用户的 openid
     *
     * @param code wx.login() 返回的临时凭证
     * @return 微信 openid
     */
    String getOpenIdByCode(String code);

    /**
     * 获取微信 access_token（带内存缓存）
     * 供内容安全检测等场景使用
     *
     * @return access_token
     */
    String getAccessToken();
}
