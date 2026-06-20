package com.fit.service;

import com.fit.entity.TripTag;

import java.util.List;

public interface TripTagService {

    /**
     * 获取所有标签
     */
    List<TripTag> listAll();

    /**
     * 根据类型获取标签
     */
    List<TripTag> listByType(String type);

    /**
     * 根据ID获取标签
     */
    TripTag getById(String id);

    /**
     * 创建标签
     */
    TripTag save(TripTag tag);

    /**
     * 更新标签
     */
    TripTag update(TripTag tag);

    /**
     * 删除标签
     */
    void delete(String id);
}
