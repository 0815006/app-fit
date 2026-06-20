package com.fit.service;

import com.fit.entity.TripPlanSource;

import java.util.List;

public interface TripPlanSourceService {

    /**
     * 根据明细ID获取来源列表
     */
    List<TripPlanSource> listByPlanDetailId(String planDetailId);

    /**
     * 创建来源
     */
    TripPlanSource save(TripPlanSource source);

    /**
     * 批量创建来源
     */
    void saveBatch(List<TripPlanSource> sources);

    /**
     * 根据明细ID删除所有来源
     */
    void deleteByPlanDetailId(String planDetailId);
}
