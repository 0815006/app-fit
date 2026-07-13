package com.fit.service;

import java.util.Map;

public interface SubscribeQuotaService {

    /**
     * 增加订阅次数（内部调用 & 小程序显式调用）
     * @param empNo      用户工号
     * @param templateId 模板ID
     * @param count      增加次数
     * @return Map { "remainingCount": N }
     */
    Map<String, Object> increment(String empNo, String templateId, int count);

    /**
     * 查询当前用户订阅次数
     * @param empNo      用户工号
     * @param templateId 模板ID
     * @return Map { "templateId", "remainingCount", "pushEnabled" }
     */
    Map<String, Object> query(String empNo, String templateId);

    /**
     * 切换推送开关
     * @param empNo       用户工号
     * @param templateId  模板ID
     * @param pushEnabled 是否开启推送
     * @return Map { "pushEnabled": true/false }
     */
    Map<String, Object> togglePush(String empNo, String templateId, boolean pushEnabled);
}
