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
    public Map<String, Object> toggle(String empNo, String dishName) {
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getEmpNo, empNo)
          .eq(UserFavoriteDish::getDishName, dishName);
        UserFavoriteDish existing = mapper.selectOne(qw);

        if (existing != null) {
            // 已存在 → 取消收藏
            mapper.deleteById(existing.getId());
            log.info("取消收藏: empNo={}, dishName={}", empNo, dishName);
            Map<String, Object> result = new HashMap<>();
            result.put("favorited", false);
            result.put("dishName", dishName);
            return result;
        } else {
            // 不存在 → 收藏，并增加 1 次订阅次数
            UserFavoriteDish entity = new UserFavoriteDish();
            entity.setEmpNo(empNo);
            entity.setDishName(dishName);
            mapper.insert(entity);

            // 收藏成功后增加 1 次订阅次数
            try {
                subscribeQuotaService.increment(empNo, TEMPLATE_ID, 1);
            } catch (Exception e) {
                log.warn("增加订阅次数失败 (不影响收藏): empNo={}, error={}", empNo, e.getMessage());
            }

            log.info("收藏成功: empNo={}, dishName={}", empNo, dishName);
            Map<String, Object> result = new HashMap<>();
            result.put("favorited", true);
            result.put("dishName", dishName);
            return result;
        }
    }

    @Override
    public List<String> check(String empNo, List<String> dishNames) {
        if (dishNames == null || dishNames.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getEmpNo, empNo)
          .in(UserFavoriteDish::getDishName, dishNames);
        return mapper.selectList(qw).stream()
                .map(UserFavoriteDish::getDishName)
                .toList();
    }

    @Override
    public List<UserFavoriteDish> list(String empNo) {
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getEmpNo, empNo)
          .orderByDesc(UserFavoriteDish::getCreateTime);
        return mapper.selectList(qw);
    }

    @Override
    public void delete(String empNo, String dishName) {
        LambdaQueryWrapper<UserFavoriteDish> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavoriteDish::getEmpNo, empNo)
          .eq(UserFavoriteDish::getDishName, dishName);
        mapper.delete(qw);
        log.info("删除收藏: empNo={}, dishName={}", empNo, dishName);
    }
}
