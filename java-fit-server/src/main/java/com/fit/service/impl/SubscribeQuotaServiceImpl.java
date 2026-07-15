package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.UserSubscribeQuota;
import com.fit.mapper.UserSubscribeQuotaMapper;
import com.fit.service.SubscribeQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeQuotaServiceImpl implements SubscribeQuotaService {

    private final UserSubscribeQuotaMapper mapper;

    /** 每日浏览菜品累计上限 */
    private static final int DAILY_BROWSE_LIMIT = 5;

    /** 累计总上限 */
    private static final int TOTAL_LIMIT = 30;

    @Override
    @Transactional
    public Map<String, Object> increment(String userId, String templateId, int count) {
        LambdaQueryWrapper<UserSubscribeQuota> qw = new LambdaQueryWrapper<>();
        qw.eq(UserSubscribeQuota::getUserId, userId)
          .eq(UserSubscribeQuota::getTemplateId, templateId);
        UserSubscribeQuota entity = mapper.selectOne(qw);

        if (entity == null) {
            // 记录不存在 → 创建
            entity = new UserSubscribeQuota();
            entity.setUserId(userId);
            entity.setTemplateId(templateId);
            int initialCount = Math.min(count, TOTAL_LIMIT);
            entity.setRemainingCount(initialCount);
            entity.setPushEnabled(1);
            mapper.insert(entity);
            log.info("创建订阅配额: userId={}, remainingCount={}", userId, initialCount);

            Map<String, Object> result = new HashMap<>();
            result.put("remainingCount", initialCount);
            return result;
        }

        // 记录已存在 → 累加，但不超过总上限
        int newCount = entity.getRemainingCount() + count;
        if (newCount > TOTAL_LIMIT) {
            newCount = TOTAL_LIMIT;
        }
        entity.setRemainingCount(newCount);
        mapper.updateById(entity);
        log.info("增加订阅次数: userId={}, +{}, total={}", userId, count, newCount);

        Map<String, Object> result = new HashMap<>();
        result.put("remainingCount", newCount);
        return result;
    }

    @Override
    public Map<String, Object> query(String userId, String templateId) {
        LambdaQueryWrapper<UserSubscribeQuota> qw = new LambdaQueryWrapper<>();
        qw.eq(UserSubscribeQuota::getUserId, userId)
          .eq(UserSubscribeQuota::getTemplateId, templateId);
        UserSubscribeQuota entity = mapper.selectOne(qw);

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);

        if (entity == null) {
            result.put("remainingCount", 0);
            result.put("pushEnabled", true);
        } else {
            result.put("remainingCount", entity.getRemainingCount());
            result.put("pushEnabled", entity.getPushEnabled() == 1);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> togglePush(String userId, String templateId, boolean pushEnabled) {
        LambdaQueryWrapper<UserSubscribeQuota> qw = new LambdaQueryWrapper<>();
        qw.eq(UserSubscribeQuota::getUserId, userId)
          .eq(UserSubscribeQuota::getTemplateId, templateId);
        UserSubscribeQuota entity = mapper.selectOne(qw);

        int flag = pushEnabled ? 1 : 0;

        if (entity == null) {
            // 记录不存在时自动创建
            entity = new UserSubscribeQuota();
            entity.setUserId(userId);
            entity.setTemplateId(templateId);
            entity.setRemainingCount(0);
            entity.setPushEnabled(flag);
            mapper.insert(entity);
            log.info("创建订阅配额并设置推送开关: userId={}, pushEnabled={}", userId, pushEnabled);
        } else {
            entity.setPushEnabled(flag);
            mapper.updateById(entity);
            log.info("切换推送开关: userId={}, pushEnabled={}", userId, pushEnabled);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pushEnabled", pushEnabled);
        return result;
    }
}
