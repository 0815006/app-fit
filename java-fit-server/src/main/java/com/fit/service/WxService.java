package com.fit.service;

/**
 * 微信服务接口 —— 封装微信 code2Session 调用
 */
public interface WxService {

    /**
     * 使用小程序临时 code 换取用户的 openid
     *
     * @param code wx.login() 返回的临时凭证
     * @return 微信 openid
     */
    String getOpenIdByCode(String code);
}
