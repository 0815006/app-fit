package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.UserFavoriteDish;
import com.fit.mapper.UserFavoriteDishMapper;
import com.fit.service.FavoriteDishService;
import com.fit.service.SubscribeQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteDishServiceImpl implements FavoriteDishService {

    private final UserFavoriteDishMapper mapper;
    private final SubscribeQuotaService subscribeQuotaService;

    private static final String TEMPLATE_ID = "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA";

    @Override
    @Transactional
    public Map<String, Object> toggle(String userId, String dishName) {
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getUserId, userId)
          .eq(UserFavoriteDish::getDishName, dishName);
        UserFavoriteDish existing = mapper.selectOne(qw);

        if (existing != null) {
            // 已存在 → 取消收藏
            mapper.deleteById(existing.getId());
            log.info("取消收藏: userId={}, dishName={}", userId, dishName);
            Map<String, Object> result = new HashMap<>();
            result.put("favorited", false);
            result.put("dishName", dishName);
            return result;
        } else {
            // 不存在 → 收藏，并增加 1 次订阅次数
            UserFavoriteDish entity = new UserFavoriteDish();
            entity.setUserId(userId);
            entity.setDishName(dishName);
            mapper.insert(entity);

            // 收藏成功后增加 1 次订阅次数
            try {
                subscribeQuotaService.increment(userId, TEMPLATE_ID, 1);
            } catch (Exception e) {
                log.warn("增加订阅次数失败 (不影响收藏): userId={}, error={}", userId, e.getMessage());
            }

            log.info("收藏成功: userId={}, dishName={}", userId, dishName);
            Map<String, Object> result = new HashMap<>();
            result.put("favorited", true);
            result.put("dishName", dishName);
            return result;
        }
    }

    @Override
    public List<String> check(String userId, List<String> dishNames) {
        if (dishNames == null || dishNames.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getUserId, userId)
          .in(UserFavoriteDish::getDishName, dishNames);
        return mapper.selectList(qw).stream()
                .map(UserFavoriteDish::getDishName)
                .toList();
    }

    @Override
    public List<UserFavoriteDish> list(String userId) {
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getUserId, userId)
          .orderByDesc(UserFavoriteDish::getCreateTime);
        return mapper.selectList(qw);
    }

    @Override
    public void delete(String userId, String dishName) {
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getUserId, userId)
          .eq(UserFavoriteDish::getDishName, dishName);
        mapper.delete(qw);
        log.info("删除收藏: userId={}, dishName={}", userId, dishName);
    }
}
