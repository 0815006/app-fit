package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TripPlan;

import java.util.List;

public interface TripPlanService {

    /**
     * 分页查询计划
     */
    Page<TripPlan> queryPage(int page, int size, String userId, String status);

    /**
     * 获取用户所有计划
     */
    List<TripPlan> listByUserId(String userId);

    /**
     * 根据ID获取计划
     */
    TripPlan getById(String id);

    /**
     * 创建计划
     */
    TripPlan save(TripPlan plan);

    /**
     * 更新计划
     */
    TripPlan update(TripPlan plan);

    /**
     * 删除计划
     */
    void delete(String id);

    /**
     * 更新计划状态
     */
    void updateStatus(String id, String status);
}
