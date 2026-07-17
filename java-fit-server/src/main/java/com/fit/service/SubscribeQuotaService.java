package com.fit.service;

import java.util.Map;

public interface SubscribeQuotaService {

    /**
     * 增加订阅次数（内部调用 & 小程序显式调用）
     * @param userId     用户主键 ID
     * @param templateId 模板ID
     * @param count      增加次数
     * @return Map { "remainingCount": N }
     */
    Map<String, Object> increment(String userId, String templateId, int count);

    /**
     * 查询当前用户订阅次数
     * @param userId     用户主键 ID
     * @param templateId 模板ID
     * @return Map { "templateId", "remainingCount", "pushEnabled" }
     */
    Map<String, Object> query(String userId, String templateId);

    /**
     * 浏览菜品攒次数（受每日 5 次上限约束）
     * @param userId     用户主键 ID
     * @param templateId 模板ID
     * @return Map { "remainingCount": N }
     * @throws RuntimeException 当日浏览次数已达上限
     */
    Map<String, Object> browseIncrement(String userId, String templateId);

    /**
     * 切换推送开关
     * @param userId      用户主键 ID
     * @param templateId  模板ID
     * @param pushEnabled 是否开启推送
     * @return Map { "pushEnabled": true/false }
     */
    Map<String, Object> togglePush(String userId, String templateId, boolean pushEnabled);
}
