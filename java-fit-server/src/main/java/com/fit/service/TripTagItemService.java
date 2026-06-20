package com.fit.service;

import com.fit.entity.TripTagItem;

import java.util.List;

public interface TripTagItemService {

    /**
     * 根据标签ID获取关联物品
     */
    List<TripTagItem> listByTagId(String tagId);

    /**
     * 根据多个标签ID获取关联物品(去重)
     */
    List<TripTagItem> listByTagIds(List<String> tagIds);

    /**
     * 创建关联
     */
    TripTagItem save(TripTagItem tagItem);

    /**
     * 删除关联
     */
    void delete(String id);
}
