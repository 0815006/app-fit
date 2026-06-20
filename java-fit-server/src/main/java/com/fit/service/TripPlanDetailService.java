package com.fit.service;

import com.fit.entity.TripPlanDetail;

import java.util.List;

public interface TripPlanDetailService {

    /**
     * 根据计划ID获取明细列表
     */
    List<TripPlanDetail> listByPlanId(String planId);

    /**
     * 根据ID获取明细
     */
    TripPlanDetail getById(String id);

    /**
     * 创建明细
     */
    TripPlanDetail save(TripPlanDetail detail);

    /**
     * 批量创建明细
     */
    void saveBatch(List<TripPlanDetail> details);

    /**
     * 更新明细
     */
    TripPlanDetail update(TripPlanDetail detail);

    /**
     * 更新装箱状态
     */
    void updateChecked(String id, Integer isChecked);

    /**
     * 更新排除标志
     */
    void updateExcludeFlag(String id, Integer excludeFlag);

    /**
     * 更新目标数量
     */
    void updateQuantity(String id, Integer targetQuantity);

    /**
     * 根据计划ID删除所有明细
     */
    void deleteByPlanId(String planId);
}
