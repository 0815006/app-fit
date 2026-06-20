package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TripItem;

import java.util.List;

public interface TripItemService {

    /**
     * 分页查询物品
     */
    Page<TripItem> queryPage(int page, int size, String name, String category);

    /**
     * 获取所有物品(不分页)
     */
    List<TripItem> listAll();

    /**
     * 根据ID获取物品
     */
    TripItem getById(String id);

    /**
     * 创建物品
     */
    TripItem save(TripItem item);

    /**
     * 更新物品
     */
    TripItem update(TripItem item);

    /**
     * 删除物品
     */
    void delete(String id);
}
