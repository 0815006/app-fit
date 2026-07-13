package com.fit.service;

import java.util.Map;

/**
 * 微信订阅消息发送服务
 */
public interface WxSubscribeMessageService {

    /**
     * 发送订阅消息
     *
     * @param openId     用户微信 openId
     * @param templateId 模板 ID
     * @param thing2     饮食提醒内容
     * @param thing3     饮食推荐菜品
     * @param time1      就餐时间
     * @param page       点击跳转页面路径
     * @return 发送结果，包含 errcode 等信息
     */
    Map<String, Object> sendSubscribeMessage(String openId, String templateId,
                                              String thing2, String thing3,
                                              String time1, String page);
}
